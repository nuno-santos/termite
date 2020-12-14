package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;
import pt.inesc.termite.server.exceptions.ControllerDriverException;

import java.util.ArrayList;
import java.util.Arrays;


public class RefreshEmulatorsCommand extends Command {

    public RefreshEmulatorsCommand() {
        super("refresh");
    }

    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        System.out.println("Executing RefreshCommand...");
        ArrayList<String> result = new ArrayList<>();


        //refresh emulators instances and port redirections
        AVDController.refreshAllRedirections();

        //get new assign emulators
        String emulatorsString = AVDController.getEmulatorsString();

        if (emulatorsString != null) {
            String[] lines = emulatorsString.split("\\r?\\n");
            result = new ArrayList<>(Arrays.asList(lines));
        }

        System.out.println("RefreshCommand executed, result = " + result);
        return result;

    }

    @Override
    public String cmdSyntax() {
        return "refresh";
    }

    @Override
    public String getExplanation() {
        return "Refreshes all online emulators and port redirections.";
    }
}

