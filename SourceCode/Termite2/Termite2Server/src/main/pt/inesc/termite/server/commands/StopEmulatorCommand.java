package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;
import pt.inesc.termite.server.Emulator;
import pt.inesc.termite.server.exceptions.ControllerDriverException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

public class StopEmulatorCommand extends Command {

    private AVDControllerDriver _AVDControler;

    public StopEmulatorCommand() {
        super("stopemu");
    }

    /* This command stops the emulator corresponding to the port passed in args msg["port"] */
    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        ArrayList<Integer> portsToStop = new ArrayList<>();
        ArrayList<String> result = new ArrayList<>();
        _AVDControler = AVDController;

        if (msg.size() < 1) {
            result.add("Error: Wrong number of arguments");
            return result;
        }

        //We refreshed emulators online and redirections
        AVDController.refreshAllRedirections();

        ArrayList<Emulator> emusOnline = AVDController.getEmulatorsInstances();
        ArrayList<Integer> onlinePorts = new ArrayList<>();
        for (Emulator emu : emusOnline) {
            onlinePorts.add(emu.get_port());
        }

        if (msg.size() == 1 && msg.get(0).equals("all")) {
            portsToStop.addAll(onlinePorts);
        } else {
            for (String port : msg) {
                if (onlinePorts.contains((Integer.parseInt(port)))) {
                    portsToStop.add(Integer.parseInt(port));
                } else {
                    System.out.println("Emulator on port " + port + " not online.");
                }
            }
        }

        // To reduce adb crashes in case it was making work rigth before this command
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException ignored) {
        }


        // For each emulator to stop, create a thread that handles the stop command
        Hashtable<Thread, StopEmulatorThread> stopEmulatorThreads = new Hashtable<>();
        for (Integer port : portsToStop) {
            StopEmulatorThread stopThread = new StopEmulatorThread(port, AVDController);
            Thread t = new Thread(stopThread);
            t.start();
            stopEmulatorThreads.put(t, stopThread);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }

        // wait for the execution of all stop emulator threads
        for (Thread t : stopEmulatorThreads.keySet()) {
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        }

        // Check emulators that started without errors
        ArrayList<Integer> executedStopPorts = new ArrayList<>();
        for (StopEmulatorThread stopThread : stopEmulatorThreads.values()) {
            if (stopThread.success) {
                System.out.println("Stoping emulator " + stopThread.emuPort + ".");
                executedStopPorts.add(stopThread.emuPort);
            }
        }

        if (executedStopPorts.size() != 0) {
            System.out.println("Waiting for emulator(s) to stop...");
            waitForStops(emusOnline.size(), portsToStop.size());
            System.out.println("Emulator(s) stoped");
        }

        //We refreshed emulators online and redirections
        AVDController.refreshAllRedirections();

        // * Finally if all goes well we get the new emulators online string to return
        String emulatorsString = AVDController.getEmulatorsString();

        if (emulatorsString != null) {
            String[] lines = emulatorsString.split("\\r?\\n");
            result = new ArrayList<>(Arrays.asList(lines));
        }

        return result;

    }

    private void waitForStops(int started, int finish) {
        long initTime = System.currentTimeMillis();
        boolean timeElapsed = true;
        while (timeElapsed) {
            // cheack if 1min seconds have passed from the beggining
            if (System.currentTimeMillis() - initTime >= 180000) {
                System.out.println("Warning: Reached maximum waiting time (3min) to stop emulator(s)");
                timeElapsed = false;
            } else {
                if (_AVDControler.getOnlineEmulators().size() <= (started - finish)) {
                    timeElapsed = false;
                } else {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    @Override
    public String cmdSyntax() {
        return "stopemu <all>|<emu-port1 emu-port2 ...>";
    }

    @Override
    public String getExplanation() {
        return "Stops emulator(s) with specified <emu-port> or all.";
    }

    private static class StopEmulatorThread implements Runnable {

        public int emuPort;
        public boolean success = false;
        private AVDControllerDriver _AVDController;

        public StopEmulatorThread(int port, AVDControllerDriver avd) {
            emuPort = port;
            _AVDController = avd;
        }

        @Override
        public void run() {
            try {
                _AVDController.stopEmulator(emuPort);
                success = true;
            } catch (ControllerDriverException e) {
                System.out.println("Error: Could not stop emulators on port " + emuPort);
                success = false;
            }
        }
    }
}
