package com.example.termite2logger;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketManager;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pManager.PeerListListener;
import pt.inesc.termite.wifidirect.SimWifiP2pManager.GroupInfoListener;

public class MainActivity extends AppCompatActivity implements PeerListListener, GroupInfoListener {

    public static final String TAG = "termite2logger";

    private boolean autoSend = true; // this is used to turn on or off the automatic sending of messages on group changes
    private Button autoButton;
    private Button ImgButton;

    private int dataSize = 1;
    private byte[] dataToSend;

    private GlobalClass globalClass;

    //TextView
    private TextView _textView;
    private ArrayList<String> loggedValues = new ArrayList<>();

    //TermiteApi variables
    private boolean mBound = false; //Indicates wifi-Direct ON or OFF
    public SimWifiP2pManager mManager = null;
    private SimWifiP2pManager.Channel mChannel = null;
    private Messenger mService = null;
    private SimWifiP2pBroadcastReceiver mReceiver;

    //Comunication variables
    private SimWifiP2pSocketServer mSrvSocket = null;
    private SimWifiP2pSocket mCliSocket = null;

    //Used to updated groups showing table or not
    private boolean showGTable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize the UI
        setContentView(R.layout.activity_main);
        guiSetButtonListeners();

        // initialize the WDSim API
        SimWifiP2pSocketManager.Init(getApplicationContext());

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
        mReceiver = new SimWifiP2pBroadcastReceiver(MainActivity.this);
        registerReceiver(mReceiver, filter);

        Intent intent = new Intent(this, SimWifiP2pService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        wifiOn();
        globalClass = (GlobalClass) getApplicationContext();

        Log.d("DeviceName", "Device Name is " + globalClass.mDeviceName);

        dataToSend = createByteArray(1);

        autoButton = (Button) findViewById(R.id.idAutoBtn);
        ImgButton = (Button) findViewById(R.id.idImgBtn);
        _textView = (TextView) findViewById(R.id.idTextView);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    /*
     * Listeners associated to buttons
     */

    private View.OnClickListener listenerImgButton = new View.OnClickListener() {
        public void onClick(View v) {
            dataToSend = null;
            addMeanVarianceDeviation();

            if(dataSize == 10){
                dataSize = 1;
                dataToSend = createByteArray(1);
                ImgButton.setText("DataS: " + 1);
            }
            else{
                dataSize = dataSize + 1;
                dataToSend = createByteArray(dataSize);
                ImgButton.setText("DataS: " + dataSize);
            }
            addLogEntry("Data size changed to byte array with " + humanReadableByteCountBin(dataToSend.length));
            loggedValues = new ArrayList<>();
        }
    };

    private View.OnClickListener listenerInRangeButton = new View.OnClickListener() {
        public void onClick(View v){
            if (mBound) {
                mManager.requestPeers(mChannel, MainActivity.this);
            } else {
                Toast.makeText(v.getContext(), "Service not bound", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener listenerInGroupButton = new View.OnClickListener() {
        public void onClick(View v){
            if (mBound) {
                showGTable = true;
                mManager.requestGroupInfo(mChannel, MainActivity.this);
            } else {
                Toast.makeText(v.getContext(), "Service not bound", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener listenerAutoButton = new View.OnClickListener() {
        public void onClick(View v){
                if(autoSend){
                    autoSend = false;
                    autoButton.setText("Auto Off");
                }
                else{
                    autoSend = true;
                    autoButton.setText("Auto On");
                }
            Toast.makeText(v.getContext(), "Automatic send: " + autoSend, Toast.LENGTH_SHORT).show();
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        // callbacks for service binding, passed to bindService()

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mManager = new SimWifiP2pManager(mService);
            mChannel = mManager.initialize(getApplication(), getMainLooper(), null);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mManager = null;
            mChannel = null;
            mBound = false;
        }
    };

    /*
     * Asynctasks implementing message exchange
     */

    public class IncommingCommTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "IncommingCommTask started (" + this.hashCode() + ").");
            try {
                mSrvSocket = new SimWifiP2pSocketServer();
                Log.d(TAG, "IncommingCommTask server socket oppened on port 10001");
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    SimWifiP2pSocket sock = mSrvSocket.accept();
                    Log.d(TAG, "IncommingCommTask server socket received external connection, reading msg...");
                    try {

                        //open channels for incoming message
                        ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
                        ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                        out.flush();

                        //Read byte array message
                        byte[] bytesReceived = (byte[]) in.readObject();
                        Log.d(TAG, "IncommingCommTask bytes message received.");
                        //String logMsg = "Received message with size: " + humanReadableByteCountBin(bytesReceived.length);

                        // Send OK message
                        out.writeObject("OK");
                        out.flush();
                        Log.d(TAG, "OK response sent.");


                        //String[] toLog = new String[]{logMsg};
                        //publishProgress(toLog);

                    } catch (IOException | ClassNotFoundException e) {
                        Log.d("Error reading socket:", e.getMessage());
                    } finally {
                        sock.close();
                    }
                } catch (IOException e) {
                    Log.d("Error socket:", e.getMessage());
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String[] values) {
            addLogEntry(values[0]);
        }
    }

    public class SendCommTask extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... msg) {

            SimWifiP2pSocket mCliSocket = null;

            String destinationName = msg[0];
            String destIp = msg[1];
            String destPort = msg[2];

            String destinationString = msg[0] + " " + msg[1] + ":" + msg[2];


            //First try to creat connection

            Log.d(TAG, "Trying to start connection to :" + destinationString);
            try {
                mCliSocket = new SimWifiP2pSocket(destIp, Integer.parseInt(destPort));
            } catch (UnknownHostException | ClassNotFoundException e) {
                e.fillInStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Connection to: " + destinationName + ". Started." );

            try {

                String finallog = "";

                ////////////// Sende message array
                long stringSentTime = globalClass.getCurrentTime();

                // SEND STRING MESSAGE
                mCliSocket.getObjectOutputStream().writeObject(dataToSend);
                mCliSocket.getObjectOutputStream().flush();

                //String logEntry1 = "Sent message with size: " + humanReadableByteCountBin(dataToSend.length);


                // Now we wait for OK response
                Log.d(TAG, "SendCommTask data sent, waiting for response...");
                String response = (String) mCliSocket.getObjectInputStream().readObject();
                Log.d(TAG, "SendCommTask, ok received = " + response);

                // now we log how long it took to send string message and receive ok response
                long stringtimeDif =  globalClass.getCurrentTime() - stringSentTime;
                //finallog = logEntry1 + "- took = " + stringtimeDif + " ms.";
                finallog = "" + stringtimeDif;
                /////////////

                String[] toLog = new String[]{finallog};
                loggedValues.add(toLog[0]);
                publishProgress(toLog);

                mCliSocket.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String[] values) {
            addLogEntry(values[0]);
        }
    }

    /*
     * Listeners associated to Termite
     */

    @Override
    public void onPeersAvailable(SimWifiP2pDeviceList peers) {
        StringBuilder peersStr = new StringBuilder();

        // compile list of devices in range
        for (SimWifiP2pDevice device : peers.getDeviceList()) {
            String devstr = "" + device.deviceName + " (" + device.getVirtIp() + ":" + device.getVirtPort() + ")\n";
            peersStr.append(devstr);
        }

        // display list of devices in range
        new AlertDialog.Builder(this)
                .setTitle("Devices in WiFi Range")
                .setMessage(peersStr.toString())
                .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    @Override
    public void onGroupInfoAvailable(SimWifiP2pDeviceList devices, SimWifiP2pInfo groupInfo) {

        ArrayList<String> connections = new ArrayList<>(); // stores group members [device_name/ip, ...]

        // compile list of network members
        StringBuilder peersStr = new StringBuilder();
        for (String deviceName : groupInfo.getDevicesInNetwork()) {
            SimWifiP2pDevice device = devices.getByName(deviceName);
            String devstr = "" + deviceName + " (" +
                    ((device == null)?"??":device.getVirtIp()) + ":" + device.getVirtPort() + ")\n";
            connections.add( deviceName + " " + device.getVirtIp() + " " + device.getVirtPort() );
            peersStr.append(devstr);
        }
        if(showGTable){
            // display list of network members
            new AlertDialog.Builder(this)
                    .setTitle("Devices in WiFi Network")
                    .setMessage(peersStr.toString())
                    .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
        else{
            updateConnections(connections);
        }
    }


    public void newGroupsDetected(){

        if(!autoSend){
            /* do nothing auto send is of */
        }
        else if (mBound) {
            showGTable = false;
            mManager.requestGroupInfo(mChannel, MainActivity.this);
        } else {
            Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * Helper methods
     */

    public void updateConnections(ArrayList<String> connections){
        ArrayList<String> previousConn = globalClass.getConnections();
        ArrayList<String> newConnections = new ArrayList<>();

        if(previousConn.size() == 0){
            newConnections = connections;
        }
        else{
            for (String name_ip: connections) {
                if(!(previousConn.contains(name_ip))){
                    newConnections.add(name_ip);
                }
            }
        }

        globalClass.setPreviousConnections(connections);

        if(newConnections.size() != 0)
            processNewConnections(newConnections);
    }

    private void processNewConnections(ArrayList<String> connections) {
        Log.d(TAG, "Processing new connections: " + connections.toString() + "\n");

        for (String conn : connections) {
            String[] splitConn = conn.split("\\s+");
            new SendCommTask().executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    splitConn[0], splitConn[1], splitConn[2]);
        }

    }

    public void wifiOn(){
        Intent intent = new Intent(this, SimWifiP2pService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;

        // spawn the chat server background task
        new IncommingCommTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        //Toast.makeText(this, "Wifi-On", Toast.LENGTH_SHORT).show();
    }

    public void wifiOff(){
            unbindService(mConnection);
            mBound = false;
    }

    public void addLogEntry(String log){
        String newData = log + "\n";
        _textView.append(newData);
    }

    private void guiSetButtonListeners() {
        findViewById(R.id.idRangeBtn).setOnClickListener(listenerInRangeButton);
        findViewById(R.id.idGroupsBtn).setOnClickListener(listenerInGroupButton);
        findViewById(R.id.idImgBtn).setOnClickListener(listenerImgButton);
        findViewById(R.id.idAutoBtn).setOnClickListener(listenerAutoButton);
    }

    // used to convert byte to mb
    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    public byte[] createByteArray(int size){
        if(size == 0){
            return new byte[34]; // equal to "Hello World" string
        }
        int onemb = 1048576; //1mb
        return new byte[(onemb*size)];
    }

    public void addMeanVarianceDeviation(){
        if(loggedValues.size() != 0) {

            // The mean average
            double mean = 0.0;
            for (int i = 0; i < loggedValues.size(); i++) {
                mean += Long.parseLong( loggedValues.get(i), 10);
            }
            mean /= loggedValues.size();
            addLogEntry("Mean average: " + mean);

            // The variance
            double variance = 0;
            for (int i = 0; i < loggedValues.size(); i++) {
                variance += Math.pow(Long.parseLong( loggedValues.get(i), 10) - mean, 2);
            }
            variance /= loggedValues.size();
            addLogEntry("Variance: " + variance);

            // Standard Deviation
            double std = Math.sqrt(variance);
            addLogEntry("Standard deviation: " + std);

            //Coefficient of Variation
            double cv = (std/mean)*100;
            addLogEntry("Coefficient of Variation: " + ((int)cv) +"%");
        }
    }
}
