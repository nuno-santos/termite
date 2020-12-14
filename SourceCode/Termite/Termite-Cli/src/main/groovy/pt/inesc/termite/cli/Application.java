package pt.inesc.termite.cli;

import java.util.HashMap;
import java.util.Map;

public class Application {

    public static final String TAG_ID = "id";
    public static final String TAG_APPID = "appid";
    public static final String TAG_ACTIVITY = "activity";
    public static final String TAG_APK = "apk";
    public static final String TAG_RPATH = "rpath";

    private String mId;
    private String mAppId;
    private String mActivity;
    private String mApk;
    private String mRPath;

    public Application(String id, String appId, String activity, String apk, String rPath) {
        mId = id;
        mAppId = appId;
        mActivity = activity;
        mApk = apk;
        mRPath = rPath;
    }

    public String getId() {
        return mId;
    }

    public String getAppId() {
        return mAppId;
    }

    public String getActivity() {
        return mActivity;
    }

    public String getApk() {
        return mApk;
    }

    public String getRPath() {
        return mRPath;
    }

    public static Application fromMap(Map map) {

        if (map == null) {
            return null;
        }

        try {
            String id = (String) map.get(TAG_ID);
            String appId = (String) map.get(TAG_APPID);
            String activity = (String) map.get(TAG_ACTIVITY);
            String apk = (String) map.get(TAG_APK);
            String rPath = (String) map.get(TAG_RPATH);
            return new Application(id, appId, activity, apk, rPath);

        } catch (Exception e) {
            return null;
        }
    }

    public Map toMap() {
        Map<String,String> map = new HashMap<String,String>();
        map.put(TAG_ID, mId);
        map.put(TAG_APPID, mAppId);
        map.put(TAG_ACTIVITY, mActivity);
        map.put(TAG_APK, mApk);
        map.put(TAG_RPATH, mRPath);
        return map;
    }

    public void print() {
        System.out.println("[" + TAG_ID + ":" + mId +
                ", " + TAG_APPID + ":" + mAppId + ", " + TAG_ACTIVITY + ":" + mActivity +
                ", " + TAG_APK + ":" + mApk + ", " + TAG_RPATH + ":" + mRPath + "]");
    }}
