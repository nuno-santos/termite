package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RunScriptCommand extends Command {

    public RunScriptCommand() {
        super("runscript");
    }

    @Override
    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        ArrayList<String> result = new ArrayList<>();
        String scriptsFolderPath = AVDController.termiteServerPath + File.separator + "scripts" + File.separator;


        // verify num of arguments
        if (msg.size() < 1) {
            result.add("Error: Wrong number of arguments");
            return result;
        }

        String scriptFileName = msg.get(0);

        // check if file exists
        String fullScriptPath = (scriptsFolderPath + scriptFileName);
        if (!(doesFileExist(fullScriptPath))) {
            result.add("Error: Script file \"" + scriptFileName + "\" not found.");
            return result;
        }

        List<String> exeList = new ArrayList<>();
        if (AVDController.platform.equals("windows")) {
            exeList.add("cmd");
            exeList.add("/c");
            exeList.add("start");
        }
        exeList.add(fullScriptPath);

        // Create arguments string if they exist
        String args = "";
        if (msg.size() > 1) {
            msg.remove(0); //remove file path
            exeList.addAll(msg);
        }

        //Script found, lets run it
        try {
            ProcessBuilder pb = new ProcessBuilder(exeList);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            result.add("Success: Script file \"" + scriptFileName + "\" executed.");
            return result;

        } catch (IOException e) {
            e.printStackTrace();
            result.add("Error: Script file \"" + scriptFileName + "\" not executed.");
            return result;
        }
    }

    @Override
    public String cmdSyntax() {
        return "runscript <file_name> <args...>";
    }

    @Override
    public String getExplanation() {
        return "Runs script file with args specified.";
    }

    // HELPER METHODS

    private boolean doesFileExist(String filePath) {
        File f = new File(filePath);
        return (f.exists() && !f.isDirectory());
    }
}
