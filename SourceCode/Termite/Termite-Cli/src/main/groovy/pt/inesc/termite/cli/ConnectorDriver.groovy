package pt.inesc.termite.cli;

abstract public class ConnectorDriver implements IConnectorDriver {

    protected ConfigManager mConfigManager;
    protected Connector mConnector;
    protected Map mConfig;

    public ConnectorDriver(ConfigManager configManager, Connector connector, Map config) {
        mConfigManager = configManager
        mConnector = connector
        mConfig = config
    }

    protected void beginPrint() {
        print "${Utils.ANSI_BLUE}Connector '${mConnector.getId()}'...\n"
    }

    protected void endPrint() {
        print "${Utils.ANSI_RESET}"
    }
}
