package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;

import java.util.ArrayList;
import java.util.Arrays;

public class runRemoteScriptCommand extends Command {

    public runRemoteScriptCommand(){ super("runscript", Command.NULLABVR); }

    @Override
    public String executeCommand(Context context, String[] args) {
        assert context != null && args != null;

        if(args.length < 3){
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        String serverIp = args[1];
        if(!(context.mConfigManager.getControllerNetworks().contains(serverIp))){
            return returnError("Controller network with ip \"" + serverIp + "\" does not exist.");
        }

        String scriptName = args[2];
        if(scriptName.length() == 0){
            return returnError("Invalid script file name \"" + scriptName + "\" .");
        }

        ArrayList<String> scriptArgs = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
        String cmdResult = context.mRemoteAVDController.runScriptFile(serverIp, scriptName, scriptArgs);
        System.out.println(cmdResult);

        return OK;
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: runscript <tserver-ip> <script-name> <args...>");
    }

    @Override
    public String getExplanation() {
        return "Runs remote script file with arguments specified on the chosen termite2 server.";
    }
}
