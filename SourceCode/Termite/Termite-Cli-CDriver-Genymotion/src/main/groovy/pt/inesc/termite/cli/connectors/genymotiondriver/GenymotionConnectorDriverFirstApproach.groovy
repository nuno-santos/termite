package pt.inesc.termite.cli.connectors.genymotiondriver

import pt.inesc.termite.cli.AddressSet
import pt.inesc.termite.cli.ConfigManager
import pt.inesc.termite.cli.Connector
import pt.inesc.termite.cli.ConnectorDriver
import pt.inesc.termite.cli.exceptions.ConnectorTargetException


/*

    Network profile configuration was as follows:

    "config" : {
        "avnet" : "192.168.0.0/24",
        "avmax" : "3",
        "aport" : "10001",
        "cport" : "9001"
    }

 */

public class GenymotionConnectorDriverFirstApproach extends ConnectorDriver {

    private String mGMPath
    private String mVmiPrefix
    private String mSdk

    private HashMap<String,Integer> mEmulators
    private HashMap<String,AddressSet> mAssignedEmulators

    private ArrayList<String> mIpListFree
    private ArrayList<String> mIpListBusy

    private String mAVNet
    private int mAVMax
    private int mAPort
    private int mCPort
    private boolean mAPInitialized


    public GenymotionConnectorDriverFirstApproach(ConfigManager configManager, Connector connector, Map config) {
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
        mGMPath = config['path']
        mVmiPrefix = config['vmiprefix']
        mSdk = config['sdk']
        mEmulators = new HashMap<>()
        mAssignedEmulators = new HashMap<>()
        mAPInitialized = false

        /*
        println "Initializing the Genymotion Connector Driver..."
        println "sdk: " + mSdk
        println "path: " + mGMPath
        println "vmiprfix: " + mVmiPrefix
        */
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

        int cloneNum = 0;

        def exec = ["${mGMPath}/player", "--vm-name", "${mVmiPrefix} - Clone ${cloneNum}"]

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

        try {
            mAPort = Integer.parseInt((String)params["aport"])
            println "mAPort:" + mAPort

            mCPort = Integer.parseInt((String)params["cport"])
            println "mCPort:" + mCPort

            mAVNet = params["avnet"]
            mAVMax = Integer.parseInt((String)params["avmax"])
            IPCalc ipCalc = new IPCalc(mAVNet)
            mIpListFree = ipCalc.getHostAddressRange(mAVMax)
            mIpListBusy = new ArrayList<>()

        } catch (Exception e) {
            throw new ConnectorTargetException("Unable to parse netprofile parameters.\n" +
                    e.getMessage())
        }

        if (mAPort < 0 || mCPort < 0) {
            throw new ConnectorTargetException("Invalid port numbers.\n")
        }

        mAPInitialized = true

        println "ADDRESSES:"
        println mIpListFree.toArray().toString();
    }

    @Override
    AddressSet assignAddressSet(String eid) throws ConnectorTargetException {

        if (!mAPInitialized) {
            throw new ConnectorTargetException("Address provider was not initialized.")
        }

        if (eid == null) {
            throw new ConnectorTargetException("Emulator id missing.")
        }

        if (mAssignedEmulators[(eid)] != null) {
            throw new ConnectorTargetException("Addresses already assigned to '${eid}'.")
        }

        /*
         * obtain the emulator IP address
         */

        String[] split = eid.tokenize(":") // expected eid in the form of "<ip>:<port>"
        if (split == null || split.length != 2) {
            throw new ConnectorTargetException("Emulator id is invalid.")
        }
        String ipReal = split[0];

        /*
         * obtain an available virtual IP address
         */

        if (mIpListFree.size() <= 0) {
            throw new ConnectorTargetException("No more virtual IP addresses are available.")
        }
        String ipVirtual = mIpListFree.remove(0)
        mIpListBusy.add(ipVirtual)

        /*
         * build new address set: no need to perform port forwarding
         */

        String avaddr = ipVirtual + ":" + mAPort
        String araddr = ipReal + ":" + mAPort
        String cvaddr = ipVirtual + ":" + mCPort
        String craddr = ipReal + ":" + mCPort
        AddressSet addr = new AddressSet(avaddr,araddr,cvaddr,craddr)
        mAssignedEmulators[(eid)] = addr
        mEmulators[(eid)] = EMU_STATE_NETOK

        return addr;
    }

    @Override
    void unassignAddressSet(String eid) throws ConnectorTargetException {
        throw new ConnectorTargetException("Operation not supported.")
    }

    @Override
    void finalizeAddressProvider() throws ConnectorTargetException {
        throw new ConnectorTargetException("Operation not supported.")
    }

    @Override
    void installApp(String eid, String apkPath) throws ConnectorTargetException {
        throw new ConnectorTargetException("Operation not supported.")
    }

    @Override
    void runApp(String eid, String appId, String activity) throws ConnectorTargetException {
        throw new ConnectorTargetException("Operation not supported.")
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
     * Helper class for CIDR to IP translator
     */

    class IPCalc {

        private int baseIPnumeric;
        private int netmaskNumeric;

        /**
         * Constructor
         *
         *@param IPinCIDRFormat IP in CIDR format e.g. 192.168.1.0/24
         */
        public IPCalc(String IPinCIDRFormat) throws NumberFormatException {

            String[] st = IPinCIDRFormat.split("\\/");
            if (st.length != 2) {
                throw new NumberFormatException("Invalid CIDR format '"
                        + IPinCIDRFormat + "', should be: xx.xx.xx.xx/xx");
            }
            String symbolicIP = st[0];
            String symbolicCIDR = st[1];

            Integer numericCIDR = new Integer(symbolicCIDR);
            if (numericCIDR > 32) {
                throw new NumberFormatException("CIDR can not be greater than 32");
            }

            //Get IP
            st = symbolicIP.split("\\.");
            if (st.length != 4) {
                throw new NumberFormatException("Invalid IP address: " + symbolicIP);
            }

            int i = 24;
            baseIPnumeric = 0;

            for (int n = 0; n < st.length; n++) {
                int value = Integer.parseInt(st[n]);
                if (value != (value & 0xff)) {
                    throw new NumberFormatException("Invalid IP address: " + symbolicIP);
                }
                baseIPnumeric += value << i;
                i -= 8;
            }

            //Get netmask
            if (numericCIDR < 8)
                throw new NumberFormatException("Netmask CIDR can not be less than 8");
            netmaskNumeric = 0xffffffff;
            netmaskNumeric = netmaskNumeric << (32 - numericCIDR);
        }

        /**
         * Get the IP in symbolic form, i.e. xxx.xxx.xxx.xxx
         *
         *@return The reult of convertNumericIpToSymbolic() when passed baseIPnumeric
         */
        public String getIP() {
            return convertNumericIpToSymbolic(baseIPnumeric);
        }

        /**
         * Converts Numeric version of IP to Symbolic, i.e. xxx.xxx.xxx.xxx
         *
         *@param ip IP Address in numeric form
         *@return the result of sb.toString(), the symbolic IP as a String
         */
        private String convertNumericIpToSymbolic(Integer ip) {
            StringBuffer sb = new StringBuffer(15);
            for (int shift = 24; shift > 0; shift -= 8) {
                // process 3 bytes, from high order byte down.
                sb.append(Integer.toString((ip >>> shift) & 0xff));
                sb.append('.');
            }
            sb.append(Integer.toString(ip & 0xff));
            return sb.toString();
        }

        /**
         * Get the net mask in symbolic form, i.e. xxx.xxx.xxx.xxx
         *
         *@return the result of sb.toString(), the symbolic netmask as a String
         */
        public String getNetmask() {
            StringBuffer sb = new StringBuffer(15);
            for (int shift = 24; shift > 0; shift -= 8) {
                // process 3 bytes, from high order byte down.
                sb.append(Integer.toString((netmaskNumeric >>> shift) & 0xff));
                sb.append('.');
            }
            sb.append(Integer.toString(netmaskNumeric & 0xff));
            return sb.toString();
        }

        /**
         * Returns a range of valid addresses
         *
         */
        public ArrayList<String> getHostAddressRange(int max) {

            int numberOfBits;
            for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
                if ((netmaskNumeric << numberOfBits) == 0)
                    break;
            }
            Integer numberOfIPs = 0;
            for (int n = 0; n < (32 - numberOfBits); n++) {
                numberOfIPs = numberOfIPs << 1;
                numberOfIPs = numberOfIPs | 0x01;
            }

            Integer baseIP = baseIPnumeric & netmaskNumeric;

            ArrayList<String> list = new ArrayList<>();
            for (int i = 1; i <= max && i < numberOfIPs - 1; i++) {
                list.add(convertNumericIpToSymbolic(baseIP + i))
            }

            //String firstIP = convertNumericIpToSymbolic(baseIP + 1);
            //String lastIP = convertNumericIpToSymbolic(baseIP + numberOfIPs - 1);

            return list
        }

        /**
         * Returns number of hosts available in given range
         *
         *@return number of hosts
         */
        public Long getNumberOfHosts() {
            int numberOfBits;
            for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
                if ((netmaskNumeric << numberOfBits) == 0)
                    break;
            }
            Double x = Math.pow(2, (32 - numberOfBits));
            if (x == -1) {
                x = 1D;
            }
            return x.longValue();
        }

        /**
         *Calculates wildcard mask
         *
         *@return the result of sb.toString(), in this case the wilcard mask in symbolic form
         */
        public String getWildcardMask() {
            Integer wildcardMask = netmaskNumeric ^ 0xffffffff;
            StringBuffer sb = new StringBuffer(15);
            for (int shift = 24; shift > 0; shift -= 8) {
                // process 3 bytes, from high order byte down.
                sb.append(Integer.toString((wildcardMask >>> shift) & 0xff));
                sb.append('.');
            }
            sb.append(Integer.toString(wildcardMask & 0xff));
            return sb.toString();

        }

        /**
         * Calculates the broadcast address
         *
         *@return the broadcast ip address as a String
         */
        public String getBroadcastAddress() {
            if (netmaskNumeric == 0xffffffff) {
                return "0.0.0.0";
            }
            int numberOfBits;
            for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
                if ((netmaskNumeric << numberOfBits) == 0)
                    break;
            }
            Integer numberOfIPs = 0;
            for (int n = 0; n < (32 - numberOfBits); n++) {
                numberOfIPs = numberOfIPs << 1;
                numberOfIPs = numberOfIPs | 0x01;
            }
            Integer baseIP = baseIPnumeric & netmaskNumeric;
            Integer ourIP = baseIP + numberOfIPs;
            String ip = convertNumericIpToSymbolic(ourIP);
            return ip;
        }
    }
}
