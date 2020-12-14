package pt.inesc.termite.cli.commands;

import pt.inesc.termite.cli.Backend;
import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.ConnectorDriver;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Emulation;
import pt.inesc.termite.cli.Experiment;
import pt.inesc.termite.cli.NetProfile;

public class BeginEmulationCommand extends Command {

    public BeginEmulationCommand(String name, String abrv) {
        super(name,abrv);
    }

    public BeginEmulationCommand() {
        super("beginemulation","-");
    }

    public boolean executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (args.length != 2) {
            printError("Wrong number of input arguments.");
            printHelp();
            return false;
        }

        /*
         * initialize an emulation object for the given experiment
         */

        Experiment exp = context.mConfigManager.getExperiments().get(args[1]);
        if (exp == null) {
            printError("Experiment id '" + args[1] + "' does not exist.");
            return false;
        }

        Backend backend = context.mConfigManager.getBackends().get((String)exp.getBackend());
        assert backend != null;

        Emulation emulation = context.mEmulations.get(backend);
        if (emulation != null) {
            printError("Emulation for backend '" + backend.getId() + "' is already in course.");
            return false;
        }

        emulation = new Emulation(backend, exp);

        /*
         * initialize the address provider of the connector driver
         */

        String npid = exp.getNetProfile();
        NetProfile netProfile = context.mConfigManager.getNetProfiles().get(npid);
        assert netProfile != null;

        if (!backend.getConnector().equals(netProfile.getConnector())) {
            printError("Incompatible connectors (" + backend.getConnector() +
                    " vs. " + netProfile.getConnector() + ").");
            return false;
        }

        ConnectorDriver ct = backend.getConnectorTarget();
        assert ct != null;

        try {
            ct.initializeAddressProvider(netProfile.getConfig());
        } catch(Exception e) {
            printError("Could not initialize the address provider.");
            System.out.println(e.getMessage());
            return false;
        }

        /*
         * everything went well, update the context
         */

        context.mEmulations.put(backend, emulation);
        context.mCurrentBackend = backend;
        context.mCurrentEmulation = emulation;
        context.getReader().setPrompt("\u001B[1m" + backend.getId() + ":" +
                exp.getId() + ">\u001B[0m ");

        return true;
    }

    public void printHelp() {
        System.out.println("Syntax: beginemulation <experiment-id>");
    }
}