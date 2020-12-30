package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

public class StartEmulatorsCommand extends Command {

    public StartEmulatorsCommand() {
        super("startemus", Command.NULLABVR);
    }

    @Override
    public String executeCommand(Context context, String[] args) {

        String app = "";

        assert context != null && args != null;

        // removes first string from args wich corresponds to the command name ex [startemus, ip ... ]
        String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);

        ArrayList<ArrayList<String>> fullCommand = new ArrayList<>();
        fullCommand = separateCmd(cmdArgs);
        String verificationResult = verifyAllCommandArgs(fullCommand, context);

        if(!(verificationResult.equals(OK))){
            printHelp();
            return returnError(verificationResult);
        }

        // For each emulator to stop, create a thread that handles the stop command
        Hashtable<Thread, StartEmulatorsThread> StartEmulatorThreads = new Hashtable<>();
        for(ArrayList<String> tcmd: fullCommand){
            String apppackage = "";
            if(tcmd.size() == 3 ){
                apppackage = tcmd.get(2);
            }
            StartEmulatorsThread StartEmulatorT = new StartEmulatorsThread(tcmd.get(0), tcmd.get(1), apppackage, context.mRemoteAVDController);
            Thread t = new Thread(StartEmulatorT);
            System.out.println("Processing startemus cmd to termite2 server \"" +tcmd.get(0) + "\" ...");
            t.start();
            StartEmulatorThreads.put(t, StartEmulatorT);
        }

        // wait for the execution of all stop emulator threads
        for (Thread t : StartEmulatorThreads.keySet()) {
            try {
                t.join();
            } catch (InterruptedException ignored) { }
        }

        // Save startemus command result for each termite2 server
        ArrayList<String> startResults = new ArrayList<>();
        for(StartEmulatorsThread StartEmulatorT : StartEmulatorThreads.values()) {
            startResults.add(StartEmulatorT.result);
        }

        StringBuilder errorResult = new StringBuilder();
        boolean update = false;
        for(int i = 0; i < startResults.size(); i++){
            if(startResults.get(i).equals(OK)){
                update = true;
            }
            else{
                errorResult.append(startResults.get(i));
                if(i != (startResults.size()-1)){
                    errorResult.append("\n");
                }
            }
        }

        if(update){
            //Refresh emuTracker
            ArrayList<Emulator> emulators = context.mRemoteAVDController.getEmulators();
            EmusTracker et = context.mCurrentEmulation.getEmusTracker();
            et.updateEmuList(new ArrayList<>(emulators));
        }

        if(errorResult.length() != 0 ){
            return errorResult.toString();
        }

        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: startemus <tserver-ip> <all|nº> <app-package> | <tserver-ip> <all|nº> <app-package> | ...");
    }

    public String getExplanation(){
        return "Tries to starts all|(1-16) emulator(s) instance on a chosen termite2 server(s) running (optional) chosen application. Separate the command with \"|\" to add another startemus command for another termite2 server.";
    }

    private ArrayList<ArrayList<String>> separateCmd(String[] args){
        ArrayList<ArrayList<String>> fullCommand = new ArrayList<>();
        fullCommand.add(new ArrayList<>());
        int i = 0;
        for(String arg : args){
            if(arg.equals("|")){
                fullCommand.add(new ArrayList<>());
                i++;
            }else{
                fullCommand.get(i).add(arg);
            }
        }
        return fullCommand;
    }

    private String verifyAllCommandArgs(ArrayList<ArrayList<String>> cmdArray, Context context){

        ArrayList<String> ipsFound = new ArrayList<>();

        if(cmdArray == null ||cmdArray.size() < 1){
            return "Wrong number of input arguments.";
        }
        for(ArrayList<String> tcmd : cmdArray){
            if(tcmd.size() < 2 || tcmd.size() > 3 ){
                return "Wrong number of input arguments.";
            }

            String serverIp = tcmd.get(0);
            String numEmusToStart = tcmd.get(1);
            String appPackage = "";
            if(tcmd.size() == 3){
                appPackage = tcmd.get(2);
                if(appPackage == null || appPackage.length() == 0){
                    return "Invalid application package" + appPackage + ".";
                }
            }

            // Validate controller ip
            if(!(context.mConfigManager.getControllerNetworks().contains(serverIp))){
                return "Controller network with ip " + serverIp + " does not exist.";
            }

            // Validate num of emulators to start
            if(!numEmusToStart.equals("all")){
                try{
                    if(Integer.parseInt(numEmusToStart) < 1 || Integer.parseInt(numEmusToStart) > 16 ){
                        return "Invalid number of emulator(s) to start: " + numEmusToStart + ", (1-16)|all.";
                    }
                }catch (NumberFormatException e){
                    return "Invalid number of emulator(s) to start: " + numEmusToStart + ", (1-16)|all.";
                }
            }

            // Validate if controller ip is not duplicate
            for(String ip : ipsFound){
                if(ip.equals(serverIp)){
                    return "Repeated termite2 server ip: \"" + serverIp + "\".";
                }
            }
            ipsFound.add(serverIp);
        }

        return OK;
    }

    private static class StartEmulatorsThread implements Runnable {

        private RemoteAVDController avdController;
        public String tServerIp;
        private String num;
        private String appPackage;
        public String result;

        public StartEmulatorsThread(String ip, String n, String app, RemoteAVDController avd) {
            tServerIp = ip;
            num = n;
            appPackage = app;
            avdController = avd;

        }

        @Override
        public void run() {
            result = avdController.startEmulators(tServerIp, num, appPackage);
        }
    }

}
