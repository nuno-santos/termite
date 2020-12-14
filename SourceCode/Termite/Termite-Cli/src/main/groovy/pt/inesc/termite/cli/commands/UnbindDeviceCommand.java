package pt.inesc.termite.cli.commands;


import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.ConnectorDriver;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Devices;
import pt.inesc.termite.cli.Groups;
import pt.inesc.termite.cli.Network;

public class UnbindDeviceCommand extends Command {

    public UnbindDeviceCommand(String name, String abrv) {
        super(name,abrv);
    }

    public UnbindDeviceCommand() {
        super("unbinddevice","-");
    }

    public boolean executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (context.mCurrentEmulation == null) {
            printError("No emulation is currently active.");
            return false;
        }

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();
        Groups groups = network.getGroups();

        if (args.length != 2) {
            printError("Wrong number of input arguments.");
            return false;
        }

        String deviceName = args[1];
        if (devices.checkDevice(deviceName)) {
            printError("No device registered with name \"" + deviceName + "\"");
            return false;
        }
        int devId = devices.getDeviceId();
        devices.updateDevice(deviceName, "0.0.0.0", devId, "0.0.0.0", devId, "0.0.0.0", devId);
        devices.incDeviceId();
        groups.removeGroup(deviceName);
        context.mCurrentEmulation.getBinders().remove(deviceName);

        return true;
    }

    public void printHelp() {
        System.out.println("Syntax: unbinddevice <device-id>");
    }
}