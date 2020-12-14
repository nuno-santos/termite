package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;

import java.util.ArrayList;

public class InstalledEmulatorsCommand extends Command {

    public InstalledEmulatorsCommand() { super("installed", Command.NULLABVR); }

    @Override
    public String executeCommand(Context context, String[] args) {
        assert context != null && args != null;

        ArrayList<String> mInstalled = new ArrayList<>();
        ArrayList<String> requestTargets = new ArrayList<>();

        if (args.length < 1 || args.length > 2) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        ArrayList<String> networks = context.mConfigManager.getControllerNetworks();
        if(args.length == 2){
            String serverIp = args[1];
            if(!(networks.contains(serverIp))){
                return returnError("Network with ip " + serverIp + " does not exist.");
            }
            requestTargets.add(serverIp);
        }
        else{
            requestTargets.addAll(networks);
        }

        for(String ip : requestTargets){
            String cmdResult = context.mRemoteAVDController.getInstalled(ip);
            if(cmdResult.contains("Error")){
                System.out.println(cmdResult);
            }else{
                System.out.println("- (" + ip + "): " + cmdResult);
            }
            mInstalled.add("(" + ip + "): " + cmdResult);
        }

        context.setLatestInstalledResult(mInstalled);
        return OK;
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: installed (optional <tserver-ip>)");
    }

    @Override
    public String getExplanation() {
        return "Returns the installed avds at the chosen termite2 server(s).";
    }
}
