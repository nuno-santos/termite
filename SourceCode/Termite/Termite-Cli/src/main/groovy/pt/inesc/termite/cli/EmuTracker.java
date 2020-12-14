package pt.inesc.termite.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class EmuTracker {

    private HashMap<String,String> mEmuList;
    private int mIdCounter;
    private ArrayList<String> mIdPool;

    public EmuTracker() {
        mEmuList = new HashMap<>();
        mIdCounter = 1;
        mIdPool = new ArrayList<>();
    }

    public HashMap<String,String> getEmuList() {
        return mEmuList;
    }

    public void updateEmuList(ArrayList<String> emuList) {

        // first, remove stale entries
        HashMap<String,String> newList = new HashMap<>();
        for (String id : mEmuList.keySet()) {
            String realName = mEmuList.get(id);
            if (emuList.contains(realName)) {
                newList.put(id, realName);
            } else {
                mIdPool.add(id);
            }
        }

        // assign ids to the new emulators
        Collection<String> realNames = newList.values();
        for (String realName : emuList) {
            if (!realNames.contains(realName)) {
                String id;
                if (mIdPool.isEmpty()) {
                    id = "e" + mIdCounter;
                    mIdCounter++;
                } else {
                    Collections.sort(mIdPool);
                    id = mIdPool.remove(0);
                }
                newList.put(id,realName);
            }
        }

        mEmuList = newList;
    }
}
