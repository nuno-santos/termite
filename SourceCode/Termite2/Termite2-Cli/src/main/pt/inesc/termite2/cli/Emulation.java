package main.pt.inesc.termite2.cli;


import java.util.ArrayList;
import java.util.HashMap;


/*
* This class represents the current devices and emulators interactions within the program.
* Here we can store and obtain the current available and connected emulators to be binded with the created virtual devices
* */
public class Emulation {

    private HashMap<String, String> mBinds; // device-id -> emu-id (ex: a -> e1, b -> e2, ...)
    private ArrayList<Emulator> mAvailableEmulators; // connected and available emulators
    private EmusTracker mEmusTracker;

    private Network mNetwork;

    public Emulation(){
        mBinds = new HashMap<>();
        mAvailableEmulators = new ArrayList<>();
        mNetwork = new Network();
        mEmusTracker = new EmusTracker();
    }

    public HashMap<String, String> getBinds(){
        return mBinds;
    }

    public ArrayList<Emulator> getAvailableEmulators(){
        return mAvailableEmulators;
    }

    public Network getNetwork() {
        return mNetwork;
    }

    public EmusTracker getEmusTracker() {
        return mEmusTracker;
    }

    public void clearBinds(){
        mBinds.clear();
    }
}
