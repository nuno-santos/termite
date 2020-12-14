package pt.inesc.termite.cli.commands;


import pt.inesc.termite.cli.Application;
import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.ConnectorDriver;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.EmuTracker;
import pt.inesc.termite.cli.Network;

public class RunAppCommand extends Command {

    public RunAppCommand(String name, String abrv) {
        super(name,abrv);
    }

    public RunAppCommand() {
        super("runapp","run");
    }

    public boolean executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (context.mCurrentEmulation == null) {
            printError("No emulation is currently active.");
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

        String appId = context.mCurrentEmulation.getExperiment().getApplication();
        Application app = context.mConfigManager.getApplications().get(appId);
        assert app != null;

        try {
            ct.runApp(eid, app.getAppId(), app.getActivity());
        } catch(Exception e) {
            printError("Could not run application on emulator '" + eid + "'.");
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    public void printHelp() {
        System.out.println("Syntax: runapp|run <emu-id>");
    }
}