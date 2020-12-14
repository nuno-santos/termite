package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;
import pt.inesc.termite.server.Emulator;
import pt.inesc.termite.server.exceptions.ControllerDriverException;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DestroyAvdsCommand extends Command {

    private AVDControllerDriver AVDController;

    public DestroyAvdsCommand() {
        super("destroyavds");
    }

    @Override
    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        ArrayList<String> result = new ArrayList<>();
        this.AVDController = AVDController;

        // Check if number of arguments received is valid
        if (msg.size() < 1 || msg.size() > 16) {
            result.add("Error: Wrong number of arguments");
            return result;
        }

        // Check if there are installed emulators
        ArrayList<String> installedEmus = AVDController.getInstalledEmulators();
        if (installedEmus.size() == 0) {
            result.add("Error: No emulators installed.");
            return result;
        }

        //Refresh emulators online
        AVDController.refreshAllRedirections();

        // Check for option destrouavds all
        if (msg.size() == 1 && msg.get(0).equals("all")) {
            msg = installedEmus;
        }


        ArrayList<String> onlineEmulators = getOnlineEmulatorNames();

        for (String avdName : msg) {
            if (!installedEmus.contains(avdName)) { // check if emulator is installed
                result.add("Avd " + avdName + " does not exist.");
            } else if (onlineEmulators.contains(avdName)) { // check if emulator is online
                result.add("Avd " + avdName + " online.");
            } else {
                try {
                    AVDController.destroyAvd(avdName);
                    TimeUnit.SECONDS.sleep(2);
                    result.add("Avd " + avdName + " deleted.");
                } catch (ControllerDriverException | InterruptedException e) {
                    result.add("Avd " + avdName + " deletion could not be verified.");
                }
            }
        }

        return result;
    }

    // HELPER METHODS

    private ArrayList<String> getOnlineEmulatorNames() {
        ArrayList<String> names = new ArrayList<>();

        for (Emulator emulator : AVDController.getEmulatorsInstances()) {
            names.add(emulator.get_name());
        }
        return names;
    }

    @Override
    public String cmdSyntax() {
        return "destroyavds [all | <avd1_name> <...>]";
    }

    @Override
    public String getExplanation() {
        return "Destroys the avds <avd1_name>, <...> passed in arguments. Online avds can not be destroyed.";
    }
}
