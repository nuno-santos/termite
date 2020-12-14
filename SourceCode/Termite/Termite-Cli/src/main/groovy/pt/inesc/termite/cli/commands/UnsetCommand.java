package pt.inesc.termite.cli.commands;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Network;

public class UnsetCommand extends Command {
	
	public UnsetCommand(String name, String abrv) {
		super(name,abrv);
	}

	public UnsetCommand() {
		super("unset", Command.NULLABVR);
	}

	public boolean executeCommand(Context context, String [] args) {

		assert context != null && args != null;

		if (args.length != 2) {
			printError("Wrong number of input arguments.");
			printHelp();
			return false;
		}
		
		context.getProperties().removeProperty(args[1]);
		return true;
	}

	public void printHelp() {
		System.out.println("Syntax: unset <property>");
	}
}
