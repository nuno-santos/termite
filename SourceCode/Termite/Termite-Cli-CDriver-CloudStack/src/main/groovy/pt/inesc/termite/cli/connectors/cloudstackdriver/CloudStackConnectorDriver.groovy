package pt.inesc.termite.cli.connectors.cloudstackdriver

import pt.inesc.termite.cli.AddressSet
import pt.inesc.termite.cli.ConfigManager
import pt.inesc.termite.cli.Connector
import pt.inesc.termite.cli.ConnectorDriver
import pt.inesc.termite.cli.IAddressProvider
import pt.inesc.termite.cli.exceptions.ConnectorTargetException


class CloudStackConnectorDriver extends ConnectorDriver {

    /* <eid, <vmid,asldkjasldkaljs>> */
    private HashMap<String,HashMap<String,String>> emulators = new HashMap<String,HashMap<String,String>>()
    /* <vmid, ...> */
    private HashMap<String,AddressSet> mVirtAddresses = new HashMap<String,AddressSet>()
    private IAddressProvider mAddressProvider

    private String mCloudmonkeyPath
    private String mCloudmonkeyConfig
    private String mZone
    private String mNetwork
    private String mAndroidTemplate
    private String mAndroidService
    private String mAndroidPassword
    private String mDebianTemplate
    private String mDebianService
    private String mDebianPassword

    private String mCloudmonkeyPrefix
    private String mSSHOptions

    CloudStackConnectorDriver(ConfigManager configManager, Connector connector, Map config) {
        super(configManager, connector, config)

        String undefined = "Must define config parameter "

        if(config == null) {
            throw new ConnectorTargetException("Must provide configuration parameters.")
        }
        if(config['cloudmonkey_path'] == null) {
            throw new ConnectorTargetException(undefined + 'cloudmonkey_path.')
        }
        if(config['cloudmonkey_config'] == null) {
            throw new ConnectorTargetException(undefined + 'cloudmonkey_config.')
        }
        if(config['zone'] == null) {
            throw new ConnectorTargetException(undefined + 'zone.')
        }
        if(config['network'] == null) {
            throw new ConnectorTargetException(undefined + 'network.')
        }
        if(config['template_android'] == null) {
            throw new ConnectorTargetException(undefined + 'template_android.')
        }
        if(config['service_android'] == null) {
            throw new ConnectorTargetException(undefined + 'service_android.')
        }
        if(config['password_android'] == null) {
            throw new ConnectorTargetException(undefined + 'password_android.')
        }
        if(config['template_debian'] == null) {
            throw new ConnectorTargetException(undefined + 'template_debian.')
        }
        if(config['service_debian'] == null) {
            throw new ConnectorTargetException(undefined + 'service_debian.')
        }
        if(config['password_debian'] == null) {
            throw new ConnectorTargetException(undefined + 'password_debian.')
        }

        mCloudmonkeyPath = config['cloudmonkey_path']
        mCloudmonkeyConfig = config['cloudmonkey_config']
        mZone = config['zone']
        mNetwork = config['network']
        mAndroidTemplate = config['template_android']
        mAndroidService = config['service_android']
        mAndroidPassword = config['password_android']
        mDebianTemplate = config['template_debian']
        mDebianService = config['service_debian']
        mDebianPassword = config['password_debian']

        mCloudmonkeyPrefix = """${mConfigManager.getTermitePath()}/${mCloudmonkeyPath} -c ${mCloudmonkeyConfig} """
        mSSHOptions = " -o StrictHostKeyChecking=no "
    }

    /*
     * Connector driver methods
     */

    @Override
    public void check() {

        println "Checking CloudStack connector driver..."

        String notFound = " could not be reached."

        // Check for sshpass
        try{ "sshpass -h".execute() }
        catch(Exception ignored) {
            throw new ConnectorTargetException("Error: sshpass" + notFound)
        }

        // Check for python
        try{ "/usr/bin/python2 -h".execute() }
        catch(Exception ignored) {
            throw new ConnectorTargetException("Error: python" + notFound +
            " Expected location: /usr/bin/python2")
        }

        // Check for required python modules
        String toolPath = "${mConfigManager.getTermitePath()}/drivers/connectors/cloudstack"
        String[] pyModules = [	"requests", "pygments", "argcomplete", "prettytable"]
        for(String module : pyModules) {
            Process p  = """${toolPath}/import.py ${module}""".execute()
            p.waitFor()
            if(p.exitValue()) {
                throw new ConnectorTargetException("Error: could not load python module:" +
                        "${module}")
            }
        }

        // Check for cloudstack access
        Process p = """${mCloudmonkeyPrefix} sync""".execute()
        p.waitFor()
        String err = p.err.text
        if(err.contains("Error")) {
            throw new ConnectorTargetException("Error: could not run cloudmonkey: " + err)
        }
    }

    @Override
    void deployEmulator() {
        // TODO - if startvm command is not available, startvm=true...
        Process p = """${mCloudmonkeyPrefix}
		   | deploy virtualmachine
		   | templateid=${mAndroidTemplate}
		   | zoneid=${mZone}
		   | serviceofferingid=${mAndroidService}
		   | networkids=${mNetwork}""".stripMargin().execute()
        p.waitFor()
        String output = p.in.text
        if(output.contains("Error")) {
            throw new ConnectorTargetException(output)
        }
    }

    @Override
    void killEmulator(String seid) {
        fetchEmulators()
        HashMap<String,String> emulator = getEmulator(seid)
        """${mCloudmonkeyPrefix} destroy virtualmachine
		   | id=${emulator["vmid"]}""".stripMargin().execute().waitFor()
    }

    @Override
    void stopEmulator(String seid) {
        fetchEmulators()
        HashMap<String,String> emulator = getEmulator(seid)
        """${mCloudmonkeyPrefix} stop virtualmachine
		   | id=${emulator["vmid"]}""".stripMargin().execute().waitFor()
    }

    @Override
    void startEmulator(String seid) {
        fetchEmulators()
        HashMap<String,String> emulator = getEmulator(seid)
        """${mCloudmonkeyPrefix} start virtualmachine
		   | id=${emulator["vmid"]}""".stripMargin().execute().waitFor()
    }

    @Override
    void initializeAddressProvider(Map params) {

        if (params == null) {
            throw new ConnectorTargetException("Addresses missing.")
        }

        mVirtAddresses.clear()
        mAddressProvider = new CloudStackAddressProvider(params)
    }

    @Override
    AddressSet assignAddressSet(String seid) {
        fetchEmulators()
        HashMap<String,String> emulator = getEmulator(seid)

        if (mAddressProvider == null) {
            throw new ConnectorTargetException("Address provider was not initialized.")
        }

        if (mVirtAddresses.containsKey(emulator['vmid'])) {
            throw new ConnectorTargetException("Addresses already assigned to '${seid}'.")
        }
        AddressSet addr = mAddressProvider.claimAddressSet()
        // FIXME - this is a hack! Maybe the address provider interface should be adapted?
        addr.mARAddr = emulator['ip'] + ":" + addr.mARAddr.split(":")[1]
        addr.mCRAddr = emulator['ip'] + ":" + addr.mCRAddr.split(":")[1]
        mVirtAddresses[emulator['vmid']] = addr

        return addr
    }

    @Override
    void unassignAddressSet(String seid) {
        fetchEmulators()
        HashMap<String,String> emulator = getEmulator(seid)

        if (mAddressProvider == null) {
            throw new ConnectorTargetException("Address provider was not initialized.")
        }

        if (!mVirtAddresses.containsKey(emulator['vmid'])) {
            throw new ConnectorTargetException("No addresses assigned to '${seid}'.")
        }

        mAddressProvider.releaseAddressSet(mVirtAddresses[emulator['vmid']])
        mVirtAddresses.remove(emulator['vmid'])
    }

    @Override
    void finalizeAddressProvider() {
        mVirtAddresses.clear()
        mAddressProvider = null
    }

    @Override
    void installApp(String seid, String apkPath) {
        fetchEmulators()
        HashMap<String,String> emulator = getEmulator(seid)

        if(apkPath == null) {
            throw new ConnectorTargetException("APK path is invalid.")
        }

        File apk = new File(apkPath)

        if(!apk.isFile()) {
            throw new ConnectorTargetException("APK file not found.")
        }

        String toolPath = "${mConfigManager.getTermitePath()}/drivers/connectors/cloudstack"
        Process p = """${toolPath}/installApk.sh ${mAndroidPassword} ${emulator["ip"]} ${apkPath} ${apk.getName()}""".execute()
        p.waitFor()
        println p.in.text
        println p.err.text
    }

    @Override
    void runApp(String seid, String appId, String activity) {
        fetchEmulators()
        HashMap<String,String> emulator = getEmulator(seid)

        if(appId == null) {
            throw new ConnectorTargetException("Application id '${appId}' not valid.")
        }
        if(activity == null) {
            throw new ConnectorTargetException("Activity id '${activity}' not valid.")
        }
        String toolPath = "${mConfigManager.getTermitePath()}/drivers/connectors/cloudstack"
        Process p1 = """${toolPath}/runApk.sh ${mAndroidPassword} ${emulator["ip"]} ${appId}/${activity}""".execute()
        p1.waitFor()
        println p1.in.text
        println p1.err.text
    }

    @Override
    Map<String,Integer> getEmulators() {
   	    fetchEmulators()
        HashMap<String,Integer> ret = new HashMap<String, Integer>()
        emulators.each ( { k,v ->
            if(v['state'].equals("Stopped")) {
                ret[k] = EMU_STATE_OFFLINE
            }
            else if (v['state'].equals("Running") && mVirtAddresses.containsKey(v['vmid'])) {
                ret[k] = EMU_STATE_NETOK
            }
            else if (v['state'].equals("Running")) {
                ret[k] = EMU_STATE_ONLINE
            }
            else {
                // TODO - when emulator is being destroyed, this error is thrown.
                throw new ConnectorTargetException("Unable to resolve emulator state (${k})")
            }
        } )
        return ret
    }

    /*
     * Address provider implementation
     */

    private class CloudStackAddressProvider implements IAddressProvider {

        /* <avip,ias> */
        HashMap<String, InternalAddressSet> mAddrPool = new HashMap<String, InternalAddressSet>()

        public CloudStackAddressProvider(Map params) {
            try {
                String cidr = params["avnet"]
                String avport = params["avport"]
                String arip = "?.?.?.?"
                String arport = params["arport"]
                String cvip = params["cvip"]
                String cvport = params["cvport"]
                String crip = "?.?.?.?"
                String crport = params["crport"]
                for(String avip : getIPs(cidr)) {
                    mAddrPool[avip] = new InternalAddressSet(
                            avip, avport, arip, arport, cvip, cvport, crip, crport)
                }
            } catch (Exception e) {
                throw new ConnectorTargetException("Unable to parse netprofile parameters.\n" +
                        e.getMessage())
            }
        }

        private class InternalAddressSet {
            private String mAVIP
            private String mAVPort
            private String mARIP
            private String mARPort
            private String mCVIP
            private String mCVPort
            private String mCRIP
            private String mCRPort
            private boolean mUsed

            public InternalAddressSet(String avip, String avport, String arip, String arport,
                                      String cvip, String cvport, String crip, String crport) {
                mAVIP = avip
                mAVPort = avport
                mARIP = arip
                mARPort = arport
                mCVIP = cvip
                mCVPort = cvport
                mCRIP = crip
                mCRPort = crport
                mUsed = false
            }

            public AddressSet getAddressSet() {
                return new AddressSet(
                        mAVIP+":"+mAVPort, mARIP+":"+mARPort,
                        mCVIP+":"+mCVPort, mCRIP+":"+mCRPort)
            }
        }

        private List<String> getIPs(String cidr) {
            List<String> list = new LinkedList<String>()
            String toolPath = "${mConfigManager.getTermitePath()}/drivers/connectors/cloudstack"
            Process p  = """${toolPath}/cidr-to-ip.sh ${cidr}""".execute()
            p.waitFor()
            p.text.eachLine { list.add(it) }
            return list
        }

        @Override
        AddressSet claimAddressSet() throws ConnectorTargetException {
            Iterator it = mAddrPool.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, InternalAddressSet> pair = it.next();
                if(!pair.getValue().mUsed) {
                    pair.getValue().mUsed = true
                    return pair.getValue().getAddressSet()
                }
            }
            throw new ConnectorTargetException("There are no available address sets.")
        }

        @Override
        void releaseAddressSet(AddressSet addressSet) throws ConnectorTargetException {
            String avip = addressSet.mAVAddr.split(":")[0]
            if(mAddrPool.containsKey(avip)) {
                mAddrPool[avip].mUsed = false
            } else {
                throw new ConnectorTargetException("Address set cannot be released.")
            }
        }
    }

    /*
     * Internal helper methods
     */

    private HashMap<String,String> getEmulator(String seid) {
        if(emulators.containsKey(seid)) {
            return emulators[seid]
        } else {
            throw new ConnectorTargetException("No emulator with id '${seid}'")
        }
    }

    private void fetchEmulators() {
        emulators.clear()
        Process p = """${mCloudmonkeyPrefix} list virtualmachines""".execute()
        p.waitFor()
        List<HashMap<String, String>> elist = new ArrayList<HashMap<String, String>>()
        String vmid = ""

        // process output from cloudmonkey and put emulators in a list
        for(String line : p.in.text.split("\n")) {
            String[] tokens = line.split(" = ")
            if(tokens[0].equals("id") && vmid.equals("")) {
                elist.add(new HashMap<String, String>())
                elist.last()["vmid"] = vmid = tokens[1]
            } else if(tokens[0].equals("zoneid")) {
                vmid = ""
            } else if(tokens[0].equals("ipaddress")) {
                elist.last()["ip"] = tokens[1]
            } else if(tokens[0].equals("templatename")) {
                elist.last()["template"] = tokens[1]
            } else if(tokens[0].equals("state")) {
                elist.last()["state"] = tokens[1]
            }
        }

        // process list and organize emulators into the map (emulators)
        for(HashMap<String, String> emulator : elist) {
            emulators["emulator-" + emulator["ip"].split("\\.")[3]] = emulator
        }

    }
}
