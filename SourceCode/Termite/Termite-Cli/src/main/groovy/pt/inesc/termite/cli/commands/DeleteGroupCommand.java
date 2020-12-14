package pt.inesc.termite.cli.commands;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Devices;
import pt.inesc.termite.cli.Groups;
import pt.inesc.termite.cli.Network;

public class DeleteGroupCommand extends Command {

	public DeleteGroupCommand(String name, String abrv) {
		super(name,abrv);
	}

	public DeleteGroupCommand() {
		super("deletegroup","dg");
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

		if (args.length != 2) {
			printError("Wrong number of input arguments.");
			printHelp();
			return false;
		}
		
		// Delete the group if it exists
		String go = args[1];
		if (!devices.existsDevice(go)) {
			printError("Group does not exist.");
			return false;
		}
		groups.deleteDevice(go);
		return true;
	}

	public void printHelp() {
		System.out.println("Syntax: deletegroup|dg <go>");
	}
}
