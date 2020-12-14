package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;

import java.util.ArrayList;
import java.util.Hashtable;

public class CreateAvdsCommand extends Command {

    public CreateAvdsCommand() {
        super("createavds");
    }

    @Override
    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        ArrayList<String> result = new ArrayList<>();

        if (msg.size() != 3) {
            result.add("Error: Wrong number of arguments");
            return result;
        }

        String num = msg.get(0);
        String name = msg.get(1);
        String api = msg.get(2);

        if (Integer.parseInt(num) < 1 || name.length() < 1 || Integer.parseInt(api) < 21) {
            result.add(("Error: Invalid arguments values; num: " + num + ", name: " + name + ", api: " + api));
            return result;
        }

        String osVersion = "system-images;android-" + api + ";default;x86";
        if (!AVDController.checkIfInstalled(osVersion)) {
            result.add("Error: Android OS package " + osVersion + " is not installed.\n" +
                    "First install package with Android sdkmanager inside $ANDROID_SDK/tools/bin folder:\n" +
                    " - (./sdkmanager --install \"" + osVersion + "\"), on Linux or Mac OS.\n" +
                    " - (sdkmanager.bat --install \"" + osVersion + "\"), on Windows.");
            return result;
        }

        Hashtable<Thread, CreateAvdThread> createAvdThreads = new Hashtable<>();
        for (int i = 1; i <= Integer.parseInt(num); i++) {
            CreateAvdThread createavd = new CreateAvdThread((name + i), api, AVDController);
            Thread t = new Thread(createavd);
            t.start();
            createAvdThreads.put(t, createavd);
        }

        // wait for the termination of all start app threads
        for (Thread t : createAvdThreads.keySet()) {
            try {
                t.join();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        // Check start apps results
        for (CreateAvdThread installThread : createAvdThreads.values()) {
            if (!installThread.result) {
                result.add("Error: A problem occurred creating avd " + installThread._name + " from createavds script.");
            } else {
                result.add("Avd " + installThread._name + " created.");
            }
        }
        return result;
    }

    @Override
    public String cmdSyntax() {
        return "createavds <nº> <name> <api>";
    }

    @Override
    public String getExplanation() {
        return "Creates avd(s) with name: <name>, api: <api>, <nº> times. This OVERWRITES avd's with the same name.";
    }


    // Class thread to handle creation of each avd
    private static class CreateAvdThread implements Runnable {

        public boolean result;
        private String _name;
        private String _api;
        private AVDControllerDriver AVDController;

        public CreateAvdThread(String name, String api, AVDControllerDriver avd) {
            _name = name;
            _api = api;
            AVDController = avd;
        }

        @Override
        public void run() {
            result = AVDController.createEmulatorsFromScript(_name, _api);
        }
    }
}
