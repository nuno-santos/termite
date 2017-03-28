# Probing the network
Here, we provide information on how to get simulated WiFi network information, as well as details on the network information retrieval API and type of data returned by that API's methods.

### Retrieving simulated WiFi network information
Before establishing a TCP connection, it is necessary to know which devices are part of the network and
learn their IPs. This and other information about the simulated WiFi network can be retrieved by 
obtaining instances of classes `SimWifiP2pInfo` and `SimWifiP2pDeviceList`. There are two ways to
obtain these classes:

   **1. Notified by Termite whenever there are changes in the network:** In this case, instances of these
classes are passed to the listeners of the broadcast receiver (see [here](Simulating-Groups.html)).

   **2. Explicitly requested by the application:** In particular, calling the methods `requestPeers` and
`requestGroupInfo` of the `SimWifiP2pManager` class. The first method obtains the list of devices that
are within the WiFi range of the device where the call is made, irrespective of whether or not the devices
belong to a P2P group; the second method obtains various information about the WiFi groups of the
calling device and about the devices that are connected to the same groups. Both methods return
immediately and the results are propagated to the application using listener classes, which looks like this:

```java
mManager.requestPeers(mChannel, (PeerListListener)
     SimpleChatActivity.this);
//...
@Override
public void onPeersAvailable(SimWifiP2pDeviceList peers) {
   //...
}
```

And this is what the listener looks like for for the second callback:

```java
mManager.requestGroupInfo(mChannel, (GroupInfoListener)
     SimpleChatActivity.this);
//...
@Override
public void onGroupInfoAvailable(SimWifiP2pDeviceList devices,
     SimWifiP2pInfo groupInfo) {
   //...
}
```

### Network information retrieval API
Callbacks and broadcast receiver methods return their results in two classes: `SimWifiP2pInfo`, and
`SimWifiP2pDeviceList`. These are some of the most relevant methods:

* **[SimWifiP2pInfo] String getDeviceName()**

   Returns the name of the device, identification that corresponds to the device name contained in
the topology file.

* **[SimWifiP2pInfo] Set\<String\> getDevicesInNetwork()**

   Returns the name of the devices that are in the same network of the calling device. It will include
the name of the devices registered in all the P2P groups of which the home device is client or
group owner of.

* **[SimWifiP2pInfo] boolean askIsConnected()**

   Tells whether the local device is connected to a WiFi network, i.e. is group owner of a P2P group,
or client of one or multiple P2P groups.

* **[SimWifiP2pInfo] boolean askIsGO()**

   Tells whether the local device is the group owner of a P2P group.

* **[SimWifiP2pInfo] boolean askIsClient()**

   Tells whether the local device is the client of a P2P group.

* **[SimWifiP2pInfo] boolean askIsConnectionPossible(String deviceName)**

   Tells whether it is possible to set up a TCP connection between the local device and a remote
device.
This class uses the names of the devices as IDs. To translate this information to address information,
which is necessary for setting up TCP connections, we must use the instances of the
SimWifiP2pDeviceList, which contain device descriptors (class SimWifiP2pDevice).

* **[SimWifiP2pDeviceList] Collection\<SimWifiP2pDevice\> getDeviceList()**

   Returns the list of devices. This list includes different sets of devices depending on the callback
method that returns the instance of this class. The instance returned by the requestPeers
callback refers only to devices that are within the range of the local host. The instance returned by
the requestGroupInfo callback includes all the devices of the simulation scenario.

* **[SimWifiP2pDeviceList] SimWifiP2pDevice getByName(String deviceName)**

   Yields the device descriptor based on the device name or null if the device descriptor was not
found in the list.
The device descriptor contains address information that is necessary for connecting the devices using
sockets:

   * **[SimWifiP2pDevice] String getVirtIp()**

      Returns the virtual IP of the device referred in the device descriptor.

   * **[SimWifiP2pDevice] String getVirtPort()**

      Returns the virtual port of the device referred in the device descriptor.
The virtual devices will be used for setting up TCP connections.
