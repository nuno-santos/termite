package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

public class BindDeviceCommand extends Command {

    public BindDeviceCommand() {
        super("binddevice",Command.NULLABVR);
    }

    @SuppressWarnings("unchecked")
    public String executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();

        if (args.length != 3) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        // check the device name
        String deviceName = args[1];
        if (devices.checkDevice(deviceName)) {
            return returnError("Device \"" + deviceName + "\" does not exist.");
        }

        EmusTracker et = context.mCurrentEmulation.getEmusTracker();
        String eid = args[2];
        if(!et.getEmusList().containsKey(args[2])){ //HashMap<String,Emulator> mEmuList; //e1 -> Emulator_obj, e2 -> emulator_obj
            return returnError("Emulator '" + args[2] + "' does not exist.");
        }

        // check if the targets device or emulator are already bound
        if (context.mCurrentEmulation.getBinds().containsKey(deviceName)) { // HashMap<String, Emulator> getBinds()
            return returnError("Device '" + deviceName + "' is already bound.");
        }
        if (context.mCurrentEmulation.getBinds().containsValue(eid)) {
            return returnError("Emulator '" + eid + "' is already bound.");
        }

        // Gets the necessary emulator ip and ports
        String networkIp = et.getEmusList().get(eid).getIp();
        int emuPort = et.getEmusList().get(eid).getPort();
        int commitPort = et.getEmusList().get(eid).getCommitPort();
        int messagePort = et.getEmusList().get(eid).getMessagePort();

        // register the device, updateDevice(String name, String ip, int port, int cport, int mport)
        devices.updateDevice(deviceName, networkIp, emuPort, commitPort, messagePort);
        context.mCurrentEmulation.getBinds().put(deviceName, eid);

        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: binddevice <device-id> <emu-id>");
    }

    public String getExplanation(){
        return "Binds a created virtual devices to an emulator instance.";
    }
}
