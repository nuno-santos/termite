package pt.inesc.termite.cli.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.TreeMap;

import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Device;
import pt.inesc.termite.cli.Devices;
import pt.inesc.termite.cli.Network;

/**
 * Check the status the currently registered devices by opening a socket
 * to their respective control ports
 *
 */
public class PingCommand extends Command {

	private static int PROBE_TIMEOUT = 1000;
	
	public PingCommand(String name, String abrv) {
		super(name,abrv);
	}

	public PingCommand() {
		super("ping","g");
	}

	public boolean executeCommand(Context context, String [] args) {

		assert context != null && args != null;

        if (context.mCurrentEmulation == null) {
            printError("No emulation is currently active.");
            return false;
        }

        Network network = context.mCurrentEmulation.getNetwork();
		Devices devices = network.getDevices();

		if (args.length != 1) {
			printError("Wrong number of input arguments.");
			return false;
		}
		
		// nothing to do if true
		if (devices.numDevices() == 0) {
			return true;
		}

		// check if the devices are online
		Socket client;
		boolean online;
		TreeMap<String,Boolean> probe = new TreeMap<String,Boolean>();
		for(Device dev : devices.getDevices()) {
			online = true;
			try {
				printMsg("Probing " + dev.getName() + "...");
				client = new Socket();
				client.connect(new InetSocketAddress(
						dev.getCtrlRealIp(), dev.getCtrlRealPort()), PROBE_TIMEOUT);

                // push the device info to the virtual device
                PrintWriter printwriter = new PrintWriter(client.getOutputStream(), true);
                printwriter.write("<PING>\n");
                printwriter.flush();

                // receive the acknowledgment (OK)
                InputStreamReader inputStreamReader = new InputStreamReader(
                        client.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String message = bufferedReader.readLine();

                // close the streams and the socket
                inputStreamReader.close();
                printwriter.close();
				client.close();

                if (message != null && !"<OK>".equals(message)) {
                    System.out.println("Error: message unexpected '" + message + "'.");
                    online = false;
                }
			} catch (UnknownHostException e) {
				online = false;
			} catch (IOException e) {
				online = false;
			}
			probe.put(dev.getName(), new Boolean(online));
		}

		// print the results
		for(Device dev : devices.getDevices()) {
			System.out.println(dev.getName() + "\t" + dev.getCtrlRealIp() + 
					"\t" + dev.getCtrlRealPort() + "\t" + 
					(probe.get(dev.getName()).booleanValue()?"ONLINE":"OFFLINE"));
		}

		return true;
	}

	public void printHelp() {
		System.out.println("Syntax: ping|g");
	}
}
