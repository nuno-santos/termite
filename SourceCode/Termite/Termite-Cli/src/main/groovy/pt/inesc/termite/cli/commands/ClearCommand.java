package pt.inesc.termite.cli.commands;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Devices;
import pt.inesc.termite.cli.Groups;
import pt.inesc.termite.cli.Network;


public class ClearCommand extends Command {
	
	public ClearCommand(String name, String abrv) {
		super(name,abrv);
	}

	public ClearCommand() {
		super("clear", Command.NULLABVR);
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

		if (args.length != 1) {
			printError("Wrong number of input arguments.");
			return false;
		}

		devices.clear();
		groups.clear();
		return true;
	}
}
