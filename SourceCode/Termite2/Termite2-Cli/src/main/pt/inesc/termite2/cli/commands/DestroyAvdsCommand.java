package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;

import java.util.ArrayList;
import java.util.Arrays;

public class DestroyAvdsCommand extends Command {

    public DestroyAvdsCommand(){ super("destroyavds", Command.NULLABVR);}

    @Override
    public String executeCommand(Context context, String[] args) {
        assert context != null && args != null;

        if(args.length < 3){
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        String serverIp = args[1];
        if(!(context.mConfigManager.getControllerNetworks().contains(serverIp))){
            return returnError("Controller network with ip " + serverIp + " does not exist.");
        }

        ArrayList<String> destroyTargets = new ArrayList<>();
        destroyTargets.addAll(Arrays.asList(args).subList(2, args.length));

        String cmdResult = context.mRemoteAVDController.destroyAvds(serverIp, destroyTargets);
        System.out.println(cmdResult);

        return OK;
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: destroyavds <tserver-ip> <all|avd_name ...>");
    }

    @Override
    public String getExplanation() {
        return "Destroys all or just the selected avd(s) on the chosen termite2 server.";
    }
}
