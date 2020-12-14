package main.pt.inesc.termite2.cli;

import main.pt.inesc.termite2.cli.exceptions.NeighborMalformedException;

import java.util.Collection;
import java.util.TreeMap;

public class Devices {

    private TreeMap<String,Device> mDevices; // device_Name -> device_Obj , stores all created devices

    public Devices(){
        mDevices = new TreeMap<String,Device>();
    }

    public void clear(){
        mDevices.clear();
    }

    public boolean checkDevice(String name){
        return !mDevices.containsKey(name);
    }

    public boolean existsDevice(String name) {
        return mDevices.containsKey(name);
    }

    public void addDevice(String name, String ip, int port, int cport, int mport){
        assert !mDevices.containsKey(name) : "Check if device already exists";
        mDevices.put(name, new Device(name, ip, port, cport, mport));
    }

    public void updateDevice(String name, String ip, int port, int cport, int mport){
        mDevices.remove(name);
        mDevices.put(name, new Device(name, ip, port, cport, mport));
    }

    public void removeDevice(String name){
        for (Device device : mDevices.values()) {
            device.removeNeighbor(name);
        }
        mDevices.remove(name);
    }

    public Device getDevice(String name) {
        return mDevices.get(name);
    }

    public Collection<Device> getDevices() {
        return mDevices.values();
    }

    public int numDevices(){
        return mDevices.size();
    }

    public String marshalDeviceNeighbors(Device device) {
        String deviceOwnNetwork = device.getIp();
        StringBuilder sb = new StringBuilder(128);
        for (String neighbor : device.getNeighbors()) {
            Device ndev = getDevice(neighbor);
            if(!(ndev.getIp().equals("0.0.0.0"))){
                sb.append(neighbor + ":" +
                        ndev.getIp() + ":" + ndev.getMessagePort() + ":" +
                        deviceOwnNetwork  + ":" + "7000" + "@");
            }
        }
        return sb.toString();
    }

    public String marshalDevices() {
        StringBuilder sb = new StringBuilder();
        for (Device device : mDevices.values()) {
            if(!(device.getIp().equals("0.0.0.0"))){
                sb.append(device.getName() + ":" +
                        device.getIp() + ":" + device.getMessagePort() + ":" +
                        device.getIp() + ":" + "7000" + "@");
            }
        }
        return sb.toString();
    }
}
