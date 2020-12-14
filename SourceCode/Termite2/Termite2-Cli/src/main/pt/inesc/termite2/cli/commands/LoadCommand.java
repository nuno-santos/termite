package main.pt.inesc.termite2.cli.commands;

import jline.console.ConsoleReader;
import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;
import main.pt.inesc.termite2.cli.RemoteAVDController;

import java.io.*;

public class LoadCommand extends Command {

    public LoadCommand() {
        super("load", Command.NULLABVR);
    }

    @Override
    public String executeCommand(Context context, String[] args) {

        String scriptsPath = context.mConfigManager.getTermitePath() + "/scripts/";

        if (args.length != 2) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        String fileName = args[1];
        File scriptFile = new File(scriptsPath+fileName);
        if(!scriptFile.exists()){
            return returnError("Script file "+ fileName + " does not exist.");
        }

        System.out.println("You can end the load command by typing 'stop' at any time.");
        ReadFileThread ReadThread = new ReadFileThread((scriptsPath+fileName), context);
        Thread t = new Thread(ReadThread);
        t.start();

        try {
            ConsoleReader reader = new ConsoleReader();
            while (t.isAlive()) {
                String line = reader.readLine();
                String[] tokens = line.split("\\s+");
                if (tokens.length == 0) {
                    continue;
                }
                String cmd = tokens[0];
                if (cmd.equals("") || cmd.startsWith("#")) {
                    continue;
                }
                if(cmd.equals("stop")){
                    System.out.println("Stopping load command, please wait...");
                    ReadThread.end = true;
                    break;
                }
            }
            t.join(60000);
        } catch (IOException | InterruptedException e) {
            ReadThread.end = true;
            return returnError("");
        }

        System.out.println("Load command finished.");
        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: load <script-file-name>");
    }

    public String getExplanation(){
        return "Loads txt file with termite2 commands. Type stop to interrupt load command.";
    }

    private static class ReadFileThread implements Runnable {

        private Context context;
        private String fileName;
        public boolean end = false;

        public ReadFileThread(String file, Context c) {
            fileName = file;
            context = c;
        }

        @Override
        public void run() {
            try {

                BufferedReader reader = new BufferedReader(new FileReader(fileName));

                String line;
                while (!end && (line = reader.readLine()) != null) {

                    String[] tokens = line.split("\\s+");
                    if (tokens.length == 0) {
                        continue;
                    }
                    String cmd = tokens[0];
                    if (cmd.equals("")) {
                        continue;
                    }
                    if (cmd.startsWith("#")) {
                        continue;
                    }
                    boolean found = false;
                    String result;
                    for (Command command : context.getCommands()) {
                        if (command.getName().equals(cmd) || (!command.getAbvr().equals(Command.NULLABVR) && command.getAbvr().equals(cmd))) {
                            found = true;
                            System.out.println("Executing command: " + line);
                            result = command.executeCommand(context,tokens);
                            if (result.equals("OK")){
                                context.getHistory().setLast(line);
                            }
                            else{
                                System.out.println(cmd + " " + result);
                                break;
                            }
                        }
                    }
                    if (!found) System.out.println("Error: Command \"" + cmd + "\" does not exist.");
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
