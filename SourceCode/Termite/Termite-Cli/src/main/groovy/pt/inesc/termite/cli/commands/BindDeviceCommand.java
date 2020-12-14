package pt.inesc.termite.cli.commands;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.inesc.termite.cli.AddressSet;
import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.ConnectorDriver;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Devices;
import pt.inesc.termite.cli.EmuTracker;
import pt.inesc.termite.cli.Network;

public class BindDeviceCommand extends Command {

    public BindDeviceCommand(String name, String abrv) {
        super(name,abrv);
    }

    public BindDeviceCommand() {
        super("binddevice","-");
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

        if (args.length != 3) {
            printError("Wrong number of input arguments.");
            printHelp();
            return false;
        }

        // check the device name
        String deviceName = args[1];
        if (devices.checkDevice(deviceName)) {
            printError("Device \"" + deviceName + "\" does not exist.");
            return false;
        }

        EmuTracker et = context.mCurrentEmulation.getEmuTracker();
        String eid = et.getEmuList().get(args[2]);
        if (eid == null) {
            printError("Emulator '" + args[2] + "' does not exist.");
            return false;
        }

        // check if the targets device or emulator are already bound
        if (context.mCurrentEmulation.getBinders().containsKey(deviceName)) {
            printError("Device '" + deviceName + "' is already bound.");
            return false;
        }
        if (context.mCurrentEmulation.getBinders().containsValue(eid)) {
            printError("Emulator '" + eid + "' is already bound.");
            return false;
        }

        // check that the target emulator's addresses have been set up
        AddressSet addr = context.mCurrentEmulation.getAssignedAddresses().get(eid);
        if (addr == null) {
            printError("Must assign emulator '" + eid + "' with valid addresses first.");
            return false;
        }

        // parse the sequence of addresses
        DeviceAddr appVirtAddr = new DeviceAddr();
        if (!parseDeviceAddr(devices, addr.mAVAddr, appVirtAddr)) {
            printError("Unable to parse \"" + addr.mAVAddr + "\"");
            return false;
        }
        DeviceAddr appRealAddr = new DeviceAddr();
        if (!parseDeviceAddr(devices, addr.mARAddr, appRealAddr)) {
            printError("Unable to parse \"" + addr.mARAddr + "\"");
            return false;
        }
        DeviceAddr ctlrRealAddr = new DeviceAddr();
        if (!parseDeviceAddr(devices, addr.mCRAddr, ctlrRealAddr)) {
            printError("Unable to parse \"" + addr.mCRAddr + "\"");
            return false;
        }

        // register the device
        devices.updateDevice(deviceName, ctlrRealAddr.mIP, ctlrRealAddr.mPort,
                appRealAddr.mIP, appRealAddr.mPort, appVirtAddr.mIP, appVirtAddr.mPort);
        context.mCurrentEmulation.getBinders().put(deviceName, eid);

        return true;
    }

    public void printHelp() {
        System.out.println("Syntax: binddevice <device-id> <emu-id>");
    }

    class DeviceAddr {
        public String mIP;
        public int mPort;
    }

    private boolean parseDeviceAddr(Devices devices,
                                    String addrStrIn, DeviceAddr addrOut) {
        assert addrStrIn != null && addrOut != null;

        // split the address into two tokens IP and port
        String [] tokens = addrStrIn.split(":");
        if (tokens.length != 2) {
            printError("Malformed address format IP:port.");
            return false;
        }

        // validate the IP address
        String ip = tokens[0];
        IPAddressValidator ipChecker = new IPAddressValidator();
        if (!ipChecker.validate(ip)) {
            printError("Wrong IP address format.");
            return false;
        }

        // validate the port number
        int port = 0;
        try {
            port = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            printError("Wrong port number format.");
            return false;
        }

        // check that the address is not in use
        if (!devices.checkAddress(ip,port)) {
            printError("Device already registered with address \"" + ip +
                    ":" + port + "\"");
            return false;
        }

        // everything is ok
        addrOut.mIP = ip;
        addrOut.mPort = port;
        return true;
    }

    class IPAddressValidator {

        private Pattern pattern;
        private Matcher matcher;

        private static final String IPADDRESS_PATTERN =
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

        public IPAddressValidator() {
            pattern = Pattern.compile(IPADDRESS_PATTERN);
        }

        /**
         * Validate ip address with regular expression
         * @param ip ip address for validation
         * @return true valid ip address, false invalid ip address
         */
        public boolean validate(final String ip) {
            matcher = pattern.matcher(ip);
            return matcher.matches();
        }
    }
}
