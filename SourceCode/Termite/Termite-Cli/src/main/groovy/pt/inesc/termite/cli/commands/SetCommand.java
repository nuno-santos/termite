package pt.inesc.termite.cli.commands;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Network;

public class SetCommand extends Command {
	
	public SetCommand(String name, String abrv) {
		super(name,abrv);
	}

	public SetCommand() {
		super("set", Command.NULLABVR);
	}

	public boolean executeCommand(Context context, String [] args) {

		assert context != null && args != null;

		if (args.length != 2) {
			printError("Wrong number of input arguments.");
			printHelp();
			return false;
		}
		
		context.getProperties().addProperty(args[1]);
		return true;
	}
	
	public void printHelp() {
		System.out.println("Syntax: set <property>");
	}
}
