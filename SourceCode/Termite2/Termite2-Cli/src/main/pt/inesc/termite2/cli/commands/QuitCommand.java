package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;

public class QuitCommand extends Command {

    public QuitCommand() {
        super("quit","q");
    }

    public String executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        context.mRemoteAVDController.closeConnections();
        System.exit(0);
        return OK;
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: quit|q");
    }

    public String getExplanation(){
        return "Terminates Termite2-Cli.";
    }
}
