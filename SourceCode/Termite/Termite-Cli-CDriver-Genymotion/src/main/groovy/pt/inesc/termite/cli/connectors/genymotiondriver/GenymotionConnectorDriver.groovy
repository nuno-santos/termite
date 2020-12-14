package pt.inesc.termite.cli.connectors.genymotiondriver

import pt.inesc.termite.cli.AddressSet
import pt.inesc.termite.cli.ConfigManager
import pt.inesc.termite.cli.Connector
import pt.inesc.termite.cli.ConnectorDriver
import pt.inesc.termite.cli.IAddressProvider
import pt.inesc.termite.cli.exceptions.ConnectorTargetException

public class GenymotionConnectorDriver extends ConnectorDriver {

    private String mGMPath
    private String mVmiPrefix
    private String mSdk
    private int mNumClones

    private HashMap<String,Integer> mEmulators
    private HashMap<String,AddressSet> mAssignedEmulators

    private ArrayList<String> mFreeCloneList
    private ArrayList<String> mBusyCloneList

    private IAddressProvider mAddrProvider


    private boolean mAPInitialized

    public GenymotionConnectorDriver(ConfigManager configManager, Connector connector, Map config) {
        super(configManager, connector, config)

        if (config == null) {
            throw new ConnectorTargetException("Must provide configuration parameters.")
        }
        if (config['path'] == null) {
            throw new ConnectorTargetException("Must define the 'path' config paramenter.")
        }
        if (config['vmiprefix'] == null) {
            throw new ConnectorTargetException("Must define the 'vmiprefix' config paramenter.")
        }
        if (config['sdk'] == null) {
            throw new ConnectorTargetException("Must define the 'sdk' config paramenter.")
        }
        if (config['numclones'] == null) {
            throw new ConnectorTargetException("Must define the 'numclones' config paramenter.")
        }
        mGMPath = config['path']
        mVmiPrefix = config['vmiprefix']
        mSdk = config['sdk']
        try {
            mNumClones = Integer.parseInt((String)config['numclones']);
        } catch (Exception e) {
            throw new ConnectorTargetException("Invalid clone number." + e.getMessage())
        }
        mEmulators = new HashMap<>()
        mAssignedEmulators = new HashMap<>()
        mAPInitialized = false

        mFreeCloneList = new ArrayList<>()
        mBusyCloneList = new ArrayList<>()

        // populate the free clone list
        for (int i = 0; i < mNumClones; i++) {
            mFreeCloneList.add("${mVmiPrefix}-Clone${i}")
        }
        Collections.sort(mFreeCloneList)
    }

    /*
     * Connector driver methods
     */

    @Override
    public void check() throws ConnectorTargetException {

        println "Checking Genymotion connector driver..."

        try {
            "${mGMPath}/player".execute()
        } catch(Exception e) {
            throw new ConnectorTargetException("Genymotion player tool not found." +
                    e.getMessage())
        }

        try {
            "${mSdk}/platform-tools/adb".execute()
        } catch(Exception e) {
            throw new ConnectorTargetException("Android adb tool not found. " +
                    e.getMessage())
        }
    }

    @Override
    void deployEmulator() throws ConnectorTargetException {
        assert mGMPath != null && mVmiPrefix != null

        if (mFreeCloneList.isEmpty()) {
            throw new ConnectorTargetException("No more clones available.")
        }

        String cloneName = mFreeCloneList.remove(0);
        mBusyCloneList.add(cloneName)
        def exec = ["${mGMPath}/player", "--vm-name", cloneName]

        Thread.start {
            try {
                def process = exec.execute()
                print "${process.text}"
            } catch(ConnectorTargetException e) {
                throw e
            } catch (Exception e) {
                println "Error: Unable to deploy emulator."
                println e.getMessage()
            }
        }
    }

    @Override
    void killEmulator(String eid) throws ConnectorTargetException {
        throw new ConnectorTargetException("Operation not supported.")

        /*

        assert mSdk != null

        if (eid == null) {
            throw new ConnectorTargetException("Emulator id is invalid.")
        }

        if (mBusyCloneList.contains(eid)) {
            mBusyCloneList.remove(eid)
        }
        mFreeCloneList.add(eid)
        Collections.sort(mFreeCloneList)

        def process = """./adb -s ${eid} shell reboot -p &""".execute(
                null, new File("${mSdk}/platform-tools")
        )
        print "${process.text}"
        */

    }

    @Override
    void stopEmulator(String eid) throws ConnectorTargetException {
        throw new ConnectorTargetException("Operation not supported.")
    }

    @Override
    void startEmulator(String eid) throws ConnectorTargetException {
        throw new ConnectorTargetException("Operation not supported.")
    }

    @Override
    void initializeAddressProvider(Map params) throws ConnectorTargetException {

        if (params == null) {
            throw new ConnectorTargetException("Addresses missing.")
        }

        mAssignedEmulators.clear()

        if (params.containsKey("manual")) {
            mAddrProvider = new ManualAVDAddressProvider(params);
            return;
        }

        if (params.containsKey("auto")) {
            mAddrProvider = new AutoAVDAddressProvider(params);
            return;
        }

        throw new ConnectorTargetException("Address assignment mode not supported.")
    }

    @Override
    AddressSet assignAddressSet(String eid) throws ConnectorTargetException {

        if (mAddrProvider == null) {
            throw new ConnectorTargetException("Address provider was not initialized.")
        }

        if (eid == null) {
            throw new ConnectorTargetException("Emulator id missing.")
        }

        if (mAssignedEmulators[(eid)] != null) {
            throw new ConnectorTargetException("Addresses already assigned to '${eid}'.")
        }

        /*
         * obtain an address set and update the CLI internal state right away
         */

        AddressSet addr = mAddrProvider.claimAddressSet()
        if (addr == null) {
            throw new ConnectorTargetException("There are no available address sets.")
        }
        mAssignedEmulators[(eid)] = addr
        mEmulators[(eid)] = EMU_STATE_NETOK

        /*
         * perform port forwarding
         */

        int avport = getAddrPort(addr.mAVAddr);
        int arport = getAddrPort(addr.mARAddr);
        int cvport = getAddrPort(addr.mCVAddr);
        int crport = getAddrPort(addr.mCRAddr);
        if (avport < 0 || arport < 0 || cvport < 0 || crport < 0) {
            throw new ConnectorTargetException("Invalid port numbers.")
        }

        try {
            "${mSdk}/platform-tools/adb -s ${eid} forward tcp:${arport} tcp:${avport}".execute()
        } catch(Exception ignored) {
            throw new ConnectorTargetException("Could not forward ports ${arport}=>${avport} " +
                    "on emulator ${eid}.")
        }
        try {
            "${mSdk}/platform-tools/adb -s ${eid} forward tcp:${crport} tcp:${cvport}".execute()
        } catch(Exception ignored) {
            throw new ConnectorTargetException("Could not forward ports ${crport}=>${cvport} " +
                    "on emulator ${eid}.")
        }

        return addr
    }

    @Override
    void unassignAddressSet(String eid) throws ConnectorTargetException {

        if (mAddrProvider == null) {
            throw new ConnectorTargetException("Address provider was not initialized.")
        }

        if (eid == null) {
            throw new ConnectorTargetException("Emulator id missing.")
        }

        /*
         * check if the emulator is bound and obtain its address set
         */

        AddressSet addr = mAssignedEmulators[(eid)]
        if (addr == null) {
            return
        }

        /*
         * update the internal state right away to prevent inconsistencies
         */

        mAddrProvider.releaseAddressSet(addr)
        mAssignedEmulators.remove(eid)
        if (mEmulators[(eid)] >= EMU_STATE_NETOK) {
            mEmulators[(eid)] = EMU_STATE_ONLINE
        }

        /*
         * remove port forwarding rules
         */

        int avport = getAddrPort(addr.mAVAddr);
        int arport = getAddrPort(addr.mARAddr);
        int cvport = getAddrPort(addr.mCVAddr);
        int crport = getAddrPort(addr.mCRAddr);
        if (avport < 0 || arport < 0 || cvport < 0 || crport < 0) {
            throw new ConnectorTargetException("Invalid port numbers.")
        }

        try {
            "${mSdk}/platform-tools/adb -s ${eid} forward --remove tcp:${arport}".execute()
        } catch(Exception ignored) {
            throw new ConnectorTargetException("Could not remove forwarded port ${arport}=>${avport} " +
                    "on emulator ${eid}.")
        }
        try {
            "${mSdk}/platform-tools/adb -s ${eid} forward --remove tcp:${crport}".execute()
        } catch(Exception ignored) {
            throw new ConnectorTargetException("Could not remove forwarded port ${crport}=>${cvport} " +
                    "on emulator ${eid}.")
        }
    }

    @Override
    void finalizeAddressProvider() throws ConnectorTargetException {

        mAssignedEmulators.clear()
        mAddrProvider = null

    }

    @Override
    void installApp(String eid, String apkPath) throws ConnectorTargetException {

        assert mSdk != null

        if (eid == null) {
            throw new ConnectorTargetException("Emulator id is invalid.")
        }
        if (apkPath == null) {
            throw new ConnectorTargetException("APK path is invalid.")
        }
        def file = new File("${apkPath}")
        if (!file.exists()) {
            throw new ConnectorTargetException("APK not found: ${apkPath}.")
        }

        def process1 = """./adb -s ${eid} install -r ${apkPath}""".execute(
                null, new File("${mSdk}/platform-tools")
        )

        println "${process1.text}"

        /*
        // FIXME - turn off warnings. Is there a better way to do this? - rodrigo
        def process2 = """grep -v WARNING""".execute()
        process1 | process2
        process2.waitFor()
        println process2.err.text
        println process2.in.text
        */
    }

    @Override
    void runApp(String eid, String appId, String activity) throws ConnectorTargetException {

        assert mSdk != null

        if (eid == null) {
            throw new ConnectorTargetException("Emulator id is invalid.")
        }
        if (appId == null) {
            throw new ConnectorTargetException("App ID is invalid.")
        }
        if (activity == null) {
            throw new ConnectorTargetException("Activity is invalid.")
        }

        def process = """./adb -s ${eid} shell am start -n ${appId}/${activity} -a android.intent.action.MAIN -c android.intent.category.LAUNCHER""".execute(
                null, new File("${mSdk}/platform-tools")
        )

        print "${process.text}"
    }

    @Override
    Map<String, Integer> getEmulators() throws ConnectorTargetException {
        assert mSdk != null

        String exec = "${mSdk}/platform-tools/adb devices"

        def process = exec.execute()

        HashMap<String,String> currList = new HashMap<String,String>()

        def out = process.text
        out.eachLine { line, count ->
            if (count > 0) {
                String[] split = line.tokenize(" \t")
                if (split.length == 2) {
                    currList[(split[0])] = split[1]
                }
            }
        }

        // first remove the stale entries from the emulators list
        HashMap<String,Integer> emuTmp = new HashMap<String,Integer>()
        for (String e : mEmulators.keySet()) {
            if (currList[(e)] != null) {
                emuTmp[(e)] = mEmulators[(e)]
            }
        }
        mEmulators = emuTmp;

        // then, update the current state of the emulators
        for (String s : currList.keySet()) {
            String state = currList[(s)]
            if (mEmulators[(s)] == null ||                  // doesn't exist yet
                    mEmulators[(s)] < EMU_STATE_ONLINE) {   // is in INIT or OFFLINE states
                if (state.equals("offline")) {
                    mEmulators[(s)] = EMU_STATE_OFFLINE
                    continue
                }
                if (state.equals("device")) {
                    mEmulators[(s)] = EMU_STATE_ONLINE
                    continue
                }
                throw new ConnectorTargetException("Unknown emulator state.")
            }
        }

        return mEmulators
    }

    /*
     * Address provider implementations for manual and automatic address management
     */

    class ManualAVDAddressProvider implements IAddressProvider{

        private ArrayList<AddressSet> mASListFree;
        private ArrayList<AddressSet> mASListBusy;

        public ManualAVDAddressProvider(Map params) {

            mASListFree = new ArrayList<>()
            mASListBusy = new ArrayList<>()

            try {
                for (Map aset : params.manual) {
                    String avaddr = aset["avaddr"]
                    String araddr = aset["araddr"]
                    String cvaddr = aset["cvaddr"]
                    String craddr = aset["craddr"]
                    mASListFree.add(new AddressSet(avaddr,araddr,cvaddr,craddr))
                }
            } catch (Exception e) {
                throw new ConnectorTargetException("Unable to parse netprofile parameters.\n" +
                        e.getMessage())
            }
        }

        @Override
        AddressSet claimAddressSet() throws ConnectorTargetException {
            if (mASListFree.size() > 0) {
                AddressSet addressSet = mASListFree.remove(0)
                mASListBusy.add(addressSet)
                return addressSet
            }
            return null
        }

        @Override
        void releaseAddressSet(AddressSet addressSet) throws ConnectorTargetException {
            if (mASListBusy.contains(addressSet)) {
                mASListBusy.remove(addressSet)
                mASListFree.add(addressSet)
                return
            }
            throw new ConnectorTargetException("Address set cannot be released.")
        }
    }

    class AutoAVDAddressProvider implements IAddressProvider{

        public AutoAVDAddressProvider(Map params) {
        }

        @Override
        AddressSet claimAddressSet() throws ConnectorTargetException {
            throw new ConnectorTargetException("Auto AVD address provider not implemented.")
        }

        @Override
        void releaseAddressSet(AddressSet addressSet) throws ConnectorTargetException {
            throw new ConnectorTargetException("Auto AVD address provider not implemented.")
        }
    }

    /*
     * Internal helper methods
     */

    private static int getAddrPort(String addr) {
        int port;
        if (addr == null) {
            return -1;
        }
        String[] split = addr.tokenize(":") // expected address "<ip>-<port>"
        if (split == null || split.length != 2) {
            return -1
        }
        try {
            port = split[1].toInteger();
        } catch(Exception e) {
            return -1
        }
        return port;
    }
}
