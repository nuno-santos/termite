package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class CreateGroupCommand extends Command {

    public CreateGroupCommand() {
        super("creategroup","cg");
    }

    public String executeCommand(Context context, String [] args) {
        assert context != null && args != null;

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();
        Groups groups = network.getGroups();

        if (args.length != 3) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        // check that the GO exists and is not bound to a previous group yet
        String go = args[1];
        if (!devices.existsDevice(go)) {
            return returnError("Targeted device is not registered.");
        }
        if (groups.existsGroup(go)) {
            return returnError("Node \"" + go + "\" already owns a group.");
        }

        // parse client list
        Set<String> clients = new TreeSet<String>();
        String nlist = args[2];
        if (!nlist.startsWith("(") || !nlist.endsWith(")")) {
            printHelp();
            return returnError("Clients list malformed.");
        }
        nlist = nlist.substring(1, nlist.length() - 1);
        if (nlist != null && !nlist.equals("")) {
            Collections.addAll(clients,nlist.split(","));
        }

        // check that the clients exist and is reachable to the GO
        Device goDevice = devices.getDevice(go);
        for (String client : clients) {
            if (!devices.existsDevice(client)) {
                return returnError("Device \"" + client + "\" does not exist.");
            }
            if (!goDevice.hasNeighbor(client)) {
                return returnError("Device \"" + client + "\" is not reachable to \"" + go + "\".");
            }
        }

        // OK. create group and add it to the group list
        Group group = new Group(devices.getDevice(go));
        for (String client : clients) {
            group.getClientList().add(devices.getDevice(client));
        }
        groups.addGroup(group);
        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: creategroup|cg <go> (<client_1>,...,<client_n>)");
    }

    public String getExplanation(){
        return "Creates a Termite2 P2P group between chosen devices.";
    }
}
