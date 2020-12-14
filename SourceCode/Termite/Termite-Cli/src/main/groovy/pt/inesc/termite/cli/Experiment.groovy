package pt.inesc.termite.cli

import pt.inesc.termite.cli.exceptions.ConfigErrorException;


public class Experiment {

    public static final String TAG_ID = "id";
    public static final String TAG_APPLICATION = "application";
    public static final String TAG_BACKEND = "backend";
    public static final String TAG_NETPROFILE = "netprofile";

    private String mId;
    private String mApplication;
    private String mBackend;
    private String mNetProfile;

    public Experiment(String id, String application, String backend, String netProfile) {
        mId = id;
        mApplication = application;
        mBackend = backend;
        mNetProfile = netProfile;
    }

    public String getId() {
        return mId;
    }

    public String getApplication() {
        return mApplication;
    }

    public String getBackend() {
        return mBackend;
    }

    public String getNetProfile() {
        return mNetProfile;
    }

    public static Experiment fromMap(Map map) {

        if (map == null) {
            return null;
        }

        try {
            String id = (String) map.get(TAG_ID);
            String application = (String) map.get(TAG_APPLICATION);
            String backend = (String) map.get(TAG_BACKEND);
            String netProfile = (String) map.get(TAG_NETPROFILE);
            return new Experiment(id, application, backend, netProfile);

        } catch (Exception e) {
            throw new ConfigErrorException("Error: cannot parse experiment data.\n" +
                    + e.getMessage())
        }
    }

    public Map toMap() {
        Map map = new HashMap();
        map.put(TAG_ID, mId);
        map.put(TAG_APPLICATION, mApplication);
        map.put(TAG_BACKEND, mBackend);
        map.put(TAG_NETPROFILE, mNetProfile);
        return map;
    }

    public void print() {
        System.out.println("[" + TAG_ID + ":" + mId +
                ", " + TAG_APPLICATION + ":" + mApplication +
                ", " + TAG_BACKEND + ":" + mBackend +
                ", " + TAG_NETPROFILE + ":" + mNetProfile + "]");
    }
}
