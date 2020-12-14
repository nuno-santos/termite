package pt.inesc.termite.cli.connectors.avddriver

import pt.inesc.termite.cli.AddressSet
import pt.inesc.termite.cli.ConfigManager
import pt.inesc.termite.cli.Connector
import pt.inesc.termite.cli.ConnectorDriver
import pt.inesc.termite.cli.IAddressProvider
import pt.inesc.termite.cli.exceptions.ConnectorTargetException


public class AVDConnectorDriver extends ConnectorDriver {

    private String mSdk
    private String mVmi
    private HashMap<String,Integer> mEmulators
    private HashMap<String,AddressSet> mAssignedEmulators
    private IAddressProvider mAddrProvider


    public AVDConnectorDriver(ConfigManager configManager, Connector connector, Map config)
            throws ConnectorTargetException {
        super(configManager, connector, config)
        if (config == null) {
            throw new ConnectorTargetException("Must provide configuration parameters.")
        }
        if (config['sdk'] == null) {
            throw new ConnectorTargetException("Must define the 'sdk' config paramenter.")
        }
        if (config['vmi'] == null) {
            throw new ConnectorTargetException("Must define the 'vmi' config parameter.")
        }
        mSdk = config['sdk']
        mVmi = config['vmi']
        mEmulators = new HashMap<>()
        mAssignedEmulators = new HashMap<>()
        mAddrProvider = null
    }

    /*
     * Connector driver methods
     */

    @Override
    public void check() throws ConnectorTargetException {

        println "Checking AVD connector driver..."

        try {
            "${mSdk}/tools/emulator -version".execute()
        } catch(Exception ignored) {
            throw new ConnectorTargetException("Android emulator tool not found.")
        }

        try {
            "${mSdk}/platform-tools/adb".execute()
        } catch(Exception ignored) {
            throw new ConnectorTargetException("Android adb tool not found.")
        }
    }

    @Override
    void deployEmulator() throws ConnectorTargetException {

        assert mSdk != null && mVmi != null

        String exec = "${mSdk}/tools/emulator -avd ${mVmi} -no-audio"

        Thread.start {
            try {
                def process = exec.execute()
                print "${process.text}"
//                if (eid != null) {
//                    // TODO - imeplement -> return real eid
//                    throw new ConnectorTargetException("Cannot use virtual ids with AVD.")
//                }
            } catch(ConnectorTargetException e) {
                throw e
            } catch (Exception e) {
                println "Error: Unable to deploy emulator."
                println e.getMessage()
            }
        }
    }

    @Override
    void killEmulator(String seid) throws ConnectorTargetException {

        assert mSdk != null

        if (seid == null) {
            throw new ConnectorTargetException("Emulator id is invalid.")
        }

        String exec = "${mSdk}/platform-tools/adb -s ${seid} emu kill"

        def process = exec.execute()
        print "${process.text}"
    }

    @Override
    void stopEmulator(String seid) throws ConnectorTargetException {

        throw new ConnectorTargetException("Method 'stopEmulator' not implemented.")

    }

    @Override
    void startEmulator(String seid) {

        throw new ConnectorTargetException("Method 'startEmulator' not implemented.")

    }

    @Override
    void initializeAddressProvider(Map params) {

        if (params == null) {
            throw new ConnectorTargetException("Addresses missing.")
        }

        mAssignedEmulators.clear()

        if (params.containsKey("manual")) {
            mAddrProvider = new ManualAVDAddressProvider(params);
            return;
        }

        if (params.containsKey("auto")) {
            mAddrProvider = new AutoAVDAddressProvider(params);
            return;
        }

        throw new ConnectorTargetException("Address assignment mode not supported.")
    }

    @Override
    AddressSet assignAddressSet(String eid) {

        if (mAddrProvider == null) {
            throw new ConnectorTargetException("Address provider was not initialized.")
        }

        if (eid == null) {
            throw new ConnectorTargetException("Emulator id missing.")
        }

        if (mAssignedEmulators[(eid)] != null) {
            throw new ConnectorTargetException("Addresses already assigned to '${eid}'.")
        }

        /*
         * obtain the emulator control port
         */

        int emuPort;
        String[] split = eid.tokenize("-") // expected eid in the form of "emulator-<port>"
        if (split == null || split.length != 2 || !split[0].equals("emulator")) {
            throw new ConnectorTargetException("Emulator id is invalid.")
        }
        try {
            emuPort = split[1].toInteger();
        } catch(Exception e) {
            throw new ConnectorTargetException("Emulator id is invalid (${e.getMessage()}")
        }

        /*
         * obtain an address set and update the CLI internal state right away
         */

        AddressSet addr = mAddrProvider.claimAddressSet()
        if (addr == null) {
            throw new ConnectorTargetException("There are no available address sets.")
        }
        mAssignedEmulators[(eid)] = addr
        mEmulators[(eid)] = EMU_STATE_NETOK

        /*
         * start from clean slate
         */

        silentResetAddressSet(eid)

        /*
         * perform port forwarding
         */

        int avport = getAddrPort(addr.mAVAddr);
        int arport = getAddrPort(addr.mARAddr);
        int cvport = getAddrPort(addr.mCVAddr);
        int crport = getAddrPort(addr.mCRAddr);
        if (avport < 0 || arport < 0 || cvport < 0 || crport < 0) {
            throw new ConnectorTargetException("Invalid port numbers.")
        }

        try {
            Socket s = new Socket("localhost", emuPort);
            s.withStreams { input, output ->
                BufferedReader reader = input.newReader()

                if (!checkRedirReturn(reader)) {
                    return
                }

                output << "redir add tcp:${crport}:${cvport}\n"
                if (!checkRedirReturn(reader)) {
                    return
                }

                output << "redir add tcp:${arport}:${avport}\n"
                if (!checkRedirReturn(reader)) {
                    return
                }

                output << "quit\n"
            }
            s.close()
        } catch (Exception e) {
            throw new ConnectorTargetException("Unable to install the port forwarding rules " +
                    "on the emulator (${e.getMessage()}")
        }

        return addr
    }

    @Override
    void unassignAddressSet(String eid) {

        if (mAddrProvider == null) {
            throw new ConnectorTargetException("Address provider was not initialized.")
        }

        if (eid == null) {
            throw new ConnectorTargetException("Emulator id missing.")
        }

        /*
         * check if the emulator is bound and obtain its address set
         */

        AddressSet addr = mAssignedEmulators[(eid)]
        if (addr == null) {
            silentResetAddressSet(eid)
            return
        }

        /*
         * update the internal state right away to prevent inconsistencies if redir doesn't work
         */

        mAddrProvider.releaseAddressSet(addr)
        mAssignedEmulators.remove(eid)
        if (mEmulators[(eid)] >= EMU_STATE_NETOK) {
            mEmulators[(eid)] = EMU_STATE_ONLINE
        }

        /*
         * obtain the emulator control port
         */

        int emuPort;
        String[] split = eid.tokenize("-") // expected eid in the form of "emulator-<port>"
        if (split == null || split.length != 2 || !split[0].equals("emulator")) {
            throw new ConnectorTargetException("Emulator id is invalid.")
        }
        try {
            emuPort = split[1].toInteger();
        } catch(Exception e) {
            throw new ConnectorTargetException("Emulator id is invalid (${e.getMessage()}")
        }

        /*
         * remove the port forwarding rules from the emulator
         */

        int avport = getAddrPort(addr.mAVAddr);
        int arport = getAddrPort(addr.mARAddr);
        int cvport = getAddrPort(addr.mCVAddr);
        int crport = getAddrPort(addr.mCRAddr);
        if (avport < 0 || arport < 0 || cvport < 0 || crport < 0) {
            throw new ConnectorTargetException("Invalid port numbers.")
        }

        try {
            Socket s = new Socket("localhost", emuPort);
            s.withStreams { input, output ->
                BufferedReader reader = input.newReader()

                if (!checkRedirReturn(reader)) {
                    println "Checking header failed."
                    return
                }

                output << "redir del tcp:${crport}\n"
                if (!checkRedirReturn(reader)) {
                    println "Redir del tcp ${crport} failed."
                    return
                }

                output << "redir del tcp:${arport}\n"
                if (!checkRedirReturn(reader)) {
                    println "Redir del tcp ${arport} failed."
                    return
                }

                output << "quit\n"
            }
            s.close()
        } catch (Exception e) {
            throw new ConnectorTargetException("Unable to install the port forwarding rules " +
                    "on the emulator (${e.getMessage()}")
        }
    }

    @Override
    void finalizeAddressProvider() {

        mAssignedEmulators.clear()
        mAddrProvider = null

    }

    @Override
    void installApp(String eid, String apkPath) {

        assert mSdk != null

        if (eid == null) {
            throw new ConnectorTargetException("Emulator id is invalid.")
        }
        if (apkPath == null) {
            throw new ConnectorTargetException("APK path is invalid.")
        }
        def file = new File("${apkPath}")
        if (!file.exists()) {
            throw new ConnectorTargetException("APK not found: ${apkPath}.")
        }

        def process1 = """./adb -s ${eid} install -r ${apkPath}""".execute(
                null, new File("${mSdk}/platform-tools")
        )

        println "${process1.text}"

        /*
        // FIXME - turn off warnings. Is there a better way to do this? - rodrigo
        def process2 = """grep -v WARNING""".execute()
        process1 | process2
        process2.waitFor()
        println process2.err.text
        println process2.in.text
        */
    }

    @Override
    void runApp(String eid, String appId, String activity) {

        assert mSdk != null

        if (eid == null) {
            throw new ConnectorTargetException("Emulator id is invalid.")
        }
        if (appId == null) {
            throw new ConnectorTargetException("App ID is invalid.")
        }
        if (activity == null) {
            throw new ConnectorTargetException("Activity is invalid.")
        }

        def process = """./adb -s ${eid} shell am start -n ${appId}/${activity} -a android.intent.action.MAIN -c android.intent.category.LAUNCHER""".execute(
                null, new File("${mSdk}/platform-tools")
        )

        print "${process.text}"
    }

    @Override
    Map<String,Integer> getEmulators() throws ConnectorTargetException {

        assert mSdk != null

        String exec = "${mSdk}/platform-tools/adb devices"

        def process = exec.execute()

        HashMap<String,String> currList = new HashMap<String,String>()

        def out = process.text
        out.eachLine { line, count ->
            if (count > 0) {
                String[] split = line.tokenize(" \t")
                if (split.length == 2) {
                    currList[(split[0])] = split[1]
                }
            }
        }

        // first remove the stale entries from the emulators list
        HashMap<String,Integer> emuTmp = new HashMap<String,Integer>()
        for (String e : mEmulators.keySet()) {
            if (currList[(e)] != null) {
                emuTmp[(e)] = mEmulators[(e)]
            }
        }
        mEmulators = emuTmp;

        // then, update the current state of the emulators
        for (String s : currList.keySet()) {
            String state = currList[(s)]
            if (mEmulators[(s)] == null ||                  // doesn't exist yet
                    mEmulators[(s)] < EMU_STATE_ONLINE) {   // is in INIT or OFFLINE states
                if (state.equals("offline")) {
                    mEmulators[(s)] = EMU_STATE_OFFLINE
                    continue
                }
                if (state.equals("device")) {
                    mEmulators[(s)] = EMU_STATE_ONLINE
                    continue
                }
                //throw new ConnectorTargetException("Unknown emulator state.")
            }
        }

        return mEmulators
    }

    /*
     * Address provider implementations for manual and automatic address management
     */

    class ManualAVDAddressProvider implements IAddressProvider{

        private ArrayList<AddressSet> mASListFree;
        private ArrayList<AddressSet> mASListBusy;

        public ManualAVDAddressProvider(Map params) {

            mASListFree = new ArrayList<>()
            mASListBusy = new ArrayList<>()

            try {
                for (Map aset : params.manual) {
                    String avaddr = aset["avaddr"]
                    String araddr = aset["araddr"]
                    String cvaddr = aset["cvaddr"]
                    String craddr = aset["craddr"]
                    mASListFree.add(new AddressSet(avaddr,araddr,cvaddr,craddr))
                }
            } catch (Exception e) {
                throw new ConnectorTargetException("Unable to parse netprofile parameters.\n" +
                        e.getMessage())
            }
        }

        @Override
        AddressSet claimAddressSet() throws ConnectorTargetException {
            if (mASListFree.size() > 0) {
                AddressSet addressSet = mASListFree.remove(0)
                mASListBusy.add(addressSet)
                return addressSet
            }
            return null
        }

        @Override
        void releaseAddressSet(AddressSet addressSet) throws ConnectorTargetException {
            if (mASListBusy.contains(addressSet)) {
                mASListBusy.remove(addressSet)
                mASListFree.add(addressSet)
                return
            }
            throw new ConnectorTargetException("Address set cannot be released.")
        }
    }

    class AutoAVDAddressProvider implements IAddressProvider{

        public AutoAVDAddressProvider(Map params) {
        }

        @Override
        AddressSet claimAddressSet() throws ConnectorTargetException {
            throw new ConnectorTargetException("Auto AVD address provider not implemented.")
        }

        @Override
        void releaseAddressSet(AddressSet addressSet) throws ConnectorTargetException {
            throw new ConnectorTargetException("Auto AVD address provider not implemented.")
        }
    }

    /*
     * Internal helper methods
     */

    private static void silentResetAddressSet(String eid) {

        if (eid == null) {
            return
        }

        int emuPort = getEmulatorPort(eid)
        if (emuPort < 0) {
            return
        }

        /*
         * remove all port forwarding rules from the emulator
         */

        try {
            Socket s = new Socket("localhost", emuPort);
            s.withStreams { input, output ->
                BufferedReader reader = input.newReader()

                if (!checkRedirReturn(reader)) {
                    return
                }

                output << "redir list\n"
                ArrayList<String> redirList = parseRedirList(reader)
                if (redirList == null) {
                    return
                }

                for (String redir : redirList) {
                    output << "redir del ${redir}\n"
                    if (!checkRedirReturn(reader)) {
                        return
                    }
                }

                output << "quit\n"
            }
            s.close()
        } catch (Exception e) {
        }
    }

    private static boolean checkRedirReturn(reader) {
        while (true) {
            String buffer = reader.readLine()
            if (buffer.toString().startsWith("KO")) {
                println "Error: ${buffer}"
                return false;
            }
            if (buffer.toString().startsWith("OK")) {
                return true;
            }
        }
    }

    private static ArrayList<String> parseRedirList(reader) {
        ArrayList<String> redirList = new ArrayList<>()
        while (true) {
            String buffer = reader.readLine()
            if (buffer.toString().startsWith("KO")) {
                println "Error: ${buffer}"
                return null;
            }
            if (buffer.toString().startsWith("OK")) {
                return redirList;
            }
            if (buffer.toString().startsWith("no active redirections")) {
                continue
            }
            String[] split = buffer.tokenize(" ") // expected something like "tcp:9011  => 9001"
            if (split != null && split.length > 0) {
                redirList.add(split[0])
            }
        }
    }

    private static int getEmulatorPort(String eid) {
        int emuPort;
        String[] split = eid.tokenize("-") // expected eid in the form of "emulator-<port>"
        if (split == null || split.length != 2 || !split[0].equals("emulator")) {
            return -1
        }
        try {
            emuPort = split[1].toInteger();
        } catch(Exception e) {
            return -1
        }
        return emuPort
    }

    private static int getAddrPort(String addr) {
        int port;
        if (addr == null) {
            return -1;
        }
        String[] split = addr.tokenize(":") // expected address "<ip>-<port>"
        if (split == null || split.length != 2) {
            return -1
        }
        try {
            port = split[1].toInteger();
        } catch(Exception e) {
            return -1
        }
        return port;
    }

}
