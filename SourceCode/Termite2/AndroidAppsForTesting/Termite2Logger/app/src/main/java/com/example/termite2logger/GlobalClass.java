package com.example.termite2logger;

import android.app.Application;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


// This class is used to store global data
public class GlobalClass extends Application {

    private int comsId = 0;
    public String mDeviceName = "";
    private ArrayList<String> previousConnections = new ArrayList<>();

    public ArrayList<String> getConnections(){
        return previousConnections;
    }

    public void setPreviousConnections(ArrayList<String> conn){
        previousConnections = conn;
    }

    public void addConnection(String conn){
        previousConnections.add(conn);
    }

    public void removeConnection(String conn){
        previousConnections.remove(conn);
    }

    public void setDeviceName(String name){
        assert name != null;
        mDeviceName = name;
    }

    public int getId(){
        comsId += 1;
        return comsId;
    }

    public long getCurrentTime(){
        return System.currentTimeMillis();
    }
}
