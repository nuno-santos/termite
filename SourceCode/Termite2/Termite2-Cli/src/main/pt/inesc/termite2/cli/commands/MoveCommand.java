package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

import java.util.ArrayList;
import java.util.Collections;

public class MoveCommand extends Command {

    public MoveCommand() {
        super("move","m");
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

        // validate the target node
        String target = args[1];
        if (!devices.existsDevice(target)) {
            return returnError("Targeted device is not registered.");
        }

        // parse neighbors list
        ArrayList<String> neighbors = new ArrayList<String>();
        String nlist = args[2];
        if (!nlist.startsWith("(") || !nlist.endsWith(")")) {
            printHelp();
            return returnError("Neighbors list malformed.");
        }
        nlist = nlist.substring(1, nlist.length() - 1);
        if (!nlist.equals("")) {
            Collections.addAll(neighbors,nlist.split(","));
        }

        // check that the neighbors exist
        for (String neighbor : neighbors) {
            if (!devices.existsDevice(neighbor)) {
                return returnError("Device \"" + neighbor + "\" of the neighborhood list does not exist.");
            }
        }

        // update the neighborhood list
        for (Device device : devices.getDevices()) {
            ArrayList<String> dn = device.getNeighbors();
            if (device.getName().equals(target)) {
                dn.clear();
                dn.addAll(neighbors);
            } else {
                dn.remove(target);
                if (neighbors.contains(device.getName())) {
                    dn.add(target);
                }
            }
        }

        // update the group list
        for (Group group : groups.getGroups()) {
            ArrayList<Device> toDelete = new ArrayList<Device>();
            for (Device client : group.getClientList()) {
                if (!group.getGo().hasNeighbor(client.getName())) {
                    toDelete.add(client);
                }
            }
            group.getClientList().removeAll(toDelete);
        }
        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: move|m <target> (<peer_1>,...,<peer_n>)");
    }

    public String getExplanation(){
        return "Emulates the movement of a chosen device to the vicinity of another.";
    }
}
