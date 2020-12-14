package pt.inesc.termite.cli.commands;

import java.util.Map;

import pt.inesc.termite.cli.Backend;
import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Network;

public class ChangeBackendCommand extends Command {

	public ChangeBackendCommand(String name, String abrv) {
		super(name,abrv);
	}

	public ChangeBackendCommand() {
		super("changebackend","cb");
	}

	public boolean executeCommand(Context context, String [] args) {

		assert context != null && args != null;

		if (args.length > 2) {
			printError("Wrong number of input arguments.");
			printHelp();
			return false;
		}

        if (context.mCurrentEmulation != null) {
            printError("Cannot change backend while an emulation is active.");
            return false;
        }

        if (args.length == 1) {
            context.mCurrentBackend = null;
            context.getReader().setPrompt("\u001B[1m>\u001B[0m ");
        } else {
            Map<String, Backend> backends = context.mConfigManager.getBackends();
            Backend backend = backends.get(args[1]);
            if (backend == null) {
                printError("Backend id '" + args[1] + "' does not exist.");
                return false;
            }
            context.mCurrentBackend = backend;
            context.getReader().setPrompt("\u001B[1m" + backend.getId() + ">\u001B[0m ");
        }
        return true;
	}

	public void printHelp() {
		System.out.println("Syntax: changebackend|cb [backend-id]");
	}
}
