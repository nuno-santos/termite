package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;
import pt.inesc.termite.server.Emulator;
import pt.inesc.termite.server.exceptions.ControllerDriverException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StartEmulatorsCommand extends Command {

    private AVDControllerDriver AVDController;


    public StartEmulatorsCommand() {
        super("startemus");
    }

    /*
     * This command start the num of emulators passed in msg with the specified app. to do this we get all installed emulatores and se if theres
     * emulators available and not already running to start
     * */
    @Override
    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        ArrayList<String> result = new ArrayList<>();
        this.AVDController = AVDController;
        ArrayList<String> installedEmus = AVDController.getInstalledEmulators();

        // 1st validate num of arguments and if there is any avd installed to be started
        if (msg.size() < 1 || msg.size() > 2) {
            result.add("Error: Wrong number of arguments");
            return result;
        }
        if (installedEmus.size() == 0) {
            result.add("Error: No emulator installed");
            return result;
        }

        int numRequested;
        String app_package = "";
        if (msg.size() == 2) app_package = msg.get(1);

        // check if num request is equal to all
        if (msg.get(0).equals("all")) {
            numRequested = installedEmus.size();
        } else {
            numRequested = Integer.parseInt(msg.get(0));
        }

        String check = basicCommandVerifications(numRequested);
        if (check != null) {
            result.add("Error: " + check);
            return result;
        }

        int numEmusOnline = howManyEmusCanStart();
        if (numRequested > numEmusOnline) { // requested to initiate more emulators than what we can
            int requested = numRequested;
            numRequested = requested - (requested - numEmusOnline);
            System.out.println(numEmusOnline + " emulators online, requested to start another " + requested + ".\n " +
                    "Max number of emulators online is 16.\n" +
                    "Starting " + numRequested);
        }

        ArrayList<String> emulatorsOnline = getOnlineEmulatorNames();

        // From emulators installed options we remove those that are already online
        installedEmus.removeAll(new HashSet<String>(emulatorsOnline));

        // verify if number requested exceeds num of emulators available to be started
        if (numRequested > installedEmus.size()) {
            int requested = numRequested;
            numRequested = requested - (requested - installedEmus.size());
            System.out.println("There are " + installedEmus.size() + " installed avds available to be started. Operation requested to start " + requested +
                    ". This exceeds avds available. Starting only " + numRequested);
        }

        ArrayList<String> emusToStart = new ArrayList<>();
        for (int i = 0; i < numRequested; i++) {
            emusToStart.add(installedEmus.get(i));
        }

        System.out.println("Emulators automatically selected to start: " + emusToStart.toString());


        // For each emulator to start, create a thread that handles the start command
        Hashtable<Thread, StartEmulatorThread> startEmusThreads = new Hashtable<>();
        for (String emuName : emusToStart) {
            StartEmulatorThread emuThread = new StartEmulatorThread(AVDController, emuName, emulatorsOnline.size(), emusToStart.size());
            Thread t = new Thread(emuThread);
            t.start();
            startEmusThreads.put(t, emuThread);
        }

        // wait for the termination of all start emulator threads
        System.out.println("Waiting for all emulators to come online...");
        for (Thread t : startEmusThreads.keySet()) {
            try {
                t.join();
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }

        // Check emulators that started without errors
        ArrayList<String> emusStartedCorrectly = new ArrayList<>();
        for (StartEmulatorThread emuStartThread : startEmusThreads.values()) {
            if (emuStartThread.success) {
                System.out.println("Emulator " + emuStartThread._emuName + " online.");
                emusStartedCorrectly.add(emuStartThread._emuName);
            }
        }

        // Then we refresh all emulator instances and redirection to account for the new emulator online
        if (emusStartedCorrectly.size() == 0) {
            System.out.println("No new emulator was initiated.");
        } else {

            AVDController.refreshAllRedirections();

            // wait for boot animations
            System.out.println("Wainting for all emulator(s) boot animations to finish...");
            waitForBootAnimation();
            System.out.println("All emulator(s) boot animations finished.");

            // Now we check if there is app to start, if so we start the app
            if (app_package.length() != 0) {
                ArrayList<Emulator> emusOnline = AVDController.getEmulatorsInstances();
                for (Emulator emuOnline : emusOnline) {
                    if (emusStartedCorrectly.contains(emuOnline.get_name())) {
                        //Start thread that handles app start
                        StartApplicationHandler appH = new StartApplicationHandler(emuOnline.get_port(), app_package, AVDController);
                        Thread t = new Thread(appH);
                        t.start();
                    }
                }
            }
        }

        // * Finally if all goes well we get the new emulators online string to return
        String emulatorsString = AVDController.getEmulatorsString();

        if (emulatorsString != null) {
            String[] lines = emulatorsString.split("\\r?\\n");
            result = new ArrayList<>(Arrays.asList(lines));
        }

        return result;
    }

    @Override
    public String cmdSyntax() {
        return "startemus <all|nº> <app_package>";
    }

    @Override
    public String getExplanation() {
        return "Starts <all|nº> emulators if available with optional specified app <app_package>.";
    }

    // Helper methods

    private String basicCommandVerifications(int num) {

        // check if arguments are valid
        if (num < 1 || num > 16) {
            return "Invalid arguments found, num: " + num + ".";
        }

        // After basic arguments checks:
        // refresh emulators instances and port redirection
        AVDController.refreshAllRedirections();

        if (AVDController.getEmulatorsInstances().size() == 16) {
            return "Maximum number of online emulators already reached (16).";
        }

        return null;
    }

    private int howManyEmusCanStart() {
        int numOfOnlineEmus = AVDController.getEmulatorsInstances().size();
        return (16 - numOfOnlineEmus);
    }

    private ArrayList<String> getOnlineEmulatorNames() {
        ArrayList<String> names = new ArrayList<>();

        for (Emulator emulator : AVDController.getEmulatorsInstances()) {
            names.add(emulator.get_name());
        }
        return names;
    }

    private void waitForBootAnimation() {
        String script = AVDController.termiteServerPath + File.separator + "config" + File.separator + "serverscripts" + File.separator;
        String fullScriptPath;
        ArrayList<Emulator> emusOnline = AVDController.getEmulatorsInstances();
        ArrayList<String> onlinePorts = new ArrayList<>();

        if (emusOnline.size() == 0) {
            return;
        }

        for (Emulator emu : emusOnline) {
            onlinePorts.add("" + emu.get_port());
        }


        for (String emuPort : onlinePorts) {
            AVDController.restartAdbServer();
            System.out.println("Waiting for boot animation to stop on emulator-" + emuPort);

            // Build process list
            List<String> exeList = new ArrayList<>();
            if (AVDController.platform.equals("windows")) {
                fullScriptPath = script + "checkbootanim.bat";
            } else {
                fullScriptPath = script + "checkbootanim.sh";
            }
            exeList.add(fullScriptPath);
            exeList.add(emuPort);


            try {

                long startTime = System.currentTimeMillis();
                long tpassed = 0;
                long checks = 30000; // this time is used to check if the emuator is still online if this time has passed
                while (tpassed < 120000) {

                    if(tpassed >= checks){
                        System.out.println( (tpassed/1000) + "s passed, checking if emulator-" + emuPort + " still online...");
                        AVDController.restartAdbServer();
                        if(!emuStillOnline(emuPort)){
                            System.out.println("Emulator-" + emuPort + " no longer online.");
                            break;
                        }
                        System.out.println("Emulator-" + emuPort + " still online. Waiting for boot animation...");
                        checks += 30000;
                    }

                    ProcessBuilder pb = new ProcessBuilder(exeList);
                    Process p = pb.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

                    tpassed = System.currentTimeMillis() - startTime;
                    String line = reader.readLine();
                    //System.out.println(line);
                    if (line == null) {
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    else if(line.equals("running")){
                        try {
                            TimeUnit.SECONDS.sleep(2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

        }

    }

    private boolean emuStillOnline(String emuPort){
        for(String emuStates : AVDController.getOnlineEmulators()){
            if(emuStates.contains(emuPort)){
                return true;
            }
        }
        return false;
    }


    // Class thread to handle application starts
    private static class StartApplicationHandler implements Runnable {

        private int mEmuPort;
        private String mAppPackage;
        private AVDControllerDriver AVDController;

        public StartApplicationHandler(int emuPort, String appPackage, AVDControllerDriver avd) {
            mEmuPort = emuPort;
            mAppPackage = appPackage;
            AVDController = avd;
        }

        @Override
        public void run() {
            AVDController.startApp(mEmuPort, mAppPackage);
        }
    }


    private static class StartEmulatorThread implements Runnable {

        public String _emuName;
        public int _numOnline;
        public int _numStarting;
        public boolean success = false;
        private AVDControllerDriver _AVDController;

        public StartEmulatorThread(AVDControllerDriver avd, String emuName, int numOnline, int numStarting) {
            _AVDController = avd;
            _emuName = emuName;
            _numOnline = numOnline;
            _numStarting = numStarting;
        }

        @Override
        public void run() {
            try {
                _AVDController.startEmulator(_emuName);
                waitForAll();
                success = true;
            } catch (ControllerDriverException e) {
                System.out.println("Error: Could not start emulators with name " + _emuName);
                success = false;
            }
        }

        // WAIT FOR ALL EMUS TO START
        private void waitForAll() {
            long initTime = System.currentTimeMillis();
            boolean timeElapsed = true;
            while (timeElapsed) {
                // cheack if 2min seconds have passed from the beggining
                if (System.currentTimeMillis() - initTime >= 240000) {
                    System.out.println("Warning: Reached maximum waiting time (4min) for all emulators to come online");
                    return;
                } else {
                    int currentOnline = _AVDController.getOnlineEmulators().size();
                    if (currentOnline < _numOnline) { // This is done in case the user closes some emu when we are waiting for others to start
                        _numOnline = currentOnline;
                        System.out.println("Warning: Emulators were closed while waiting for others to start.");
                    } else if (currentOnline >= (_numOnline + _numStarting)) {
                        timeElapsed = false;
                    }

                }
            }

        }

    }

}
