package pt.inesc.termite.cli.commands;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Network;

/**
 * Loads and executes a sequence of commands from a file
 *
 */
public class LoadCommand extends Command {
	
	private static String SCRIPTS_DIR = "scripts:" + "scripts/devices:" + 
			"scripts/examples:" + "scripts/usecases:" + "scripts/testing";
	private static String SCRIPTS_EXT = ".termite";
	
	public LoadCommand(String name, String abrv) {
		super(name,abrv);
	}

	public LoadCommand() {
		super("load","l");
	}

	public boolean executeCommand(Context context, String [] args) {

		assert context != null && args != null;

		if (args.length < 2 || args.length > 3) {
			printError("Wrong number of input arguments.");
			return false;
		}

		// validate the file name
		String file = args[1];
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			String [] searchDirs = SCRIPTS_DIR.split(":");
            String path = context.mConfigManager.getTermitePath();
			for (int i = 0; i < searchDirs.length; i++) {
				try {
					in = new BufferedReader(new FileReader(path + "/" + searchDirs[i]
							+ "/" + file + SCRIPTS_EXT));
					break;
				} catch (FileNotFoundException f) {
				}
			}
			if (in == null) {
				printError("File \"" + file + SCRIPTS_EXT + "\" not found.");
				return false;
			}
		}

        boolean echoOn = true;
        if (args.length == 3) {
            if (args[2].equals("-q")) {
                echoOn = false;
            } else {
                printError("Unknown option'" + args[2] + "'.");
                return false;
            }
        }

		// parse and execute the commands
		String s = null;
		String [] tokens = null;
		while (true) {
			try {
				s = in.readLine();
				if (s == null) {
					return true;
				}
				s = s.trim();
				if (s.equals("")) continue;
                if (echoOn) {
                    System.out.println("$" + s);
                }
				tokens = s.split("\\s+");
				if (tokens.length == 0) {
					continue;
				}
				String cmd = tokens[0];
				if (cmd.equals("")) {
					continue;
				}
				if (cmd.startsWith("#")) {
					continue;
				}
				boolean found = false;
				for (Command command : context.getCommands()) {
					if (command.getName().equals(cmd) || command.getAbvr().equals(cmd)) {
						found = true;
						command.executeCommand(context, tokens);
						break;
					}
				}
				if (!found)
					Command.printWrongCommand(cmd);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
}
