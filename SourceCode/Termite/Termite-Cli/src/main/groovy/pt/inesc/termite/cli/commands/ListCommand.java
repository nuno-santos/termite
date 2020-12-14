package pt.inesc.termite.cli.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pt.inesc.termite.cli.AddressSet;
import pt.inesc.termite.cli.Application;
import pt.inesc.termite.cli.Backend;
import pt.inesc.termite.cli.Command;
import pt.inesc.termite.cli.ConnectorDriver;
import pt.inesc.termite.cli.Context;
import pt.inesc.termite.cli.Device;
import pt.inesc.termite.cli.Devices;
import pt.inesc.termite.cli.EmuTracker;
import pt.inesc.termite.cli.Experiment;
import pt.inesc.termite.cli.Group;
import pt.inesc.termite.cli.Groups;
import pt.inesc.termite.cli.IConnectorDriver;
import pt.inesc.termite.cli.NetProfile;
import pt.inesc.termite.cli.Network;

public class ListCommand extends Command {
	
	public ListCommand(String name, String abrv) {
		super(name,abrv);
	}

	public ListCommand() {
		super("list","ls");
	}

	public boolean executeCommand(Context context, String [] args) {

		assert context != null && args != null;

		if (args.length != 2) {
			printError("Wrong number of input arguments.");
			printHelp();
			return false;
		}
		
		Command [] commands = context.getCommands();
		String option = args[1];

        if (option.equals("applications") || option.equals("apps")) {
            listApplications(context, args);
            return true;
        }
        if (option.equals("backends") || option.equals("backs")) {
            listBackends(context, args);
            return true;
        }
        if (option.equals("experiments") || option.equals("exps")) {
            listExperiments(context, args);
            return true;
        }
        if (option.equals("emulators") || option.equals("emus")) {
            listEmulators(context, args);
            return true;
        }
        if (option.equals("netprofiles") || option.equals("nps")) {
            listNetProfiles(context, args);
            return true;
        }
        if (option.equals("properties") || option.equals("p")) {
            listProps(context);
            return true;
        }
        if (option.equals("history") || option.equals("h")) {
            listHistory(context);
            return true;
        }

        /*
         * options related with ongoing emulation
         */

        if (context.mCurrentEmulation == null) {
            printError("No emulation is currently active.");
            return false;
        }

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();
        Groups groups = network.getGroups();

        if (option.equals("devices") || option.equals("d")) {
			listDevices(devices, commands, args);
			return true;
		}
		if (option.equals("neighbors") || option.equals("n")) {
			listNeighbors(devices, commands, args);
			return true;
		}
		if (option.equals("groups") || option.equals("g")) {
			listGroups(groups, commands, args);
			return true;
		}
		if (option.equals("network") || option.equals("net")) {
			listNetwork(network, commands, args);
			return true;
		}
		printError("Unknown option \"" + option + "\"");
		return false;
	}

	protected void listDevices(Devices devices, Command [] commands, String [] args) {
		for(Device dev : devices.getDevices()) {
			System.out.println(dev.getName() + "\t" + 
				dev.getAppVirtIp() + ":" + dev.getAppVirtPort() + "\t" +
				dev.getAppRealIp() + ":" + dev.getAppRealPort() + "\t" +
				dev.getCtrlRealIp() + ":" + dev.getCtrlRealPort());
		}
	}

	protected void listGroups(Groups groups, Command [] commands, String [] args) {
		for (Group group : groups.getGroups()) {
			System.out.print(group.getGo().getName() + " => ");
			int i = 0;
			for (Device client : group.getClientList()) {
				System.out.print(client.getName());
				i++;
				if (i < group.getClientList().size()) {
					System.out.print(",");
				}
			}
			System.out.println("");
		}
	}

	protected void listNeighbors(Devices devices, Command [] commands, String [] args) {
		for (Device device: devices.getDevices()) {
			System.out.print(device.getName() + " => ");
			ArrayList<String> neighbors = device.getNeighbors();
			int i = 0;
			for (String client : neighbors) {
				System.out.print(client);
				i++;
				if (i < neighbors.size()) {
					System.out.print(",");
				}
			}
			System.out.println();
		}
	}

	protected void listNetwork(Network network, Command [] commands, String [] args) {
		Devices devices = network.getDevices();
		Groups groups = network.getGroups();
		for (Device device: devices.getDevices()) {

			// list details of the neighbors of the current node
			System.out.println("Node " + device.getName());
			System.out.print("   Peers:");
			ArrayList<String> neighbors = device.getNeighbors();
			int i = 0;
			for (String client : neighbors) {
				System.out.print(client);
				i++;
				if (i < neighbors.size()) {
					System.out.print(",");
				}
			}
			System.out.println();
			
			System.out.println("   Groups:");

			// Print the list of groups that the current node is client of
			String clientOf = groups.getStrGroupsContaining(device.getName());
			System.out.println("      ClientOf: " +
					((clientOf != null && !clientOf.equals(""))?clientOf:"No"));

			// Print details of the group that the current node is owner of
			Group group = groups.getGroup(device.getName());
			System.out.println("      GO: " + ((group != null)?"Yes":"No ") +
				((group != null)?(" (" + group.getClientsStr() + ")"):""));
		}
	}

	protected void listProps(Context context) {
		for (String prop : context.getProperties().get()) {
			System.out.println(prop);
		}
	}
	
	protected void listHistory(Context context) {
		String last = context.getHistory().getLast();
		if (last != null) {
			System.out.println(last);
		}
	}

    protected void listApplications(Context context, String [] args) {
        Map<String,Application> apps = context.mConfigManager.getApplications();
        ArrayList<String> list = new ArrayList<>(apps.keySet());
        Collections.sort(list);
        System.out.println();
        for (String key : list) {
            Application app = apps.get(key);
            System.out.println("" + app.getId());
            System.out.println("   appid    : " + app.getAppId());
            System.out.println("   cctivity : " + app.getActivity());
            System.out.println("   apk      : " + app.getApk());
            System.out.println("   rPath    : " + app.getRPath());
            System.out.println();
        }
    }

    protected void listBackends(Context context, String [] args) {
        Map<String,Backend> backends = context.mConfigManager.getBackends();
        ArrayList<String> list = new ArrayList<>(backends.keySet());
        Collections.sort(list);
        System.out.println();
        for (String key : list) {
            Backend backend = backends.get(key);
            System.out.println("" + backend.getId());
            System.out.println("   connector : " + backend.getConnector());
            System.out.println("   config    : " + backend.getConfig());
            System.out.println();
        }
    }

    protected void listEmulators(Context context, String [] args) {
        if (context.mCurrentBackend == null) {
            printError("No backend is currently active.");
        }

        ConnectorDriver ct = context.mCurrentBackend.getConnectorTarget();
        assert ct != null;

        Map<String,Integer> emulators = null;
        try {
            emulators = ct.getEmulators();
        } catch(Exception e) {
            printError("Could not list emulators.");
            System.out.println(e.getMessage());
            return;
        }

        EmuTracker et = context.mCurrentEmulation.getEmuTracker();
        et.updateEmuList(new ArrayList<>(emulators.keySet()));
        HashMap<String,String> map = et.getEmuList();

        ArrayList<String> list = new ArrayList<>(map.keySet());
        Collections.sort(list);
        for (String id : list) {
            String s = map.get(id);     // real emulator name
            int i = emulators.get(s);   // emulator state
            switch (i) {
                case IConnectorDriver.EMU_STATE_NETOK:
                    assert context.mCurrentEmulation != null;
                    AddressSet addr = context.mCurrentEmulation.getAssignedAddresses().get(s);
                    System.out.println(id + " => " + IConnectorDriver.EMU_STATE_NAME[i]);
                    System.out.println("    name: " + s);
                    System.out.print("    addr: ");
                    addr.print();
                    break;
                default:
                    System.out.println(id + " => " + IConnectorDriver.EMU_STATE_NAME[i]);
                    System.out.println("    name: " + s);
            }
        }
    }

    protected void listExperiments(Context context, String [] args) {
        Map<String,Experiment> exps = context.mConfigManager.getExperiments();
        ArrayList<String> list = new ArrayList<>(exps.keySet());
        Collections.sort(list);
        System.out.println();
        for (String key : list) {
            Experiment exp = exps.get(key);
            System.out.println("" + exp.getId());
            System.out.println("   application : " + exp.getApplication());
            System.out.println("   backend     : " + exp.getBackend());
            System.out.println("   netprofile  : " + exp.getNetProfile());
            System.out.println();
        }
    }

    protected void listNetProfiles(Context context, String [] args) {
        Map<String,NetProfile> netProfiles = context.mConfigManager.getNetProfiles();
        ArrayList<String> list = new ArrayList<>(netProfiles.keySet());
        Collections.sort(list);
        System.out.println();
        for (String key : list) {
            NetProfile netProfile = netProfiles.get(key);
            System.out.println("" + netProfile.getId());
            System.out.println("   connector : " + netProfile.getConnector());
            System.out.println("   config    : " + netProfile.getConfig());
            System.out.println();
        }
    }

	public void printHelp() {
		System.out.println("Syntax: list|ls <what>");
        System.out.println("Options:");
        System.out.println("   applications|apps, backends|backs, experiments|exps,");
        System.out.println("   emulators|emus, netprofiles|nps, devices|d, groups|g,");
		System.out.println("   neighbors|n, network|net, properties|p, history|h");
	}
}
