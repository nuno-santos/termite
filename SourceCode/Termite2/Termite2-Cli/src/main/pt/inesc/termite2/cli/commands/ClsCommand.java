package main.pt.inesc.termite2.cli.commands;

import jline.console.ConsoleReader;
import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;

public class ClsCommand extends Command {

    public ClsCommand() {
        super("cls", Command.NULLABVR);
    }

    public String executeCommand(Context context, String [] args) {

        assert context != null;

        ConsoleReader reader = context.getReader();
        try {
            reader.clearScreen();
        } catch (Exception e) {
            printError("Error writing to the console.");
        }
        return OK;
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: cls");
    }

    public String getExplanation(){
        return "Clears console screen.";
    }
}
