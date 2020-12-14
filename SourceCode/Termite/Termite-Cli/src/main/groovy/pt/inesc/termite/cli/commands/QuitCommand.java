package pt.inesc.termite.cli.commands;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Network;

public class QuitCommand extends Command {
	
	public QuitCommand(String name, String abrv) {
		super(name,abrv);
	}

	public QuitCommand() {
		super("quit","q");
	}

	public boolean executeCommand(Context context, String [] args) {

		assert context != null && args != null;

		System.exit(0);
		return true;
	}
}
