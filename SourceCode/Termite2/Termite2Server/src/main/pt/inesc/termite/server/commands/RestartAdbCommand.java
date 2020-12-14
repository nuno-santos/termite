package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;

import java.util.ArrayList;

public class RestartAdbCommand extends Command {

    public RestartAdbCommand(){super("restartadb");}
    @Override
    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        ArrayList<String> result = new ArrayList<>();
        System.out.println("ATTENTION only use this command if termite2 server appears \"stucked\" performing some operation.");
        AVDController.restartAdbServer();
        result.add("Adb restarted.");
        return result;
    }

    @Override
    public String cmdSyntax() {
        return "restartadb";
    }

    @Override
    public String getExplanation() {
        return "Restarts android adb bridge. ATTENTION only use this command if termite2 server appears \"stucked\" on some operation.";
    }
}
