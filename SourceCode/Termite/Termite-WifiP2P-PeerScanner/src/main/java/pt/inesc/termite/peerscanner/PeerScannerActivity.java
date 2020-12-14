package pt.inesc.termite.peerscanner;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.SimWifiP2pManager.Channel;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pManager.PeerListListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class PeerScannerActivity extends Activity implements
		PeerListListener {

    public static final String TAG = "peerscanner";

    private SimWifiP2pManager mManager = null;
    private Channel mChannel = null;
	private boolean mBound = false;
    private SimWifiP2pBroadcastReceiver mReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// initialize the UI
		setContentView(R.layout.main);
		guiSetButtonListeners();
		guiUpdateInitState();

		// register broadcast receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
		filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
		filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
		filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
		mReceiver = new SimWifiP2pBroadcastReceiver(this);
		registerReceiver(mReceiver, filter);
	}

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

	/*
	 * Listeners associated to buttons
	 */

	private OnClickListener listenerWifiOnButton = new OnClickListener() {
        public void onClick(View v){
        	Intent intent = new Intent(v.getContext(), SimWifiP2pService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            mBound = true;
            guiUpdateDisconnectedState();
        }
	};

	private OnClickListener listenerWifiOffButton = new OnClickListener() {
        public void onClick(View v){
            if (mBound) {
                unbindService(mConnection);
                mBound = false;
                guiUpdateInitState();
            }
        }
	};

	private OnClickListener listenerInRangeButton = new OnClickListener() {
        public void onClick(View v){
        	if (mBound) {
                mManager.requestPeers(mChannel, PeerScannerActivity.this);
        	} else {
            	Toast.makeText(v.getContext(), "Service not bound",
            		Toast.LENGTH_SHORT).show();
            }
        }
	};

	private ServiceConnection mConnection = new ServiceConnection() {
		// callbacks for service binding, passed to bindService()

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mManager = new SimWifiP2pManager(new Messenger(service));
            mChannel = mManager.initialize(getApplication(), getMainLooper(), null);
            mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mManager = null;
            mChannel = null;
			mBound = false;
		}
	};

	/*
	 * Termite listeners
	 */
	
	@Override
	public void onPeersAvailable(SimWifiP2pDeviceList peers) {
		StringBuilder peersStr = new StringBuilder();
		
		// compile list of devices in range
		for (SimWifiP2pDevice device : peers.getDeviceList()) {
			String devstr = "" + device.deviceName + " (" + device.getVirtIp() + ")\n";
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

	/*
	 * Helper methods for updating the interface
	 */

	private void guiSetButtonListeners() {
		
		findViewById(R.id.idWifiOnButton).setOnClickListener(listenerWifiOnButton);
		findViewById(R.id.idWifiOffButton).setOnClickListener(listenerWifiOffButton);
		findViewById(R.id.idInRangeButton).setOnClickListener(listenerInRangeButton);
	}

	private void guiUpdateInitState() {
		
		findViewById(R.id.idWifiOnButton).setEnabled(true);
		findViewById(R.id.idWifiOffButton).setEnabled(false);
		findViewById(R.id.idInRangeButton).setEnabled(false);
	}

    private void guiUpdateDisconnectedState() {

        findViewById(R.id.idWifiOnButton).setEnabled(false);
        findViewById(R.id.idWifiOffButton).setEnabled(true);
        findViewById(R.id.idInRangeButton).setEnabled(true);
    }
}
