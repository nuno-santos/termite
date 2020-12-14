package main.pt.inesc.termite2.cli;

import jline.console.ConsoleReader;
import jline.console.history.History;

import java.util.ArrayList;
import java.util.Map;

public final class Context {

    //this is used to get last commit result and send it to the ui after a commit and the latest installed cmd result
    private ArrayList<String> mLastCommitResult;
    private String mInstalled;

    private Command[] mCtxCommands;
    private Command[] mSharedCommands;

    private History mCtxHistory;
    private ConsoleReader mConsoleReader;

    // public fields available for direct access
    public ConfigManager mConfigManager;
    public Emulation mCurrentEmulation;
    public RemoteAVDController mRemoteAVDController;

    public Context(Command[] commands, Command[] shared, ConsoleReader reader) {
        assert commands != null && shared != null && reader != null;
        mCtxHistory = new History();
        mCtxCommands = commands;
        mSharedCommands = shared;
        mConsoleReader = reader;
        mCurrentEmulation = new Emulation();
        mLastCommitResult = new ArrayList<>();
    }

    public Command[] getCommands() {
        return mCtxCommands;
    }

    public Command[] getSharedCommands() {
        return mSharedCommands;
    }

    public History getHistory() {
        return mCtxHistory;
    }

    public ConsoleReader getReader() {
        return mConsoleReader;
    }

    public ArrayList<String> getLastCommitResult() {
        return mLastCommitResult;
    }

    public String getLatestInstalledResult(){
        return mInstalled;
    }

    public static class History {

        private String mLast;

        public History() {
            mLast = null;
        }

        public void setLast(String cmdLine) {
            mLast = cmdLine;
        }

        public String getLast() {
            return mLast;
        }
    }

    public void setLastCommitResult(ArrayList<String> cResult){
        mLastCommitResult = cResult;
    }

    public void setLatestInstalledResult(ArrayList<String> latestInstalled ){
        mInstalled = "";
        for(int i = 0; i < latestInstalled.size(); i++){
            mInstalled = mInstalled + latestInstalled.get(i);
            if(i != (latestInstalled.size() - 1)){
                mInstalled = mInstalled + "\n";
            }
        }
    }
}
