package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;

import java.util.ArrayList;

public class InstalledEmulatorsCommand extends Command {

    public InstalledEmulatorsCommand() {
        super("installed");
    }

    @Override
    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        return AVDController.getInstalledEmulators();
    }

    @Override
    public String cmdSyntax() {
        return "installed";
    }

    @Override
    public String getExplanation() {
        return "Returns a list with all avd's installed on the machine.";
    }
}
