# Handle simulated WiFi Direct events
Throughout the lifecycle of a simulation, the Termite API fires events that can be captured by the
application using a broadcast receiver. The Termite API specifies four events:

* **WIFI_P2P_STATE_CHANGED_ACTION**

   This event is triggered whenever the Termite service is launched or terminated. Along with the
service it is possible to learn the current state of the service. This can be done by obtaining
additional state information located in an extra flag **EXTRA_WIFI_STATE**, and testing whether its
value corresponds to **WIFI_P2P_STATE_ENABLED**.

* **WIFI_P2P_PEERS_CHANGED_ACTION**

   This event is fired whenever there are changes in the set of devices placed within the local
device’s WiFi range. The function that is invoked passes the current list of devices and it is
possible to inspect several details about it using the functions described below.

* **WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION**

   This event is triggered by membership changes in any of the groups that the current device
belongs to. These changes include nodes that join a group or leave a group. The node that triggers
the event does not necessarily have to be a peer to the notified device (but it must be reachable to
the GO of the group). Note that a device could belong to multiple groups.

* **WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION**

   This event happens whenever the ownership state of a group changes: either a node becomes a
GO of a group, or a node ceases to be a GO of a group.
The application must take the initiative to specify how to handle all or part of these events using a
broadcast receiver. The structure of a broadcast receiver that handles these events looks like this:

```java
public class SimWifiP2pBroadcastReceiver extends BroadcastReceiver {
   //...
   @Override
   public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION.
          equals(action)) {
         int state = intent.getIntExtra(SimWifiP2pBroadcast.
              EXTRA_WIFI_STATE, - 1);
         if (state == SimWifiP2pBroadcast.WIFI_P2P_STATE_ENABLED) {
            //...
         } else {
            //...
         }
      } else if (SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION
           .equals(action)) {
         //...
      } else if (SimWifiP2pBroadcast.
           WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION.equals(action)) {
         SimWifiP2pInfo ginfo = (SimWifiP2pInfo) intent.
           getSerializableExtra(SimWifiP2pBroadcast.EXTRA_GROUP_INFO);
         //...
      } else if (SimWifiP2pBroadcast.
           WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION. equals(action)) {
         SimWifiP2pInfo ginfo = (SimWifiP2pInfo) intent.
           getSerializableExtra(SimWifiP2pBroadcast.EXTRA_GROUP_INFO);
         //...
      }
   }
}
```
