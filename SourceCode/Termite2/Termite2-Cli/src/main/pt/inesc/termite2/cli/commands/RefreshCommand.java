package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;
import main.pt.inesc.termite2.cli.Emulator;
import main.pt.inesc.termite2.cli.EmusTracker;
import main.pt.inesc.termite2.cli.exceptions.RemoteAVDControllerException;

import java.util.ArrayList;

public class RefreshCommand extends Command {

    public RefreshCommand() {
        super("refresh",Command.NULLABVR);
    }

    public String executeCommand(Context context, String[] args) {

        if(context.mRemoteAVDController.refreshEmulators()){
            //Refresh emuTracker
            ArrayList<Emulator> emulators = context.mRemoteAVDController.getEmulators();
            EmusTracker et = context.mCurrentEmulation.getEmusTracker();
            et.updateEmuList(new ArrayList<>(emulators));
            return OK;
        }

        return returnError("Emulators could not be refreshed.");
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: refresh");
    }

    public String getExplanation(){
        return "Refreshes all detected emulator instances.";
    }
}
