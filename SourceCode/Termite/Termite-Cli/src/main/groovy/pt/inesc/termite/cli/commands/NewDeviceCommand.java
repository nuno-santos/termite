package pt.inesc.termite.cli.commands;


import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Devices;
import pt.inesc.termite.cli.Network;

public class NewDeviceCommand extends Command {

    public NewDeviceCommand(String name, String abrv) {
        super(name,abrv);
    }

    public NewDeviceCommand() {
        super("newdevice", "-");
    }

    @SuppressWarnings("unchecked")
    public boolean executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (context.mCurrentEmulation == null) {
            printError("No emulation is currently active.");
            return false;
        }

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();

        if (args.length != 2) {
            printError("Wrong number of input arguments.");
            printHelp();
            return false;
        }

        // check the device name
        String deviceName = args[1];
        if (!devices.checkDevice(deviceName)) {
            printError("Device already registered with name \"" + deviceName + "\"");
            return false;
        }

        // register the device
        int devId = devices.getDeviceId();
        devices.addDevice(deviceName, "0.0.0.0", devId, "0.0.0.0", devId,
                "0.0.0.0", devId);
        devices.incDeviceId();
        return true;
    }

    public void printHelp() {
        System.out.println("Syntax: newdevice <device-id>");
    }
}
