package main.pt.inesc.termite2.cli;

public abstract class Command {

    protected String mName;
    protected String mAbvr;
    protected static final String OK = "OK";

    public static final String NULLABVR = "-";

    public Command(String name, String abvr) {
        assert name != null : "Invalid command name";
        mName = name;
        mAbvr = abvr;
    }

    public String getName() {
        return mName;
    }

    public String getAbvr() {
        return mAbvr;
    }

    public abstract String executeCommand(Context context, String [] args);

    public abstract void printHelp();

    public abstract String getExplanation();

    public void printError(String msg) {
        System.out.println("Error: " + msg);
    }

    public String returnError(String msg) {
        return ("Error: " + msg);
    }
}
