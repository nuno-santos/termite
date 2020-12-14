package pt.inesc.termite.cli;

public class Network {
	
	private Devices mDevices;
	private Groups mGroups;
	
	public Network () {
		mDevices = new Devices();
		mGroups = new Groups();
	}
	
	public Network (Devices devices, Groups groups) {
		mDevices = devices;
		mGroups = groups;
	}
	
	public Devices getDevices() {
		return mDevices;
	}
	
	public Groups getGroups() {
		return mGroups;
	}
}
