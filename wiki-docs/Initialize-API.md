# Initialize the Termite API
Before using the specific services of the Termite API, an application must perform a sequence of
initialization steps:

* **Update the manifest to launch the Termite service**

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
   ...
   <application
   ...
      <service
         android:name="pt.inesc.termite.wifidirect.service.
              SimWifiP2pService" />
   </application>
</manifest>
```

* **Initialize the Termite Socket Manager**

```java
// initialize the Termite API
SimWifiP2pSocketManager.Init(getApplicationContext());
```

* **Register the events you wish the application to be notified of:**

```java
IntentFilter filter = new IntentFilter();
filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
filter.addAction(SimWifiP2pBroadcast.
     WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
filter.addAction(SimWifiP2pBroadcast.
     WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
SimWifiP2pBroadcastReceiver receiver = 
     new SimWifiP2pBroadcastReceiver(this);
registerReceiver(receiver, filter);
```

* **Bind the Termite Service**

```java
Intent intent = new Intent(v.getContext(), SimWifiP2pService.class);
bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
```

   In the binding process, it is necessary to pass two callbacks, which are invoked if the service has been
correctly connected, or otherwise. These callbacks are implemented by a `ServiceConnection` class in
methods `onServiceConnected` and `onServiceDisconnected`, as illustrated below:

```java
private SimWifiP2pManager mManager = null;
private Channel mChannel = null;
private Messenger mService = null;
///...

private ServiceConnection mConnection = new ServiceConnection() {
// callbacks for service binding, passed to bindService()
   @Override
   public void onServiceConnected(ComponentName className,
        IBinder service) {
      mService = new Messenger(service);
      mManager = new SimWifiP2pManager(mService);
      mChannel = mManager.initialize(getApplication(), getMainLooper(),
         null);
      //...
   }
    @Override
   public void onServiceDisconnected(ComponentName arg0) {
      mService = null;
      mManager = null;
      mChannel = null;
      //...
   }
};
```

   This code also shows the final steps that must be done in order to make full use of the Termite API,
namely the instantiation and initialization of the `SimWifiP2PManager` class, which will provide the
interface between the application and the Termite service.
