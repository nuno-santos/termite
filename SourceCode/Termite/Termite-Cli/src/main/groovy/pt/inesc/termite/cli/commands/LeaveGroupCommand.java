package pt.inesc.termite.cli.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Device;
import pt.inesc.termite.cli.Devices;
import pt.inesc.termite.cli.Groups;
import pt.inesc.termite.cli.Network;

public class LeaveGroupCommand extends Command {

	public LeaveGroupCommand(String name, String abrv) {
		super(name,abrv);
	}

	public LeaveGroupCommand() {
		super("leavegroup","lg");
	}

	public boolean executeCommand(Context context, String [] args) {

		assert context != null && args != null;

        if (context.mCurrentEmulation == null) {
            printError("No emulation is currently active.");
            return false;
        }

        Network network = context.mCurrentEmulation.getNetwork();
		Devices devices = network.getDevices();
		Groups groups = network.getGroups();

		if (args.length != 3) {
			printError("Wrong number of input arguments.");
			printHelp();
			return false;
		}
		
		// Check that the device exists
		String target = args[1];
		if (!devices.existsDevice(target)) {
			printError("Targeted device is not registered.");
			return false;
		}

		// Parse GOs list
		Set<String> gos = new TreeSet<String>();
		String nlist = args[2];
		if (!nlist.startsWith("(") || !nlist.endsWith(")")) {
			printError("Clients list malformed.");
			printHelp();
			return false;
		}
		nlist = nlist.substring(1, nlist.length() - 1);
		if (nlist != null && !nlist.equals("")) {
			Collections.addAll(gos,nlist.split(","));
		}

		// check that the GOs exist and are reachable to the target
		for (String go : gos) {
			if (!groups.existsGroup(go)) {
				printError("GO device \"" + go + "\" does not exist.");
				return false;
			}
			if (!groups.getGroup(go).hasClient(target)) {
				printError("Device \"" + target + "\" is not client of \"" + go + "\".");
				return false;
			}
		}

		// OK. remove the target device from the desired groups
		Device targetDevice = devices.getDevice(target);
		for (String go : gos) {
			ArrayList<Device> group = groups.getGroup(go).getClientList();
			group.remove(targetDevice);
		}
		return true;
	}

	public void printHelp() {
		System.out.println("Syntax: leavegroup|lg <client> (<go_1>,...,<go_n>)");
	}
}
