package pt.inesc.termite.server;

import java.util.ArrayList;

public abstract class Command {

    private String nName;

    public Command(String name) {
        this.nName = name;
    }

    public String getName() {
        return nName;
    }

    public abstract ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController);

    // Help commands

    public abstract String cmdSyntax();

    public abstract String getExplanation();
}
