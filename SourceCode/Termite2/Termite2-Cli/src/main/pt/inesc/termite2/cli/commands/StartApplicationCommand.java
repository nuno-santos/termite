package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;
import main.pt.inesc.termite2.cli.RemoteAVDController;

import java.util.*;

public class StartApplicationCommand extends Command {

    HashMap<String,ArrayList<String>> startAppTargets = new HashMap<>();

    public StartApplicationCommand(){ super("startapp", Command.NULLABVR); }

    @Override
    public String executeCommand(Context context, String[] args) {
        assert context != null && args != null;
        startAppTargets = new HashMap<>();

        // removes first string from args wich corresponds to the command name ex [installapp, ip, ... ]
        String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);

        ArrayList<ArrayList<String>> fullCommand = new ArrayList<>();
        fullCommand = separateCmd(cmdArgs);
        String verificationResult = verifyAllCommandArgs(fullCommand, context);

        if(!(verificationResult.equals(OK))){
            printHelp();
            return returnError(verificationResult);
        }

        // For each termite2 server were we are sending installapp cmd, create a thread that handles the install cmd
        Hashtable<Thread, StartAppThread> startAppThreads = new Hashtable<>();
        for(ArrayList<String> tcmd: fullCommand){
            System.out.println(startAppTargets.get(tcmd.get(0)).toString());
            StartAppThread startT = new StartAppThread(tcmd.get(0), tcmd.get(1), startAppTargets.get(tcmd.get(0)), context.mRemoteAVDController);
            Thread t = new Thread(startT);
            t.start();
            startAppThreads.put(t, startT);
        }

        // wait for the execution of all stop emulator threads
        for (Thread t : startAppThreads.keySet()) {
            try {
                t.join();
            } catch (InterruptedException ignored) { }
        }

        // Save installapp command results for each termite2 server
        for(StartAppThread startAppT : startAppThreads.values()) {
            System.out.println("Start app result for termite2 server \"" + startAppT.tServerIp + "\":");
            System.out.println(startAppT.result);
        }

        return OK;

        /*if(args.length < 4){
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        ArrayList<String> serverIps = new ArrayList<>();
        String serverIp = args[1];

        if(serverIp.equals("all")){
            serverIps.addAll(context.mConfigManager.getControllerNetworks());
        }
        else {
            if(!(context.mConfigManager.getControllerNetworks().contains(serverIp))){
                return returnError("Controller network with ip \"" + serverIp + "\" does not exist.");
            }
            serverIps.add(serverIp);
        }

        String appPackage = args[2];
        if(appPackage.length() == 0){
            return returnError("Invalid app package name \"" + appPackage + "\" .");
        }

        ArrayList<String> startAppTargets = new ArrayList<>(Arrays.asList(args).subList(3, args.length));
        for(String ip: serverIps){
            String cmdResult = context.mRemoteAVDController.startApplication(ip, appPackage, startAppTargets);
            System.out.println(cmdResult);
        }

        return OK;*/
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: startapp <tserver-ip> <app-package> <all|avd_name ...> | <tserver-ip> <app-package> <all|avd_name ...> | ...");
    }

    @Override
    public String getExplanation() {
        return "Starts app package provided on all or just the selected ONLINE emulator(s) on the chosen termite2 server. Separate the command with \"|\" to add another startapp command for another termite2 server.";
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

            if(tcmd.size() < 3 ){
                return "Wrong number of input arguments.";
            }

            String serverIp = tcmd.get(0);
            if(!(context.mConfigManager.getControllerNetworks().contains(serverIp))){
                return returnError("Controller network with ip \"" + serverIp + "\" does not exist.");
            }
            // Validate if controller ip is not duplicate
            for(String ip : ipsFound){
                if(ip.equals(serverIp)){
                    return "Repeated termite2 server ip: \"" + serverIp + "\".";
                }
            }

            String appPackage = tcmd.get(1);
            if(appPackage.length() == 0){
                return returnError("Invalid app package name \"" + appPackage + "\" .");
            }

            for(int i = 2; i < tcmd.size(); i++){
                if(tcmd.get(i).length() < 1 ){
                    return "Invalid installation target \"" + tcmd.get(i) + "\"";
                }
                startAppTargets.put(serverIp, new ArrayList<String>(Collections.singleton(tcmd.get(i))));

            }

            ipsFound.add(serverIp);
        }
        return OK;
    }

    private static class StartAppThread implements Runnable {

        private RemoteAVDController avdController;
        public String tServerIp;
        private String appPackage;
        private ArrayList<String> startappTargets;
        public String result;

        public StartAppThread(String ip, String app, ArrayList<String> targets, RemoteAVDController avd) {
            tServerIp = ip;
            appPackage = app;
            startappTargets = targets;
            avdController = avd;

        }

        @Override
        public void run() {
            result = avdController.startApplication(tServerIp, appPackage, startappTargets);
        }
    }
}
