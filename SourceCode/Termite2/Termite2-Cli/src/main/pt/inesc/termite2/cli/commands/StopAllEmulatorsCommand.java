package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;
import main.pt.inesc.termite2.cli.exceptions.RemoteAVDControllerException;

import java.util.ArrayList;
import java.util.Hashtable;

public class StopAllEmulatorsCommand extends Command {

    public StopAllEmulatorsCommand() {
        super("stopall", Command.NULLABVR);
    }

    @Override
    public String executeCommand(Context context, String[] args) {

        assert context != null && args != null;

        ArrayList<String> toStop = new ArrayList<>();

        if (args.length < 1) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        if(context.mRemoteAVDController.getEmulators().size() == 0){
            return returnError("There is no Emulators active.");
        }

        ArrayList<String> networks = context.mConfigManager.getControllerNetworks();

        if(args.length >= 2){ // validate ip
            for(int i = 1; i < args.length; i++){
                String serverIp = args[i];
                if(!(networks.contains(serverIp))){
                    return returnError("Network with ip " + serverIp + " does not exist.");
                }
                if(!emulatorOnlineOn(serverIp, context)){
                    return returnError("There is no active Emulator on network " + serverIp);
                }
                toStop.add(serverIp);
            }
        }
        else{
            toStop.addAll(networks);
        }

        // For each termite2 server were we are sending installapp cmd, create a thread that handles the install cmd
        Hashtable<Thread, StopAllThread> stopAllThreads = new Hashtable<>();
        for(String ip : toStop){
            StopAllThread stopT = new StopAllThread(ip, context.mRemoteAVDController);
            Thread t = new Thread(stopT);
            System.out.println("Processing stopall cmd to termite2 server \"" + ip + "\" ...");
            t.start();
            stopAllThreads.put(t, stopT);
        }

        // wait for the execution of all stop emulator threads
        for (Thread t : stopAllThreads.keySet()) {
            try {
                t.join();
            } catch (InterruptedException ignored) { }
        }

        // Save installapp command results for each termite2 server
        for(StopAllThread stopAllT : stopAllThreads.values()) {
            if(stopAllT.result.equals(OK)){
                context.mRemoteAVDController.removeEmulatorForNet(stopAllT.tServerIp);
                System.out.println("All emulators on network " + stopAllT.tServerIp + " stopped.");
            }
            else{
                System.out.println(stopAllT.result);
            }
        }

        //Refresh emuTracker
        ArrayList<Emulator> emulators = context.mRemoteAVDController.getEmulators();
        EmusTracker et = context.mCurrentEmulation.getEmusTracker();
        et.updateEmuList(new ArrayList<>(emulators));
        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: stopall ( Optional <tserver-ip1> <tserver-ip2> ... )");
    }

    // helper method
    private boolean emulatorOnlineOn(String ip, Context context){
        for(Emulator emu : context.mRemoteAVDController.getEmulators()){
            if(emu.getIp().equals(ip)){
                return true;
            }
        }
        return false;
    }

    public String getExplanation(){
        return "Stops all emulator instances running on all termite2 servers or only the ones specified.";
    }


    private static class StopAllThread implements Runnable {

        private RemoteAVDController avdController;
        public String tServerIp;
        private String apk;
        private ArrayList<String> installationTargets;
        public String result;

        public StopAllThread(String ip, RemoteAVDController avd) {
            tServerIp = ip;
            avdController = avd;

        }

        @Override
        public void run() {
            result = avdController.stopAllEmulators(tServerIp);
        }
    }
}
