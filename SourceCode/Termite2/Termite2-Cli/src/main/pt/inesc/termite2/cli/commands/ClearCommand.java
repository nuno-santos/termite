package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

public class ClearCommand extends Command {

    public ClearCommand() {
        super("clear", Command.NULLABVR);
    }

    public String executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (args.length != 1) {
            return returnError("Wrong number of input arguments.");
        }

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();

        //Reset virtual network data on the connected emulators
        // TO DO

        //Updated command, binds all devices before clearing all network configurations
        context.mCurrentEmulation.clearBinds();

        Groups groups = network.getGroups();
        devices.clear();
        groups.clear();
        return OK;
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: clear");
    }

    public String getExplanation(){
        return "Clears current network state, this includes deleting any created device, group, and binds.";
    }

}
