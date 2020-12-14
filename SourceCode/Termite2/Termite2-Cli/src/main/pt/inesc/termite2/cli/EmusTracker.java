package main.pt.inesc.termite2.cli;

import java.util.*;

public class EmusTracker {

    private HashMap<String,Emulator> mEmuList; //e1 -> Emulator_obj, e2 -> emulator_obj
    private int mIdCounter;
    private ArrayList<String> mIdPool;

    public EmusTracker() {
        mEmuList = new HashMap<>();
        mIdCounter = 1;
        mIdPool = new ArrayList<>();
    }

    public HashMap<String,Emulator> getEmusList() {
        return mEmuList;
    }

    public synchronized void updateEmuList(ArrayList<Emulator> emuList) {

        /*System.out.println("\nemuList: " + emuList.toString());
        System.out.println("current emuList: ");
        for(String id : mEmuList.keySet()){
            System.out.println(id + " -> " + mEmuList.get(id).toString());
        }
        System.out.println("\n ");*/

        // first, remove stale entries
        HashMap<String,Emulator> newList = new HashMap<>();
        for (String id : mEmuList.keySet()) {
            Emulator currentEmu = mEmuList.get(id);
            if(containsEmulator(emuList, currentEmu)){
                newList.put(id, currentEmu);
            }else{
                mIdPool.add(id);
            }
        }
        //System.out.println("2 newList: " + newList.toString());
        // assign ids to the new emulators
        Collection<Emulator> currentEmus = newList.values();
        //System.out.println("3 currentEmus: " + currentEmus.toString());
        for (Emulator newEmu : emuList) {
            if(!containsEmulator(currentEmus, newEmu)){
                String id;
                if (mIdPool.isEmpty()) {
                    id = "e" + mIdCounter;
                    mIdCounter++;
                } else {
                    Collections.sort(mIdPool);
                    id = mIdPool.remove(0);
                }
                newList.put(id,newEmu);
            }
        }
        //System.out.println("final newList: " + newList.toString());
        mEmuList = newList;

        /*System.out.println("new emuList: ");
        for(String id : mEmuList.keySet()){
            System.out.println(id + " -> " + mEmuList.get(id).toString());
        }*/
    }

    public String getEmuId(String name, String ip){
        for (Map.Entry<String, Emulator> emuInstance : mEmuList.entrySet()) {
            if(emuInstance.getValue().getIp().equals(ip) && emuInstance.getValue().getName().equals(name)){
                return emuInstance.getKey();
            }
        }
        return null;
    }

    private boolean containsEmulator( Collection<Emulator> emus, Emulator emu){
        boolean result = false;
        for(Emulator curemu : emus){
            if(emu.equals(curemu)){
                result = true;
            }
        }
        return result;
    }

}
