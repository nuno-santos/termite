package pt.inesc.termite.cli.commands;


import pt.inesc.termite.cli.Application;
import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.ConnectorDriver;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.EmuTracker;
import pt.inesc.termite.cli.Network;

public class InstallAppCommand extends Command {

    public InstallAppCommand(String name, String abrv) {
        super(name,abrv);
    }

    public InstallAppCommand() {
        super("installapp","-");
    }

    @SuppressWarnings("unchecked")
    public boolean executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (context.mCurrentEmulation == null) {
            printError("No emulation is currently ongoing.");
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
        String apkPath = "" + context.mConfigManager.getTermitePath() + "/../" +
                app.getRPath() + "/" + app.getApk();

        try {
            ct.installApp(eid, apkPath);
        } catch(Exception e) {
            printError("Could not install application on emulator '" + eid + "'.");
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    public void printHelp() {
        System.out.println("Syntax: installapp <emu-id>");
    }
}