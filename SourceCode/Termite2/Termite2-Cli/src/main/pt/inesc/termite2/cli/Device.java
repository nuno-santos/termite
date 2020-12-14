package main.pt.inesc.termite2.cli;

import java.util.ArrayList;

public class Device {

    private String mName;
    private String mIp; // this is the ip of the remote machine were the binded emulator to this device is running
    private int mEmuPort;
    private int mCommitPort;
    private int mMessagePort;

    private ArrayList<String> mNeighbors;

    public Device(String name, String ip, int emuPort, int commitPort, int messagePort ){
        mName = name;
        mIp = ip;
        mEmuPort = emuPort;
        mCommitPort = commitPort;
        mMessagePort = messagePort;
        mNeighbors = new ArrayList<String>();
    }

    public String getName() {
        return mName;
    }

    public String getIp() {
        return mIp;
    }

    public int getEmuPort() {
        return mEmuPort;
    }

    public int getCommitPort() {
        return mCommitPort;
    }

    public int getMessagePort() {
        return mMessagePort;
    }

    public ArrayList<String> getNeighbors() {
        return mNeighbors;
    }

    public boolean hasNeighbor(String neighbor) {
        for (String peer : getNeighbors()) {
            if (peer.equals(neighbor)) {
                return true;
            }
        }
        return false;
    }

    public void removeNeighbor(String neighbor) {
        if (hasNeighbor(neighbor)) {
            getNeighbors().remove(neighbor);
        }
    }

    public void print() {
        System.out.println("Device = [Name:" + mName +
                ", Ip:" + mIp + ", emulatorPort:" + mEmuPort +
                ", CommitPort:" + mCommitPort + ", MessagePort:" + mMessagePort + "]");
    }
}
