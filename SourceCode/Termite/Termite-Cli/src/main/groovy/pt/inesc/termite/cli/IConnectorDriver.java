package pt.inesc.termite.cli;


import java.util.Map;

public interface IConnectorDriver {

    public static final int EMU_STATE_INIT    = 1; // deployed but not necessarily booting
    public static final int EMU_STATE_OFFLINE = 2; // booting up
    public static final int EMU_STATE_ONLINE  = 3; // finished bootstrapping but not bound yet
    public static final int EMU_STATE_NETOK   = 4; // bound to a given node (port forwarding set)

    public static final String[] EMU_STATE_NAME = {
            "", "init", "offline", "online", "netok"};

    void check();

    void deployEmulator();

    void killEmulator(String eid);

    void stopEmulator(String eid);

    void startEmulator(String eid);

    void initializeAddressProvider(Map params);

    AddressSet assignAddressSet(String eid);

    void unassignAddressSet(String eid);

    void finalizeAddressProvider();

    void installApp(String eid, String apkPath);

    void runApp(String eid, String appId, String activity);

    Map<String,Integer> getEmulators();

}
