package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;
import main.pt.inesc.termite2.cli.Emulator;
import main.pt.inesc.termite2.cli.EmusTracker;

import java.util.ArrayList;

public class CreateAvdsCommand extends Command {

    public CreateAvdsCommand(){ super("createavds", Command.NULLABVR); }

    @Override
    public String executeCommand(Context context, String[] args) {
        assert context != null && args != null;

        if(args.length != 5){
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        String serverIp = args[1];
        int num = Integer.parseInt(args[2]);
        String name = args[3];
        int api = Integer.parseInt(args[4]);

        if(!(context.mConfigManager.getControllerNetworks().contains(serverIp))){
            return returnError("Controller network with ip " + serverIp + " does not exist.");
        }
        if(num < 1 || num > 15){
            return returnError("Invalid number of avds to create:  " + num + ".");
        }
        if(name.length() == 1){
            return returnError("Invalid avd name:  " + name + ".");
        }
        if(api < 21 || api > 30){
            return returnError("Invalid api number:  " + api + ". Use (21-30)");
        }

        String cmdResult = context.mRemoteAVDController.createAvds(serverIp, num, name, api);
        if(cmdResult.contains("Error")){
            System.out.println(cmdResult);
        }else{
            String trimedStringR = cmdResult.replace("[","").replace("]","");
            String[] cmdResultSplit = trimedStringR.split(",");
            System.out.println("- " + serverIp + ":");
            for(String res : cmdResultSplit){
                System.out.println(res);
            }
        }

        return OK;
    }

    @Override
    public void printHelp()  {
        System.out.println("Syntax: createavds <tserver-ip> <nº> <name> <api>");
    }

    @Override
    public String getExplanation() {
        return "Create avd(s) on selected termite2 server with choosen name and api.";
    }
}
