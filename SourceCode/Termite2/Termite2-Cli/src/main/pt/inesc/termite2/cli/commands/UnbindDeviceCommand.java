package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

public class UnbindDeviceCommand extends Command {

    public UnbindDeviceCommand() {
        super("unbinddevice",Command.NULLABVR);
    }

    public String executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();
        Groups groups = network.getGroups();

        if (args.length != 2) {
            return returnError("Wrong number of input arguments.");
        }

        String deviceName = args[1];
        if (devices.checkDevice(deviceName)) {
            return returnError("No device registered with name \"" + deviceName + "\"");
        }
        devices.updateDevice(deviceName, "0.0.0.0", 0, 0, 0);
        groups.removeGroup(deviceName);
        context.mCurrentEmulation.getBinds().remove(deviceName);

        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: unbinddevice <device-id>");
    }

    public String getExplanation(){
        return "UnBinds chosen device to binded emulator.";
    }
}
