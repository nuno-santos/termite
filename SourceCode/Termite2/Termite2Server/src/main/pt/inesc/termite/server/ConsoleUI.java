package pt.inesc.termite.server;

import jline.console.ConsoleReader;
import pt.inesc.termite.server.commands.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class ConsoleUI {

    private AVDControllerDriver AVDController;
    private ConsoleReader mReader;

    private Command[] Commands = {
            new RefreshEmulatorsCommand(),
            new StopEmulatorCommand(),
            new InstalledEmulatorsCommand(),
            new CreateAvdsCommand(),
            new DestroyAvdsCommand(),
            new StartEmulatorsCommand(),
            new InstallApplicationCommand(),
            new StartApplicationCommand(),
            new RunScriptCommand(),
            new RestartAdbCommand()
    };

    public ConsoleUI(AVDControllerDriver controller, ConsoleReader r) {
        AVDController = controller;
        mReader = r;
    }

    private static void printResult(ArrayList<String> res) {
        if (res != null) {
            System.out.println("Result = " + res.toString());
        }
    }

    public void start() throws IOException {
        String line;
        while ((line = mReader.readLine()) != null) {

            String[] tokens = line.split("\\s+");
            if (tokens.length == 0) {
                continue;
            }

            String cmd = tokens[0];
            if (cmd.equals("")) {
                continue;
            }
            if (cmd.equals("help") || cmd.equals("h")) {
                printHelp();
                continue;
            }
            if (cmd.equals("print")) {
                AVDController.printStructures();
                continue;
            }
            if (cmd.equals("quit")) {
                System.exit(0);
            }

            boolean found = false;
            boolean ok;
            for (Command command : Commands) {
                if (command.getName().equals(cmd)) {
                    found = true;
                    ArrayList<String> arguments = new ArrayList<>(Arrays.asList(tokens));
                    arguments.remove(0); // removes the command name
                    ArrayList<String> result = command.execute(arguments, AVDController);
                    printResult(result);
                    break;
                }
            }
            if (!found)
                System.out.println("Error: Command '\"" + cmd + "\" does not exist.");
        }
    }

    private void printHelp() {
        System.out.println("Type \"print\" to print current data structures");
        System.out.println("Type \"quit\" to stop TermiteServer");
        System.out.println("");
        System.out.println("Commands and explanation:");
        for (Command cmd : Commands) {
            String indent = "                                             "; // length 45
            String base = cmd.cmdSyntax();
            base += indent.substring(0, indent.length() - base.length());
            System.out.println(base + cmd.getExplanation());
        }
    }

}
