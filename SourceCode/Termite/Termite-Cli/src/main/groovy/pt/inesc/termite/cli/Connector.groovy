package pt.inesc.termite.cli;

public class Connector {

    public static final String TAG_ID = "id";
    public static final String TAG_JAR = "jar";
    public static final String TAG_CCLASS = "cclass";

    private String mId;
    private String mJar;
    private String mCClass;

    public Connector(String id, String jar, String cClass) {
        mId = id;
        mJar = jar;
        mCClass = cClass;
    }

    public String getId() {
        return mId;
    }

    public String getJar() {
        return mJar;
    }

    public String getCClass() {
        return mCClass;
    }

    public static Connector fromMap(Map map) {

        if (map == null) {
            return null;
        }

        try {
            String id = (String) map.get(TAG_ID);
            String jar = (String) map.get(TAG_JAR);
            String cClass = (String) map.get(TAG_CCLASS);
            return new Connector(id, jar, cClass);

        } catch (Exception e) {
            return null;
        }
    }

    public Map toMap() {
        Map<String,String> map = new HashMap<String,String>();
        map.put(TAG_ID, mId);
        map.put(TAG_JAR, mJar);
        map.put(TAG_CCLASS, mCClass);
        return map;
    }

    public void print() {
        System.out.println("[" + TAG_ID + ":" + mId +
                ", " + TAG_JAR + ":" + mJar +
                ", " + TAG_CCLASS + ":" + mCClass + "]");
    }
}
