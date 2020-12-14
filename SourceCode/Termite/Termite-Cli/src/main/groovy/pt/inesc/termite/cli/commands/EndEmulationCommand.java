package pt.inesc.termite.cli.commands;


import java.util.Map;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.ConnectorDriver;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Network;

public class EndEmulationCommand extends Command {

    public EndEmulationCommand(String name, String abrv) {
        super(name,abrv);
    }

    public EndEmulationCommand() {
        super("endemulation","-");
    }

    @SuppressWarnings("unchecked")
    public boolean executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (args.length != 1) {
            printError("Wrong number of input arguments.");
            printHelp();
            return false;
        }

        if (context.mCurrentEmulation == null) {
            printError("No emulation is currently active.");
            return false;
        }
        assert context.mCurrentBackend != null;

        /*
         * unassign all currently assigned emulators
         */

        ConnectorDriver ct = context.mCurrentBackend.getConnectorTarget();
        assert ct != null;

        for (String emu : context.mCurrentEmulation.getAssignedAddresses().keySet()) {
            try {
                ct.unassignAddressSet(emu);
            } catch(Exception e) {
                printError("Could not unassign addresses.");
                System.out.println(e.getMessage());
                return false;
            }
        }

        /*
         * finalize the address provider of the connector driver
         */

        try {
            ct.finalizeAddressProvider();
        } catch(Exception e) {
            printError("Could not finalize the address provider.");
            System.out.println(e.getMessage());
            return false;
        }

        /*
         * update internal data structures
         */

        context.mEmulations.remove(context.mCurrentBackend);
        context.mCurrentEmulation = null;
        context.mCurrentBackend = null;
        context.getReader().setPrompt("\u001B[1m>\u001B[0m ");

        return true;
    }

    public void printHelp() {
        System.out.println("Syntax: endemulation");
    }
}