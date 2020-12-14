package pt.inesc.termite.cli.commands;

import java.util.ArrayList;
import java.util.Collections;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Device;
import pt.inesc.termite.cli.Devices;
import pt.inesc.termite.cli.Group;
import pt.inesc.termite.cli.Groups;
import pt.inesc.termite.cli.Network;

public class MoveCommand extends Command {

	public MoveCommand(String name, String abrv) {
		super(name,abrv);
	}

	public MoveCommand() {
		super("move","m");
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
		
		// validate the target node
		String target = args[1];
		if (!devices.existsDevice(target)) {
			printError("Targeted device is not registered.");
			return false;
		}

		// parse neighbors list
		ArrayList<String> neighbors = new ArrayList<String>();
		String nlist = args[2];
		if (!nlist.startsWith("(") || !nlist.endsWith(")")) {
			printError("Neighbors list malformed.");
			printHelp();
			return false;
		}
		nlist = nlist.substring(1, nlist.length() - 1);
		if (!nlist.equals("")) {
			Collections.addAll(neighbors,nlist.split(","));
		}

		// check that the neighbors exist
		for (String neighbor : neighbors) {
			if (!devices.existsDevice(neighbor)) {
				printError("Device \"" + neighbor + "\" of the neighborhood list does not exist.");
				return false;
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
		return true;
	}

	public void printHelp() {
		System.out.println("Syntax: move|m <target> (<peer_1>,...,<peer_n>)");
	}
}
