package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;
import main.pt.inesc.termite2.cli.Devices;
import main.pt.inesc.termite2.cli.Network;

public class NewDeviceCommand extends Command {

    public NewDeviceCommand() {
        super("newdevice", Command.NULLABVR);
    }

    public String executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();

        if (args.length != 2) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        // check the device name
        String deviceName = args[1];
        if (!devices.checkDevice(deviceName)) {
            return returnError("Device already registered with name \"" + deviceName + "\"");
        }

        // register the device
        devices.addDevice(deviceName, "0.0.0.0", 0, 0, 0);
        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: newdevice <device-id>");
    }

    public String getExplanation(){
        return "Creates a new virtual device.";
    }
}
