package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

public class DeleteGroupCommand extends Command {

    public DeleteGroupCommand() {
        super("deletegroup","dg");
    }

    public String executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();
        Groups groups = network.getGroups();

        if (args.length != 2) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        // Delete the group if it exists
        String go = args[1];
        if (!devices.existsDevice(go)) {
            return returnError("Group does not exist.");
        }
        groups.deleteDevice(go);
        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: deletegroup|dg <go>");
    }

    public String getExplanation(){
        return "Deletes chosen group.";
    }
}
