package pt.inesc.termite.cli

import groovy.json.JsonSlurper
import pt.inesc.termite.cli.exceptions.ConfigErrorException


import java.lang.reflect.InvocationTargetException

public class ConfigManager {

    private Map<String,Application> mApps = new HashMap<String,Application>();
    private Map<String,Connector> mConnectors = new HashMap<String,Connector>();
    private Map<String,Backend> mBackends = new HashMap<String,Backend>();
    private Map<String,Experiment> mExperiments = new HashMap<String,Experiment>();
    private Map<String,NetProfile> mNetProfiles = new HashMap<String,NetProfile>();

    private String mTermitePath, mTermitePlatform, mConfRPath, mAppConfFile,
            mConnConfFile, mBackConfFile, mExpConfFile, mNetConfFile, mInitScript;

    public static final String PLATFORM_MAC = "mac";
    public static final String PLATFORM_LINUX = "linux";
    public static final String PLATFORM_WINDOWS = "windows";

    public ConfigManager() {
    }

    public Map<String,Application> getApplications() {
        return mApps;
    }

    public Map<String,Connector> getConnectors() {
        return mConnectors;
    }

    public Map<String,Backend> getBackends() {
        return mBackends;
    }

    public Map<String,Experiment> getExperiments() {
        return mExperiments;
    }

    public Map<String,NetProfile> getNetProfiles() {
        return mNetProfiles;
    }

    public String getTermitePath() {
        return mTermitePath;
    }

    public String getTermitePlatform() {
        return mTermitePlatform;
    }

    public String getInitScript() {
        return "${mTermitePath}/${mConfRPath}/init/${mInitScript}";
    }

    public void loadConfiguration() throws ConfigErrorException {

        mTermitePath = System.getenv()['TERMITE_CLI_PATH']
        if (mTermitePath == null) {
            throw new ConfigErrorException("Error: environment variable TERMITE_CLI_PATH undefined.")
        }

        mTermitePlatform = System.getenv()['TERMITE_PLATFORM']
        if (mTermitePlatform == null) {
            throw new ConfigErrorException("Error: environment variable TERMITE_PLATFORM undefined.")
        }

        if (!mTermitePlatform.equals(PLATFORM_MAC) &&
                !mTermitePlatform.equals(PLATFORM_LINUX) &&
                !mTermitePlatform.equals(PLATFORM_WINDOWS)) {
            throw new ConfigErrorException("Error: platform '${mTermitePlatform}' invalid;" +
                " pick one of: '${PLATFORM_MAC}', '${PLATFORM_LINUX}', or '${PLATFORM_WINDOWS}'")
        }

        mConfRPath = "etc"

        loadGlobal()
        loadApplications()
        loadConnectors()
        loadBackends()
        loadNetProfiles()
        loadExperiments()
    }

    public void checkConnectorDrivers() {
        if (mBackends == null) {
            return
        }
        HashMap<String,ConnectorDriver> cds = new HashMap<>();
        for (String bid in mBackends.keySet()) {
            Backend b = mBackends[(bid)]
            cds[(b.getConnector())] = b.getConnectorTarget()
        }
        for (ConnectorDriver cd in cds.values()) {
            cd.check()
        }
    }

    public void printConfiguration() {
        println "Applications"
        mApps.each { k, v -> print "${k} : " ; v.print() ; println ""}
        println ""
        println "Connectors"
        mConnectors.each { k, v -> print "${k} : " ; v.print() ; println ""}
        println ""
        println "Backends"
        mBackends.each { k, v -> print "${k} : " ; v.print() ; println ""}
        println ""
        println "NetProfiles"
        mNetProfiles.each { k, v -> print "${k} : " ; v.print() ; println ""}
        println ""
        println "Experiments"
        mExperiments.each { k, v -> print "${k} : " ; v.print() ; println ""}
    }

    protected void loadGlobal() {

        Map inputJSON;

        try {
            def inputFile = new File("${mTermitePath}/${mConfRPath}/termite.conf")
            inputJSON = (Map) new JsonSlurper().parseText(inputFile.text)
        } catch (Exception e) {
            throw new ConfigErrorException("Error: cannot parse termite.conf file.\n"
                    + e.getMessage())
        }

        if (inputJSON.global != null) {
            mAppConfFile = inputJSON.global['applications'];
            mBackConfFile = inputJSON.global['backends'];
            mConnConfFile = inputJSON.global['connectors'];
            mExpConfFile = inputJSON.global['experiments'];
            mNetConfFile = inputJSON.global['netprofiles'];
            mInitScript = inputJSON.global['init'];
        }
    }

    protected void loadApplications() {

        Map inputJSON;

        try {
            def inputFile = new File("${mTermitePath}/${mConfRPath}/platform/" +
                    "${mTermitePlatform}/${mAppConfFile}")
            inputJSON = (Map) new JsonSlurper().parseText(inputFile.text)
        } catch (Exception e) {
            throw new ConfigErrorException("Error: cannot parse ${mAppConfFile} file.\n"
                    + e.getMessage())
        }

        if (inputJSON.applications != null) {

            for (Map application : inputJSON.applications) {
                Application a = Application.fromMap(application)
                if (a == null) {
                    throw new ConfigErrorException("Error: unable to unmarshall application object.")
                }
                String id = a.getId()
                if (id == null || id.isEmpty() || mApps.get(id) != null) {
                    throw new ConfigErrorException("Error: invalid application id.")
                }
                mApps[(id)] = a
            }
        }
    }

    protected void loadConnectors() {

        Map inputJSON;

        try {
            def inputFile = new File("${mTermitePath}/${mConfRPath}/${mConnConfFile}")
            inputJSON = (Map) new JsonSlurper().parseText(inputFile.text)
        } catch (Exception e) {
            throw new ConfigErrorException("Error: cannot parse ${mConnConfFile} file.\n"
                    + e.getMessage())
        }

        if (inputJSON.connectors != null) {

            for (Map connector : inputJSON.connectors) {
                Connector c = Connector.fromMap(connector)
                if (c == null) {
                    throw new ConfigErrorException("Error: unable to unmarshall connector object.")
                }
                String id = c.getId()
                if (id == null || id.isEmpty() || mConnectors.get(id) != null) {
                    throw new ConfigErrorException("Error: invalid connector id.")
                }
                mConnectors[(id)] = c
            }
        }
    }

    protected void loadBackends() {

        Map inputJSON;

        try {
            def inputFile = new File("${mTermitePath}/${mConfRPath}/platform/" +
                    "${mTermitePlatform}/${mBackConfFile}")
            inputJSON = (Map) new JsonSlurper().parseText(inputFile.text)
        } catch (Exception e) {
            throw new ConfigErrorException("Error: cannot parse ${mBackConfFile} file.\n" +
                    e.getMessage())
        }

        if (inputJSON.backends != null) {

            for (Map backend : inputJSON.backends) {
                Backend b = Backend.fromMap(backend)
                if (b == null) {
                    throw new ConfigErrorException("Error: unable to unmarshall backend object.")
                }
                String id = b.getId()
                if (id == null || id.isEmpty() || mBackends.get(id) != null) {
                    throw new ConfigErrorException("Error: invalid id.")
                }

                Connector c = mConnectors.get(b.getConnector())
                if (c == null) {
                    throw new ConfigErrorException("Error: connector '${b.getConnector()}' not declared.")
                }

                ConnectorDriver t;
                try {

                    File f = new File("${mTermitePath}/drivers/connectors/${c.getId()}/${c.getJar()}");

                    URL url = new URL("jar","","file:" + f.getAbsolutePath() + "!/")
                    URLClassLoader urlCl = new URLClassLoader([ url ] as URL[],
                            ClassLoader.getSystemClassLoader());
                    Class cl = urlCl.loadClass(c.getCClass());
                    t = (ConnectorDriver) cl.getDeclaredConstructor(ConfigManager.class,
                            Connector.class, Map.class).newInstance(this, c, b.getConfig())

                    //
                    // old way without jar
                    //
                    // t = (ConnectorDriver) Class.forName(c.getCClass()).
                    //        getDeclaredConstructor(Connector.class, Map.class).
                    //        newInstance(c, b.getConfig())

                } catch (ClassNotFoundException e) {
                    throw new ConfigErrorException("Error: Could not instantiate connector target ${c.getCClass()}.")
                } catch (InvocationTargetException e) {
                    throw new ConfigErrorException(e.getCause().getMessage())
                }
                b.setConnectorTarget(t)

                mBackends[(id)] = b
            }
        }
    }

    protected void loadNetProfiles() {

        Map inputJSON;

        try {
            def inputFile = new File("${mTermitePath}/${mConfRPath}/${mNetConfFile}")
            inputJSON = (Map) new JsonSlurper().parseText(inputFile.text)
        } catch (Exception e) {
            throw new ConfigErrorException("Error: cannot parse ${mNetConfFile} file.\n" +
                    + e.getMessage())
        }

        if (inputJSON.netprofiles != null) {

            for (Map netprofile : inputJSON.netprofiles) {
                NetProfile np = NetProfile.fromMap(netprofile)
                if (np == null) {
                    throw new ConfigErrorException("Error: unable to unmarshall netprofile object.")
                }
                String id = np.getId()
                if (id == null || id.isEmpty() || mNetProfiles.get(id) != null) {
                    throw new ConfigErrorException("Error: invalid id.")
                }

                Connector c = mConnectors.get(np.getConnector())
                if (c == null) {
                    throw new ConfigErrorException("Error: connector '${np.getConnector()}' not declared.")
                }

                mNetProfiles[(id)] = np
            }
        }
    }

    protected void loadExperiments() {

        Map inputJSON;
        try {
            def inputFile = new File("${mTermitePath}/${mConfRPath}/${mExpConfFile}")
            inputJSON = (Map) new JsonSlurper().parseText(inputFile.text)
        } catch (Exception e) {
            throw new ConfigErrorException("Error: cannot parse ${mExpConfFile} file.\n" +
                    + e.getMessage())
        }

        if (inputJSON.experiments != null) {

            for (Map experiment : inputJSON.experiments) {
                Experiment exp = Experiment.fromMap(experiment)
                if (exp == null) {
                    throw new ConfigErrorException("Error: unable to unmarshall experiment object.")
                }
                String id = exp.getId()
                if (id == null || id.isEmpty() || mExperiments.get(id) != null) {
                    throw new ConfigErrorException("Error: invalid experiment id.")
                }

                if (mApps.get(exp.getApplication()) == null) {
                    throw new ConfigErrorException("Error: application '${exp.getApplication()}' not declared.")
                }

                if (mBackends.get(exp.getBackend()) == null) {
                    throw new ConfigErrorException("Error: backend '${exp.getBackend()}' not declared.")
                }

                if (mNetProfiles.get(exp.getNetProfile()) == null) {
                    throw new ConfigErrorException("Error: netprofile '${exp.getNetProfile()}' not declared.")
                }

                mExperiments[(id)] = exp
            }
        }
    }
}
