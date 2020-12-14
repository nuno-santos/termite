package pt.inesc.termite.cli.commands;


import java.util.ArrayList;
import java.util.HashMap;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.ConnectorDriver;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.EmuTracker;
import pt.inesc.termite.cli.Network;

public class KillEmulatorCommand extends Command {

    public KillEmulatorCommand(String name, String abrv) {
        super(name,abrv);
    }

    public KillEmulatorCommand() {
        super("killemulator","kemu");
    }

    public boolean executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (context.mCurrentBackend == null) {
            printError("No backend is currently active.");
            return false;
        }

        if (args.length != 2) {
            printError("Wrong number of input arguments.");
            printHelp();
            return false;
        }

        EmuTracker et = context.mCurrentEmulation.getEmuTracker();
        String eid = et.getEmuList().get(args[1]);
        if (eid == null) {
            printError("Emulator '" + args[1] + "' does not exist.");
            return false;
        }

        ConnectorDriver ct = context.mCurrentBackend.getConnectorTarget();
        assert ct != null;

        try {
            ct.killEmulator(eid);
        } catch(Exception e) {
            printError("Could not kill emulator '" + eid + "'.");
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    public void printHelp() {
        System.out.println("Syntax: killemulator|kemu <emulator-id>");
    }
}
