package pt.inesc.termite.server;

import pt.inesc.termite.server.exceptions.ControllerDriverException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class AVDControllerDriver {

    //public static final int EMU_STATE_INIT    = 1; // deployed but not necessarily booting
    public static final int EMU_STATE_OFFLINE = 2; // booting up
    public static final int EMU_STATE_ONLINE = 3; // finished bootstrapping but not bound yet
    public String platform;
    public String termiteServerPath;
    private String sdkPath;
    private String networkIp;
    private ArrayList<AddressSet> availableAddresses;
    private ArrayList<Emulator> emulatorsInstances = new ArrayList<>();
    private HashMap<Integer, AddressSet> assignedEmulators = new HashMap<>(); // < ("5554", AddressSet) >
    //public static final int EMU_STATE_NETOK   = 4; // bound to a given node (port forwarding set)

    public AVDControllerDriver(String netIp, String sdkS, String plat, String serverPath) {
        this.sdkPath = sdkS;
        this.networkIp = netIp;
        this.platform = plat;
        this.termiteServerPath = serverPath;
        this.availableAddresses = generateAddressSetsArray();
        start();
    }

    // Receives "emulator-5554" and returns 5554
    private static int getEmulatorPort(String eid) {
        int emuPort;
        String[] split = eid.split("-"); // expected eid in the form of "emulator-<port>"
        if (split.length != 2 || !split[0].equals("emulator")) {
            return -1;
        }
        try {
            emuPort = Integer.parseInt(split[1]);
        } catch (Exception e) {
            return -1;
        }
        return emuPort;
    }

    // This is run on termite2 server start to check if there is emulators already running, and automatically sets the redirection rules
    protected void start() {
        try {
            setEmulatorInstances();
            checkAlreadySetRedirections();
            setAllRedirections();
        } catch (ControllerDriverException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("AVDControllerDriver Startup finished.\n");
    }

    /*This method is only used on start to check if emulators already online have port rules set,
    if so we create their instances and claim the port addresses that are already set */
    private void checkAlreadySetRedirections() {
        System.out.println("Checking current port redirections...");

        for (Emulator onlineEmus : emulatorsInstances) {
            int emuPort = onlineEmus.get_port();

            try {
                String redirList = "redir list\n";
                Socket emuConnection = new Socket("localhost", emuPort);

                BufferedReader reader = new BufferedReader(new InputStreamReader(emuConnection.getInputStream()));

                if (connectionOffline(reader)) {
                    throw new ControllerDriverException("Error: Socket connection could not be made with emulator-" + emuPort);
                }

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(emuConnection.getOutputStream()));

                // 1st get redir list
                writer.write(redirList);
                writer.flush();

                ArrayList<String> redirections = new ArrayList<>();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (line.equals("OK") || line.equals("no active redirections") || line.startsWith("KO")) {
                        break;
                    } else {
                        redirections.add(line);
                    }
                }

                redirections = formatRedirList(redirections);

                //Register por redirect if exists and is valid
                for (String port : redirections) {
                    AddressSet address = claimAddressSet(Integer.parseInt(port));
                    if (address != null) {
                        System.out.println("Valid port (" + port + ") redirection found on emulator-" + emuPort + ", address claimed.");
                        assignedEmulators.put(emuPort, address);
                        break;
                    }
                }
            } catch (IOException | ControllerDriverException e) {
                System.out.println("Warning: Port redirection(s) on current emulator-" + emuPort + " could not be verified.");
            }
        }
        System.out.println("Finished checking port redirections for all online emulators.");
    }

    public HashMap<Integer, AddressSet> getAssignedEmulators() {
        return assignedEmulators;
    }

    /*This method forms the Emulator objs to Strings to be sent to the Termite2 Client*/
    public String getEmulatorsString() {
        if (assignedEmulators.size() == 0) {
            return null;
        }

        String result = "";
        for (Integer emuPor : assignedEmulators.keySet()) {
            //name|port|ip|real_commit_port|real_message_port\n
            result = result + (getEmulatorInstance(emuPor).get_name()) + "|" + (getEmulatorInstance(emuPor).get_port()) + "|" + (assignedEmulators.get(emuPor).getDataString()) + "\n";
        }

        return result.substring(0, result.length() - 1);    // returns result - the last char (\n)
    }

    public String getAssignedEmuName(int commitPort) { // commitPort = 9011, 9021 ....
        for (int port : assignedEmulators.keySet()) {
            if (assignedEmulators.get(port).getRealCommitPort() == commitPort) {
                return Objects.requireNonNull(getEmulatorInstance(port)).get_name();
            }
        }
        return null;
    }

    public String getNetworkIp() {
        return networkIp;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * *  Emulator operations methods  * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public ArrayList<Emulator> getEmulatorsInstances() {
        return emulatorsInstances;
    }

    public boolean createEmulatorsFromScript(String name, String api) {
        assert name != null;
        String tid = Thread.currentThread().getName();

        String scriptFolderPath = termiteServerPath + File.separator + "config" + File.separator + "serverscripts" + File.separator;
        List<String> exeList = new ArrayList<>();
        if (platform.equals("windows")) {
            /*exeList.add("cmd");
            exeList.add("/c");
            exeList.add("start");*/
            exeList.add(scriptFolderPath + "createavds.bat");
        } else {
            exeList.add(scriptFolderPath + "createavds.sh");
        }

        exeList.add(name);
        exeList.add(api);

        try {
            ProcessBuilder pb = new ProcessBuilder(exeList);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println((tid + ": " + line));
            }
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void destroyAvd(String avdName) throws ControllerDriverException {
        String exeCmd;
        if (platform.equals("windows")) {
            exeCmd = sdkPath + File.separator + "tools" + File.separator + "bin" + File.separator + "avdmanager.bat delete avd -n " + avdName;
        } else {
            exeCmd = sdkPath + "/tools/bin/./avdmanager delete avd -n " + avdName;
        }

        try {
            Runtime.getRuntime().exec(exeCmd);
            System.out.println("Emulator " + avdName + " destroyed.");
        } catch (IOException e) {
            e.printStackTrace();
            throw new ControllerDriverException("Error: Unable to destroy avd " + avdName + ".");
        }

    }

    public void startEmulator(String emuName) throws ControllerDriverException {

        String exeCmd;
        if (platform.equals("windows")) {
            exeCmd = sdkPath + File.separator + "emulator" + File.separator + "emulator -avd " + emuName + " -partition-size 512 -no-snapshot";
        } else {
            exeCmd = sdkPath + File.separator + "emulator" + File.separator + "./emulator -avd " + emuName + " -partition-size 512 -no-snapshot";
        }

        try {
            Runtime.getRuntime().exec(exeCmd);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ControllerDriverException("Error: Unable to start emulator " + emuName + ".");
        }
    }

    public void stopEmulator(int emuPort) throws ControllerDriverException {

        String exeCmd;
        if (platform.equals("windows")) {
            exeCmd = sdkPath + File.separator + "platform-tools" + File.separator + "adb -s emulator-" + emuPort + " emu kill";
        } else {
            exeCmd = sdkPath + File.separator + "platform-tools" + File.separator + "./adb -s emulator-" + emuPort + " emu kill";
        }

        try {
            Runtime.getRuntime().exec(exeCmd);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ControllerDriverException("Error: Unable to stop emulator-" + emuPort + ".");
        }
    }

    public String startApp(int emuPort, String appPackage) {

        String exeCmd;
        if (platform.equals("windows")) {
            exeCmd = sdkPath + File.separator + "platform-tools" + File.separator + "adb -s emulator-" + emuPort + " shell monkey -p " + appPackage + " -c android.intent.category.LAUNCHER 1";
        } else {
            exeCmd = sdkPath + File.separator + "platform-tools" + File.separator + "./adb -s emulator-" + emuPort + " shell monkey -p " + appPackage + " -c android.intent.category.LAUNCHER 1";
        }

        try {
            Runtime.getRuntime().exec(exeCmd);
            return "Launcher intent sent.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Launcher intent could not be sent.";
        }
    }

    public String installApk(int emuPort, String apkName) {
        String tid = Thread.currentThread().getName();
        String CmdPath = sdkPath + File.separator + "platform-tools" + File.separator;
        String apkPath = termiteServerPath + File.separator + "apks" + File.separator + apkName;
        String exeCmd;

        if (platform.equals("windows")) {
            exeCmd = CmdPath + "adb -s emulator-" + emuPort + " install -t -r " + apkPath;
        } else {
            exeCmd = CmdPath + "./adb -s emulator-" + emuPort + " install -t -r " + apkPath;
        }

        try {
            System.out.println("Executing install apk cmd: " + exeCmd);
            Process process = Runtime.getRuntime().exec(exeCmd);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Read the output from the command
            String line = null;
            while ((line = stdInput.readLine()) != null) {
                System.out.println(tid + " -> " + line);
                if (line.contains("Success") || line.contains("Failure")) {
                    return line;
                }
            }
            return "Unexpected installation result.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Install command failed.";
        }
    }

    //return list with installed emulators -> [ "emu1_name", emu2_name", ...]
    public ArrayList<String> getInstalledEmulators() {

        ArrayList<String> result = new ArrayList<String>();
        String exeCmd = null;

        if (platform.equalsIgnoreCase("windows")) {
            exeCmd = sdkPath + File.separator + "tools" + File.separator + "bin" + File.separator + "avdmanager.bat list avd";
        } else { // unix
            exeCmd = sdkPath + File.separator + "tools" + File.separator + "bin" + File.separator + "avdmanager list avd";
        }

        try {
            Process process = Runtime.getRuntime().exec(exeCmd);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Read the output from the command
            String line = null;
            while ((line = stdInput.readLine()) != null) {
                if (line.length() > 0 && line.contains("Name:")) {
                    String[] splitLine = line.split(": ");
                    result.add(splitLine[1]);
                }
            }
            return result;

        } catch (IOException e) {
            e.printStackTrace();
            result.add("Error: Could not get installed emulators.");
            return result;
        }

    }

    //return list of online emulators -> [ "emulator-5554 status", "emulator-5556 status", ... ]
    public ArrayList<String> getOnlineEmulators() {
        ArrayList<String> result = new ArrayList<>();

        String exeCmd;
        if (platform.equals("windows")) {
            exeCmd = sdkPath + File.separator + "platform-tools" + File.separator + "adb devices";
        } else {
            exeCmd = sdkPath + File.separator + "platform-tools" + File.separator + "./adb devices";
        }

        try {

            Process process = Runtime.getRuntime().exec(exeCmd);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Read the output from the command
            String line = null;
            while ((line = stdInput.readLine()) != null) {
                if (line.length() > 0 && !line.contains("List of devices attached")) {
                    result.add(line);
                }
            }
            return result;

        } catch (IOException e) {
            System.out.println("Error: Unable to get online emulator(s) instance(s), please check adb status and refresh.");
            return result;
        }
    }

    public boolean checkIfInstalled(String osversion) {
        String exeCmd;
        if (platform.equals("windows")) {
            exeCmd = sdkPath + File.separator + "tools" + File.separator + "bin" + File.separator + "sdkmanager.bat --list";
        } else {
            exeCmd = sdkPath + File.separator + "tools" + File.separator + "bin" + File.separator + "./sdkmanager --list";
        }

        try {
            System.out.println("Executing cmd: " + exeCmd);
            Process process = Runtime.getRuntime().exec(exeCmd);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Read the output from the command
            String line = null;
            boolean installedSection = false;
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
                if (line.equals("Installed packages:")) {
                    installedSection = true;
                }
                if (line.equals("Available Packages:")) {
                    installedSection = false;
                }
                if (installedSection && line.contains(osversion)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * *   Port Redirection methods  * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // This is used when adb server crashes and we need to restart it
    public void restartAdbServer() {
        String killServer;
        String startServer;
        if (platform.equals("windows")) {
            killServer = sdkPath + File.separator + "platform-tools" + File.separator + "adb kill-server";
            startServer = sdkPath + File.separator + "platform-tools" + File.separator + "adb start-server";
        } else {
            killServer = sdkPath + File.separator + "platform-tools" + File.separator + "./adb kill-server";
            startServer = sdkPath + File.separator + "platform-tools" + File.separator + "./adb start-server";
        }

        try {
            Runtime.getRuntime().exec(killServer);
            Runtime.getRuntime().exec(startServer);
        } catch (IOException e) {
            System.out.println("Error: Unable to restart adb server.");
        }
    }

    /* Sets and refreshes state of the emulatorsInstances that are currently online on the machine. */
    private void setEmulatorInstances() throws ControllerDriverException {
        System.out.println("Setting current emulator instances...");

        //Emulators hasmap: <(5554,"device(state)"), ...>
        HashMap<Integer, String> currList = new HashMap<>();

        List<String> emusOnline = getOnlineEmulators();
        for (String emu : emusOnline) {
            String[] port_state = emu.split("\\s+");
            if (port_state.length == 2) {
                int port = getEmulatorPort(port_state[0]);
                currList.put(port, port_state[1]);
                //System.out.println("Added to emu currList: " + port +", state: " + port_state[1]);
            }
        }

        // first remove the stale entries from the emulators list
        ArrayList<Emulator> emuTmp = new ArrayList<>();
        for (Emulator emu : emulatorsInstances) {
            if (currList.get(emu.get_port()) != null) { // the emulator still exists;
                emuTmp.add(emu);
            }
        }
        emulatorsInstances = emuTmp; // refresh emulators instances list

        // then, we update the current state of the emulators
        for (int emuPort : currList.keySet()) {
            String state = currList.get(emuPort);
            Emulator temu = getEmulatorInstance(emuPort);

            // doesn't exist yet
            if (temu == null) {
                if (state.equals("offline")) {
                    emulatorsInstances.add(new Emulator(emuPort, EMU_STATE_OFFLINE));
                    continue;
                }
                if (state.equals("device")) {
                    emulatorsInstances.add(new Emulator(emuPort, EMU_STATE_ONLINE));
                    continue;
                }
                throw new ControllerDriverException("Error: Unknown emulator state found.");
            }

            // is in INIT or OFFLINE states
            else if (temu.get_state() < EMU_STATE_ONLINE) {
                if (state.equals("offline")) {
                    changeEmuState(emuPort, EMU_STATE_OFFLINE);
                    continue;
                }
                if (state.equals("device")) {
                    changeEmuState(emuPort, EMU_STATE_ONLINE);
                    continue;
                }
                throw new ControllerDriverException("Error: Unknown emulator state found.");
            } else {
                System.out.println("Emu " + emuPort + " already online.");
            }

        }

        //If we found emulators online we grab and set each emulator name
        if (emulatorsInstances.size() != 0) {
            setEmulatorsName();
        }
        System.out.println("Available emulator instances updated.");
    }

    //Sets the emulator name on all emulator instances
    private void setEmulatorsName() {
        System.out.println("Setting emulator names...");
        boolean nameSet = false;

        for (Emulator emu : emulatorsInstances) {
            int emuPort = emu.get_port();
            try {
                Socket socketAvd = new Socket("localhost", emuPort);
                socketAvd.setSoTimeout(10000); // set timeout for reads

                BufferedReader reader = new BufferedReader(new InputStreamReader(socketAvd.getInputStream()));

                if (connectionOffline(reader)) {
                    System.out.println("Error: Socket connection could not be made with emulator-" + emuPort);
                } else {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socketAvd.getOutputStream()));

                    // write command to show emulator name
                    writer.write("avd name\n");
                    writer.flush();

                    String emulatorName = "default" + emuPort;
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("KO")) {
                            break;
                        } else if (line.equals("OK")) {
                            setEmulatorName(emuPort, emulatorName);
                            nameSet = true;
                            break;
                        } else {
                            emulatorName = line;
                        }
                    }

                    writer.write("quit\n");
                    writer.flush();

                    writer.close();
                    reader.close();
                    socketAvd.close();
                }

            } catch (IOException ignored) {
            }

            if (!nameSet) {
                setEmulatorName(emuPort, "default" + emuPort);
                System.out.println("Error: Unable to set name on emulator " + emuPort + ", default name used.");
            }
        }
    }

    /* This method refreshes all emulator instances available, deletes all current binds and rebinds from new (preserves those unchanged) */
    public void refreshAllRedirections() {
        System.out.println("Refreshing all redirections...");

        try {
            setEmulatorInstances();
        } catch (ControllerDriverException e) {
            e.printStackTrace();
        }

        ArrayList<Integer> assignedEmulatorsPorts = new ArrayList<Integer>(assignedEmulators.keySet());
        for (Integer emuPort : assignedEmulatorsPorts) {
            if (getEmulatorInstance(emuPort) == null) { // emulator no longer exists
                releaseAddressSet(assignedEmulators.get(emuPort));
                assignedEmulators.remove(emuPort);
                System.out.println("Emulator " + emuPort + " no longer exists, assign deleted.");
            }
        }
        setAllRedirections();
    }

    private void setAllRedirections() {
        System.out.println("Refreshing all port redirections rules...");

        if (emulatorsInstances.size() == 0) {
            System.out.println("No emulator instances available.");
        } else {
            for (Emulator emu : emulatorsInstances) {
                setRedirection(emu.get_port());
            }
        }
    }

    private void setRedirection(int emuPort) {
        boolean commitPortRules = false;
        boolean msgPortRules = false;

        if (assignedEmulators.get(emuPort) != null) {
            return;
        }

        AddressSet newAddrSet = claimAddressSet();
        if (newAddrSet == null) {
            System.out.println("No AddressSet available to set port redirection rules on emulator-" + emuPort);
            return;
        }

        try {
            String redirCommit = "redir add tcp:" + newAddrSet.getRealCommitPort() + ":9001\n";
            String redirMessages = "redir add tcp:" + newAddrSet.getRealMessagePort() + ":10001\n";

            Socket confConnection = new Socket("localhost", emuPort);
            confConnection.setSoTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(confConnection.getInputStream()));

            if (connectionOffline(reader)) {
                System.out.println("Warnning: Socket connection could not be made with emulator-" + emuPort);
            } else {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(confConnection.getOutputStream()));


                // write redirect port for commits
                writer.write(redirCommit);
                writer.flush();
                commitPortRules = checkPortRedirection(reader);

                // write redirect port for message sharing between emulators
                writer.write(redirMessages);
                writer.flush();
                msgPortRules = checkPortRedirection(reader);

                writer.write("quit\n");
                writer.flush();

                confConnection.close();
            }

        } catch (IOException e) {
            System.out.println("Warnning: Socket connection with emulator-" + emuPort + " lost. Please check if adb is online and refresh.");
        }

        if (commitPortRules && msgPortRules) {
            assignedEmulators.put(emuPort, newAddrSet);
            System.out.println("Port redirection rules on emulator-" + emuPort + " set successfully.");
        } else {
            releaseAddressSet(newAddrSet);
            System.out.println("Warnning: Port redirection rules on emulator-" + emuPort + " not set.");
        }
    }

    /* With Android studio we can only run 16 emulator instances max ranging from port 5554 to 5584.
    Has such, we only need 16 AddressSets at all time, when then only need to properly handle their usage */
    private ArrayList<AddressSet> generateAddressSetsArray() {
        ArrayList<AddressSet> addrSets = new ArrayList<>();
        int basec = 9011;
        int basem = 10011;

        while(addrSets.size() != 16){
            if(isPortAvailable(basec) && isPortAvailable(basem)){
                AddressSet newAddrSet = new AddressSet(basec, basem, networkIp);
                addrSets.add(newAddrSet);
            }
            basec++;
            basem++;
        }
        return addrSets;
    }


    /* Claims and returns an available AddressSet to be used on emulator redirection */
    private AddressSet claimAddressSet() {
        if (availableAddresses.size() > 0) {
            return availableAddresses.remove(0);
        }
        return null;
    }

    /* Claims and returns an available AddressSet to be used on emulator redirection that correspondes to passed port */
    private AddressSet claimAddressSet(int port) {
        if (availableAddresses.size() > 0) {
            for (AddressSet address : availableAddresses) {
                if (address.getRealCommitPort() == port) {
                    availableAddresses.remove(address);
                    return address;
                }
            }
        }
        return null;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * *   Helper methods  * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /* Places received AddressSet has an available AddressSet for future redirections */
    private void releaseAddressSet(AddressSet addrS) {
        if (availableAddresses.size() < 16) {
            availableAddresses.add(0, addrS);
            //Now we order list based on ips
            availableAddresses.sort(Comparator.comparingInt(AddressSet::getRealCommitPort));
        }
    }

    private boolean connectionOffline(BufferedReader reader) {
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.equals("OK")) {
                    return false;
                }
            }
        } catch (IOException ignored) {
        }
        return true;
    }

    /* This is used to test response from redirect commands on the emulator AVD */
    private boolean checkPortRedirection(BufferedReader reader) {
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.equals("OK") || line.contains("host port already active")) {
                    return true;
                } else if (line.contains("port probably used by another program on host")) {
                    System.out.println("Warning: " + line);
                    return false;
                } else if (line.startsWith("KO")) {
                    return false;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }


    private Emulator getEmulatorInstance(int port) {
        for (Emulator emu : emulatorsInstances) {
            if (emu.get_port() == port) {
                return emu;
            }
        }
        return null;
    }

    private void changeEmuState(int port, int newState) {
        for (Emulator emulatorsInstance : emulatorsInstances) {
            if (emulatorsInstance.get_port() == port)
                emulatorsInstance.changeState(newState);
        }
    }

    private void setEmulatorName(int port, String name) {
        for (Emulator emulatorsInstance : emulatorsInstances) {
            if (emulatorsInstance.get_port() == port)
                emulatorsInstance.setName(name);
        }
    }

    //receives ["tcp:9011 => 9001", "tcp:10011 => 9001", ...] and returns ["9011","10011", ...]
    private ArrayList<String> formatRedirList(ArrayList<String> redirections) {
        ArrayList<String> trimmed = new ArrayList<String>();
        for (String redir : redirections) {
            redir = redir.replaceAll("\\s+", ""); //removes spaces, tabs and hidden chars
            String port = (redir.split(":")[1]).split("=>")[0]; // 1st split -> tcp | 9011=>9001 , 2nd split -> 9011 | 9001
            trimmed.add(port);
        }
        return trimmed;
    }

    private boolean doesFileExist(String filePath) {
        File f = new File(filePath);
        return (f.exists() && !f.isDirectory());
    }

    private boolean isPortAvailable(int port) {
        // Assume the connection is possible.
        boolean result = true;
        try {
            (new Socket("localhost", port)).close();
            result = false;
        } catch(Exception ignored) { }
        return result;
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * *  Debug methods  * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public void printMap(Map map) {
        for (Object key : map.keySet()) {
            String keys = key.toString();
            Object value = map.get(key);
            String values = "";
            if (value instanceof AddressSet) {
                values = ((AddressSet) value).getString();
                System.out.println("    Emulator " + key + " - " + values);
            } else {
                System.out.println("" + value);
            }
        }
    }

    public void printArray(ArrayList array) {
        for (Object obj : array) {
            if (obj instanceof Emulator) {
                ((Emulator) obj).print();
            } else if (obj instanceof AddressSet) {
                String s = ((AddressSet) obj).getString();
                System.out.println(s);
            } else {
                System.out.println("" + obj);
            }
        }
    }

    public void printStructures() {
        System.out.println("Network ip = " + networkIp);
        System.out.println("Android sdk path = " + sdkPath);
        System.out.println("Termite2 server path = " + termiteServerPath);
        System.out.println("Platform = " + platform);
        System.out.println("");
        System.out.println("AvailableAddresses:");
        printArray(availableAddresses);
        System.out.println("");
        System.out.println("InstalledEmulators:");
        System.out.println(getInstalledEmulators().toString());
        System.out.println("");
        System.out.println("EmulatorsInstances:");
        printArray(emulatorsInstances);
        System.out.println("");
        System.out.println("AssignedEmulators:");
        printMap(assignedEmulators);
    }
}
