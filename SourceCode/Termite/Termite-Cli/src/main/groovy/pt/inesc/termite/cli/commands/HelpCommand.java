package pt.inesc.termite.cli.commands;

import java.util.TreeMap;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Network;

/**
 * Print the list of supported commands
 *
 */
public class HelpCommand extends Command {
	
	public HelpCommand(String name, String abrv) {
		super(name,abrv);
	}

	public HelpCommand() {
		super("help","h");
	}

	public boolean executeCommand(Context context, String [] args) {

		assert context != null && args != null;

        if (args.length > 1) {
			if (args[1].equals("properties")) {
				printHelpProperties(context, args);
			} else if (args[1].equals("commands")) {
				printHelpCommands(context, args);
			} else {
				printHelp();
				return false;
			}
			return true;
		}

		System.out.println("Properties:");
		printHelpProperties(context, args);
		System.out.println();
		System.out.println("Commands:");
		printHelpCommands(context, args);
		return true;
	}
	
	private boolean printHelpProperties(Context context,
			String [] args) {
		
		Command [] commands = context.getCommands();
		TreeMap<String,String> propList = new TreeMap<String,String> ();
		for (Command cmd : commands) {
			String [] props = cmd.getProperties();
			if (props == null) {
				continue;
			}
			for (int i = 0; i < props.length; i++) {
				propList.put(props[i], cmd.getName());
			}
		}
		for (String prop : propList.keySet()) {
			System.out.println(prop + " (" + propList.get(prop) + ")");
		}

		return true;
	}
	
	private boolean printHelpCommands(Context context,
			String [] args) {
	
		Command [] commands = context.getCommands();
		for (Command cmd : commands) {
			System.out.println(cmd.getName() + " (" + cmd.getAbvr() + ")");
		}

		return true;
	}
	
	public void printHelp() {
		System.out.println("Syntax: help|h [properties|commands]");
	}
}
