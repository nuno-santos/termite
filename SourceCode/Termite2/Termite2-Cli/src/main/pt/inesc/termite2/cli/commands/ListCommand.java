package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

import java.io.File;
import java.util.*;

public class ListCommand extends Command {

    public ListCommand() {
        super("list","ls");
    }

    public String executeCommand(Context context, String[] args) {

        assert context != null && args != null;

        if (args.length != 2) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        Command [] commands = context.getCommands();
        String option = args[1];

        if (option.equals("emulators") || option.equals("emus")) {
            listEmulators(context);
            return OK;
        }
        if (option.equals("binds") || option.equals("b")) {
            listBinds(context);
            return OK;
        }
        if (option.equals("networks") || option.equals("nets")) {
            listControllerNetworks(context);
            return OK;
        }
        if (option.equals("scripts") || option.equals("scp")) {
            listScripts(context);
            return OK;
        }
        if (option.equals("history") || option.equals("h")) {
            listHistory(context);
            return OK;
        }

        /*
         * options related with ongoing emulation
         */

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();
        Groups groups = network.getGroups();

        if (option.equals("devices") || option.equals("d")) {
            listDevices(devices);
            return OK;
        }
        if (option.equals("neighbors") || option.equals("n")) {
            listNeighbors(devices);
            return OK;
        }
        if (option.equals("groups") || option.equals("g")) {
            listGroups(groups);
            return OK;
        }
        if (option.equals("tnetwork") || option.equals("tnet")) {
            listTermiteNetwork(network);
            return OK;
        }
        return returnError("Unknown option \"" + option + "\"");
    }

    protected void listDevices(Devices devices) {
        for(Device dev : devices.getDevices()) {
            System.out.println(dev.getName() + "\t" +
                    dev.getIp() + ":" + dev.getEmuPort() + "\t" +
                    dev.getCommitPort() + ":" + dev.getMessagePort());
        }
    }

    protected void listGroups(Groups groups) {
        for (Group group : groups.getGroups()) {
            System.out.print(group.getGo().getName() + " => ");
            int i = 0;
            for (Device client : group.getClientList()) {
                System.out.print(client.getName());
                i++;
                if (i < group.getClientList().size()) {
                    System.out.print(",");
                }
            }
            System.out.println("");
        }
    }

    protected void listNeighbors(Devices devices) {
        for (Device device: devices.getDevices()) {
            System.out.print(device.getName() + " => ");
            ArrayList<String> neighbors = device.getNeighbors();
            int i = 0;
            for (String client : neighbors) {
                System.out.print(client);
                i++;
                if (i < neighbors.size()) {
                    System.out.print(",");
                }
            }
            System.out.println();
        }
    }

    protected void listTermiteNetwork(Network network) {
        Devices devices = network.getDevices();
        Groups groups = network.getGroups();
        for (Device device: devices.getDevices()) {

            // list details of the neighbors of the current node
            System.out.println("Node " + device.getName());
            System.out.print("   Peers:");
            ArrayList<String> neighbors = device.getNeighbors();
            int i = 0;
            for (String client : neighbors) {
                System.out.print(client);
                i++;
                if (i < neighbors.size()) {
                    System.out.print(",");
                }
            }
            System.out.println();

            System.out.println("   Groups:");

            // Print the list of groups that the current node is client of
            String clientOf = groups.getStrGroupsContaining(device.getName());
            System.out.println("      ClientOf: " +
                    ((clientOf != null && !clientOf.equals(""))?clientOf:"No"));

            // Print details of the group that the current node is owner of
            Group group = groups.getGroup(device.getName());
            System.out.println("      GO: " + ((group != null)?"Yes":"No ") +
                    ((group != null)?(" (" + group.getClientsStr() + ")"):""));
        }
    }

    protected void listHistory(Context context) {
        String last = context.getHistory().getLast();
        if (last != null) {
            System.out.println(last);
        }
    }

    protected void listEmulators(Context context) {

        RemoteAVDController remoteAVDController = context.mRemoteAVDController;
        assert remoteAVDController != null;

        /*ArrayList<Emulator> emulators = remoteAVDController.getEmulators();
        EmusTracker et = context.mCurrentEmulation.getEmusTracker();
        et.updateEmuList(new ArrayList<>(emulators));*/

        EmusTracker et = context.mCurrentEmulation.getEmusTracker();
        HashMap<String,Emulator> map = et.getEmusList();
        for(String emuId : map.keySet()){
            Emulator emu = map.get(emuId);
            System.out.println(" => Emulator " + emuId +":");
            emu.print();
            System.out.println("");
        }

    }

    protected void listControllerNetworks(Context context) {
        for(String network : context.mConfigManager.getControllerNetworks()){
            System.out.println("    " + network);
        }
    }

    protected void listScripts(Context context) {
        File scriptFolder = new File((context.mConfigManager.getTermitePath() + "/scripts"));

        for(File file : Objects.requireNonNull(scriptFolder.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                System.out.println("    " + file.getName() );
            }
        }
    }

    protected void listBinds(Context context){
        for (Map.Entry<String, String> entry : context.mCurrentEmulation.getBinds().entrySet()) {
            String bind = "" + entry.getKey() + " => " + entry.getValue();
            System.out.println(bind);
        }
    }

    public void printHelp() {
        System.out.println("Syntax: list|ls <what>");
        System.out.println("Options:");
        System.out.println("   emulators|emus, networks|nets, devices|d, groups|g, neighbors|n,");
        System.out.println("   binds|b, tnetwork|tnet, scripts|scp, history|h");
    }

    public String getExplanation(){
        return "Lists relevant termite2 data.";
    }
}
