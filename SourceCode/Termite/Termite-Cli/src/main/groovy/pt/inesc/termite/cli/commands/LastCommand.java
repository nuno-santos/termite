package pt.inesc.termite.cli.commands;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Network;

public class LastCommand extends Command {
	
	public LastCommand(String name, String abrv) {
		super(name,abrv);
	}

	public LastCommand() {
		super("last", Command.NULLABVR);
	}

	public boolean executeCommand(Context context, String [] args) {

		assert context != null && args != null;

		boolean ret = false; // always return false to avoid updating the history

		String s = context.getHistory().getLast();
		if (s == null) {
			return ret;
		}

		String [] tokens = null;
		
		tokens = s.split("\\s+");
		if (tokens.length == 0) {
			return ret;
		}
		String cmd = tokens[0];
		if (cmd.equals("")) {
			return ret;
		}
		if (cmd.startsWith("#")) {
			return ret;
		}
		boolean found = false;
		for (Command command : context.getCommands()) {
			if (command.getName().equals(cmd) || command.getAbvr().equals(cmd)) {
				found = true;
				command.executeCommand(context, tokens);
				break;
			}
		}
		assert found;
		return ret;
	}	
}
