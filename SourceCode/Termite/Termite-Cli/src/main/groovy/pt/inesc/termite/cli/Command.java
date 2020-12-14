package pt.inesc.termite.cli;

public abstract class Command {

	protected String mName;
	protected String mAbvr;
	protected boolean mIsInternal;
	protected String [] mProperties;

    public static final String NULLABVR = "-";

	public Command(String name, String abvr) {
		assert name != null : "Invalid command name";
		mName = name;
		mAbvr = abvr;
		mProperties = null;
	}

	public String getName() {
		return mName;
	}

	public String getAbvr() {
		return mAbvr;
	}
	
	public String [] getProperties() {
		return mProperties;
	}

	public abstract boolean executeCommand(Context context, String [] args);

	public void printHelp() {
		System.out.println("Syntax: "+ mName + "|" + mAbvr);
	}

	public static void printWrongCommand(String cmd) {
		System.out.println("Error: Command \"" + cmd + "\" does not exist.");
	}

	public void printError(String msg) {
		System.out.println("Error: " + msg);
	}

	public void printMsg(String msg) {
		//System.out.println(msg);
	}
}
