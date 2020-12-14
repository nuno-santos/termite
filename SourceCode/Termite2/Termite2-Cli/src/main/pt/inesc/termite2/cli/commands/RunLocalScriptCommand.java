package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RunLocalScriptCommand extends Command {

    public RunLocalScriptCommand(){ super("localscript", Command.NULLABVR); }

    @Override
    public String executeCommand(Context context, String[] args) {
        ArrayList<String> msg = new ArrayList<>();
        String scriptsFolderPath = context.mConfigManager.getTermitePath() + File.separator + "scripts" + File.separator;


        // verify num of arguments
        if(args.length < 2){
            return "Error: Wrong number of arguments";
        }

        for(int i = 1; i < args.length; i++){
            msg.add(args[i]);
        }

        String scriptFileName = msg.get(0);

        // check if file exists
        String fullScriptPath = (scriptsFolderPath + scriptFileName);
        if(!(doesFileExist(fullScriptPath))){
            return  "Error: Script file \"" + scriptFileName + "\" not found inside scripts folder.";
        }

        List<String> exeList = new ArrayList<>();
        if(context.mConfigManager.getTermitePlatform().equals("windows")){
            exeList.add("cmd");
            exeList.add("/c");
            exeList.add("start");
        }
        exeList.add(fullScriptPath);

        // Create arguments string if they exist
        if(msg.size() > 1){
            msg.remove(0); //remove file path
            exeList.addAll(msg);
        }

        //Script found, lets run it
        try {
            ProcessBuilder pb = new ProcessBuilder(exeList);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while((line = reader.readLine()) != null){
                System.out.println(line);
            }

            System.out.println("Success: Script file \"" + scriptFileName + "\" executed.");
            return  OK;

        }   catch (IOException e) {
            //e.printStackTrace();
            return "Error: Script file \"" + scriptFileName + "\" not executed.";
        }
    }


    public void printHelp() {
        System.out.println("Syntax: localscript <file_name> <args...>");
    }

    public String getExplanation(){
        return "Runs local script file with args specified.";
    }
    // HELPER METHODS

    private boolean doesFileExist(String filePath){
        File f = new File(filePath);
        return (f.exists() && !f.isDirectory());
    }
}
