package pt.inesc.termite.cli.commands;

import jline.console.ConsoleReader;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Network;


public class ClsCommand extends Command {

    public ClsCommand(String name, String abrv) {
        super(name,abrv);
    }

    public ClsCommand() {
        super("cls", Command.NULLABVR);
    }

    public boolean executeCommand(Context context, String [] args) {

        assert context != null;

        ConsoleReader reader = context.getReader();
        try {
            reader.clearScreen();
        } catch (Exception e) {
            printError("Error writing to the console.");
        }
        return true;
    }
}
