package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;
import main.pt.inesc.termite2.cli.Emulator;
import main.pt.inesc.termite2.cli.EmusTracker;
import main.pt.inesc.termite2.cli.exceptions.RemoteAVDControllerException;

import java.util.ArrayList;

public class StopEmulatorCommand  extends Command {

    public StopEmulatorCommand() {
        super("stopemu", Command.NULLABVR);
    }

    @Override
    public String executeCommand(Context context, String[] args) {
        assert context != null && args != null;

        if (args.length != 2) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        // Validate emulator port
        String emuId = args[1];
        Emulator emulator = context.mCurrentEmulation.getEmusTracker().getEmusList().get(emuId);
        if(emulator == null){
            return returnError("Emulator " + emuId + " does not exist.");
        }

        String cmdResult = context.mRemoteAVDController.stopEmulator(emulator.getIp(), emulator.getPort());
        if(cmdResult.equals(OK)){
            //Refresh emuTracker
            ArrayList<Emulator> emulators = context.mRemoteAVDController.getEmulators();
            EmusTracker et = context.mCurrentEmulation.getEmusTracker();
            et.updateEmuList(new ArrayList<>(emulators));
            return OK;
        }

        return cmdResult;

    }

    public void printHelp() {
        System.out.println("Syntax: stopemu <emu-id>");
    }

    public String getExplanation(){
        return "Stops chosen emulator instance.";
    }
}