package pt.inesc.termite.cli.commands;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Device;
import pt.inesc.termite.cli.Devices;
import pt.inesc.termite.cli.Group;
import pt.inesc.termite.cli.Groups;
import pt.inesc.termite.cli.Network;

public class CreateGroupCommand extends Command {

	public CreateGroupCommand(String name, String abrv) {
		super(name,abrv);
	}

	public CreateGroupCommand() {
		super("creategroup","cg");
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
		
		// check that the GO exists and is not bound to a previous group yet
		String go = args[1];
		if (!devices.existsDevice(go)) {
			printError("Targeted device is not registered.");
			return false;
		}
		if (groups.existsGroup(go)) {
			printError("Node \"" + go + "\" already owns a group.");
			return false;
		}

		// parse client list
		Set<String> clients = new TreeSet<String>();
		String nlist = args[2];
		if (!nlist.startsWith("(") || !nlist.endsWith(")")) {
			printError("Clients list malformed.");
			printHelp();
			return false;
		}
		nlist = nlist.substring(1, nlist.length() - 1);
		if (nlist != null && !nlist.equals("")) {
			Collections.addAll(clients,nlist.split(","));
		}

		// check that the clients exist and is reachable to the GO
		Device goDevice = devices.getDevice(go);
		for (String client : clients) {
			if (!devices.existsDevice(client)) {
				printError("Device \"" + client + "\" does not exist.");
				return false;
			}
			if (!goDevice.hasNeighbor(client)) {
				printError("Device \"" + client + "\" is not reachable to \"" + go + "\".");
				return false;
			}
		}

		// OK. create group and add it to the group list
		Group group = new Group(devices.getDevice(go));
		for (String client : clients) {
			group.getClientList().add(devices.getDevice(client));
		}
		groups.addGroup(group);
		return true;
	}

	public void printHelp() {
		System.out.println("Syntax: creategroup|cg <go> (<client_1>,...,<client_n>)");
	}
}
