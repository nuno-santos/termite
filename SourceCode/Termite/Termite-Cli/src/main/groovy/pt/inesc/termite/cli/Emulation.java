package pt.inesc.termite.cli;

import java.util.HashMap;

public class Emulation {

    private Backend mBackend;
    private Experiment mExperiment;
    private HashMap<String, String> mBinders;                   // device-id -> emulator-id
    private HashMap<String, AddressSet> mAssignedAddresses;     // emulator-id -> addressset
//    private HashMap<String,String> mVirtualIDs;                 // virtual-id -> emulador-id
    private Network mNetwork;
    private EmuTracker mEmuTracker;

    public Emulation(Backend backend, Experiment exp) {
        mBackend = backend;
        mExperiment = exp;
        mBinders = new HashMap<>();
        mAssignedAddresses = new HashMap<>();
//        mVirtualIDs = new HashMap<>();
        mNetwork = new Network();
        mEmuTracker = new EmuTracker();
    }

    public Backend getBackend() {
        return mBackend;
    }

    public Experiment getExperiment() {
        return mExperiment;
    }

    public HashMap<String,String> getBinders() {
        return mBinders;
    }

    public Network getNetwork() {
        return mNetwork;
    }

    public HashMap<String, AddressSet> getAssignedAddresses() {
        return mAssignedAddresses;
    }

    /*
    public HashMap<String, String> getVirtualIDs() {
        return mVirtualIDs;
    }
    */

    public EmuTracker getEmuTracker() {
        return mEmuTracker;
    }
}
