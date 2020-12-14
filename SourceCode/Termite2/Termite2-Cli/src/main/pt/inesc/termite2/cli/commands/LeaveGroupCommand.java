package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class LeaveGroupCommand extends Command {

    public LeaveGroupCommand() {
        super("leavegroup","lg");
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

        // Check that the device exists
        String target = args[1];
        if (!devices.existsDevice(target)) {
            return returnError("Targeted device is not registered.");
        }

        // Parse GOs list
        Set<String> gos = new TreeSet<String>();
        String nlist = args[2];
        if (!nlist.startsWith("(") || !nlist.endsWith(")")) {
            printHelp();
            return returnError("Clients list malformed.");
        }
        nlist = nlist.substring(1, nlist.length() - 1);
        if (nlist != null && !nlist.equals("")) {
            Collections.addAll(gos,nlist.split(","));
        }

        // check that the GOs exist and are reachable to the target
        for (String go : gos) {
            if (!groups.existsGroup(go)) {
                return returnError("GO device \"" + go + "\" does not exist.");
            }
            if (!groups.getGroup(go).hasClient(target)) {
                return returnError("Device \"" + target + "\" is not client of \"" + go + "\".");
            }
        }

        // OK. remove the target device from the desired groups
        Device targetDevice = devices.getDevice(target);
        for (String go : gos) {
            ArrayList<Device> group = groups.getGroup(go).getClientList();
            group.remove(targetDevice);
        }
        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: leavegroup|lg <client> (<go_1>,...,<go_n>)");
    }

    public String getExplanation(){
        return "Removes chosen device from chosen group.";
    }
}
