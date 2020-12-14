package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;
import pt.inesc.termite.server.Emulator;
import pt.inesc.termite.server.exceptions.ControllerDriverException;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

public class InstallApplicationCommand extends Command {

    private AVDControllerDriver AVDController;

    public InstallApplicationCommand() {
        super("installapp");
    }

    @Override
    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        ArrayList<String> result = new ArrayList<>();
        this.AVDController = AVDController;
        ArrayList<String> emusOnlineName = new ArrayList<>();
        ArrayList<String> installationTargets = new ArrayList<>();

        if (msg.size() < 2) {
            result.add("Error: Wrong number of arguments");
            return result;
        }

        String apkName = msg.get(0);
        msg.remove(0);
        // check if apk exists
        if (!doesApkExist(apkName)) {
            result.add("Error: Apk file " + apkName + " not found at apks folder.");
            return result;
        }

        // refresh emulators instances and port redirection
        AVDController.refreshAllRedirections();

        emusOnlineName = getOnlineEmulatorNames();
        if (emusOnlineName.size() == 0) {
            result.add("Error: All emulators offline");
            return result;
        }

        // check were to install
        if (msg.size() == 1 && msg.get(0).equals("all")) { // On all online emulatos
            installationTargets.addAll(emusOnlineName);
        } else { // On specified emulators
            for (String emuName : msg) {
                if (emusOnlineName.contains(emuName)) {
                    installationTargets.add(emuName);
                } else {
                    result.add(emuName + ": Failure [Emulator not online]");
                }
            }
        }

        ArrayList<Emulator> emusOnline = AVDController.getEmulatorsInstances();
        Hashtable<Thread, InstallAppThread> installAppThreads = new Hashtable<>();

        for (Emulator emulator : emusOnline) {
            if (installationTargets.contains(emulator.get_name())) {
                InstallAppThread installThread = new InstallAppThread(emulator.get_name(), emulator.get_port(), apkName, AVDController);
                Thread t = new Thread(installThread);
                t.start();
                installAppThreads.put(t, installThread);
            }
        }

        // wait for the termination of all start app threads
        for (Thread t : installAppThreads.keySet()) {
            try {
                t.join();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        // Check start apps results
        for (InstallAppThread installThread : installAppThreads.values()) {
            result.add(installThread.emuName + ": " + installThread.result);
        }

        return result;
    }

    @Override
    public String cmdSyntax() {
        return "installapp <apkfile> [all | <avd1> <...>]";
    }

    @Override
    public String getExplanation() {
        return "Install/reinstall the apk passed in arguments to all emulators or only those specified (emulator(s) MUST BE ONLINE).";
    }

    // Helper methods

    private boolean doesApkExist(String apkName) {
        String filePath = AVDController.termiteServerPath + File.separator + "apks" + File.separator + apkName;
        System.out.println(filePath);
        File f = new File(filePath);
        return (f.exists() && !f.isDirectory());
    }

    private ArrayList<String> getOnlineEmulatorNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Emulator emulator : AVDController.getEmulatorsInstances()) {
            names.add(emulator.get_name());
        }
        return names;
    }


    // Class thread to handle application installs
    private static class InstallAppThread implements Runnable {

        public String emuName;
        public String result;
        private int mEmuPort;
        private String apk;
        private AVDControllerDriver AVDController;

        public InstallAppThread(String name, int emuPort, String apkName, AVDControllerDriver avd) {
            emuName = name;
            mEmuPort = emuPort;
            apk = apkName;
            AVDController = avd;
        }

        @Override
        public void run() {
            result = AVDController.installApk(mEmuPort, apk);
        }
    }
}
