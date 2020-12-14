package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

import java.util.*;

public class InstallApplicationCommand extends Command {

    HashMap<String,ArrayList<String>> installationTargets;

    public InstallApplicationCommand(){ super("installapp", Command.NULLABVR); }

    @Override
    public String executeCommand(Context context, String[] args) {
        assert context != null && args != null;
        installationTargets = new HashMap<>();

        // removes first string from args wich corresponds to the command name ex [installapp, ip, ... ]
        String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);

        ArrayList<ArrayList<String>> fullCommand = new ArrayList<>();
        fullCommand = separateCmd(cmdArgs);
        String verificationResult = verifyAllCommandArgs(fullCommand, context);

        if(!(verificationResult.equals(OK))){
            return returnError(verificationResult);
        }

        // For each termite2 server were we are sending installapp cmd, create a thread that handles the install cmd
        Hashtable<Thread, InstallAppThread> InstallThreads = new Hashtable<>();
        for(ArrayList<String> tcmd: fullCommand){
            System.out.println(installationTargets.get(tcmd.get(0)).toString());
            InstallAppThread installT = new InstallAppThread(tcmd.get(0), tcmd.get(1), installationTargets.get(tcmd.get(0)), context.mRemoteAVDController);
            Thread t = new Thread(installT);
            System.out.println("Processing installapp cmd to termite2 server \"" +tcmd.get(0) + "\" ...");
            t.start();
            InstallThreads.put(t, installT);
        }

        // wait for the execution of all install emulator threads
        for (Thread t : InstallThreads.keySet()) {
            try {
                t.join();
            } catch (InterruptedException ignored) { }
        }

        // Save installapp command results for each termite2 server
        for(InstallAppThread installAppT : InstallThreads.values()) {
            System.out.println("Installation result for termite2 server \"" + installAppT.tServerIp + "\":");
            System.out.println(installAppT.result);
        }

        return OK;
        /*
        if(args.length < 4){
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        ArrayList<String> serverIps = new ArrayList<>();

        String serverIp = args[1];
        if(serverIp.equals("all")){
            serverIps.addAll(context.mConfigManager.getControllerNetworks());
        }
        else{
            if(!(context.mConfigManager.getControllerNetworks().contains(serverIp))){
                return returnError("Controller network with ip \"" + serverIp + "\" does not exist.");
            }
            serverIps.add(serverIp);
        }

        String apkFileName = args[2];
        if(apkFileName.length() == 0 || !apkFileName.contains(".apk")){
            return returnError("Invalid apk file name \"" + apkFileName + "\" .");
        }

        ArrayList<String> installationTargets = new ArrayList<>(Arrays.asList(args).subList(3, args.length));
        for(String ip: serverIps){
            String cmdResult = context.mRemoteAVDController.installApplication(ip, apkFileName, installationTargets);
            System.out.println(cmdResult);
        }

        return OK;*/
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: installapp <tserver-ip> <apk-name> <all|avd_name ...> | <tserver-ip> <apk-name> <all|avd_name ...> | ...");
    }

    @Override
    public String getExplanation() {
        return "Install/reinstall an apk corresponding to the name provided on all or selected ONLINE emulator(s) on chosen termite2 server. Separate the command with \"|\" to add another installapp command to another termite2 server. ";
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

            String apkFileName = tcmd.get(1);
            if(apkFileName.length() == 0 || !apkFileName.contains(".apk")){
                return returnError("Invalid apk file name \"" + apkFileName + "\" .");
            }

            for(int i = 2; i < tcmd.size(); i++){
                if(tcmd.get(i).length() <1 ){
                    return "Invalid installation target \"" + tcmd.get(i) + "\"";
                }
                installationTargets.put(serverIp, new ArrayList<String>(Collections.singleton(tcmd.get(i))));
            }

            ipsFound.add(serverIp);
        }
        return OK;
    }

    private static class InstallAppThread implements Runnable {

        private RemoteAVDController avdController;
        public String tServerIp;
        private String apk;
        private ArrayList<String> installationTargets;
        public String result;

        public InstallAppThread(String ip, String apkName, ArrayList<String> targets, RemoteAVDController avd) {
            tServerIp = ip;
            apk = apkName;
            installationTargets = targets;
            avdController = avd;

        }

        @Override
        public void run() {
            result = avdController.installApplication(tServerIp, apk, installationTargets);
        }
    }
}
