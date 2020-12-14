package pt.inesc.termite.cli;

public class NetProfile {

    public static final String TAG_ID = "id";
    public static final String TAG_CONNECTOR = "connector";
    public static final String TAG_CONFIG = "config";

    private String mId;
    private String mConnector;
    private Map mConfig;

    public NetProfile(String id, String connector, Map config) {
        mId = id;
        mConnector = connector;
        mConfig = config;
    }

    public String getId() {
        return mId;
    }

    public String getConnector() {
        return mConnector;
    }

    public Map getConfig() {
        return mConfig;
    }

    public static NetProfile fromMap(Map map) {

        if (map == null) {
            return null;
        }

        try {
            String id = (String) map.get(TAG_ID);
            String connector = (String) map.get(TAG_CONNECTOR);
            Map config = (Map) map.get(TAG_CONFIG);
            return new NetProfile(id, connector, config);

        } catch (Exception e) {
            return null;
        }
    }

    public Map toMap() {
        Map map = new HashMap();
        map.put(TAG_ID, mId);
        map.put(TAG_CONNECTOR, mConnector);
        map.put(TAG_CONFIG, mConfig);
        return map;
    }

    public void print() {
        System.out.println("[" + TAG_ID + ":" + mId +
                ", " + TAG_CONNECTOR + ":" + mConnector +
                ", " + TAG_CONFIG + ":" + mConfig.toMapString() + "]");
    }
}
