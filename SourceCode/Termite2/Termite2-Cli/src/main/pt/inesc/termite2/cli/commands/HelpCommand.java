package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help","h");
    }

    public String executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (args.length > 1) {
                if(( args[1].equals("cmds") || args[1].equals("commands"))){
                    System.out.println("Commands and explanation:");
                    printFullHelpCommands(context);
                    return OK;
                }
                if((args[1].equals("shared"))){
                System.out.println("Shared commands and explanation:");
                    printSharedCommands(context);
                    return OK;
                }
        }

        System.out.println("Type 'help <commands,cmds> for more detailed explanation.'");
        System.out.println("Commands:");
        printHelpCommands(context);
        return OK;
    }

    private void printHelpCommands(Context context) {
        Command [] commands = context.getCommands();
        for (Command cmd : commands) {
            System.out.println(cmd.getName() + " (" + cmd.getAbvr() + ")");
        }
    }

    private void printFullHelpCommands(Context context){
        Command [] commands = context.getCommands();
        for (Command cmd : commands) {
            String indent = "                    "; // length 20
            String base = cmd.getName() + " (" + cmd.getAbvr() + ")";
            base += indent.substring(0, indent.length() - base.length());
            System.out.println(base + cmd.getExplanation());
        }
    }

    private void printSharedCommands(Context context){
        Command [] sharedCommands = context.getSharedCommands();
        for (Command cmd : sharedCommands) {
            String indent = "                    "; // length 20
            String base = cmd.getName() + " (" + cmd.getAbvr() + ")";
            base += indent.substring(0, indent.length() - base.length());
            System.out.println(base + cmd.getExplanation());
        }
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: help|h [cmds]");
    }

    @Override
    public String getExplanation() {
        return "";
    }
}
