# Setting up TCP connections
Once the service is active, it is possible to set up TCP connections. For that, the server and the client parts
must use two wrappers of the typical socket interface. These wrappers have been tailored for Termite:
`SimWifiP2pSocket`, and `SimWifiP2pSocketServer`. The server side looks like this:

```java
private SimWifiP2pSocketServer mSrvSocket = null;
///...
try {
   mSrvSocket = new SimWifiP2pSocketServer(10001);
   SimWifiP2pSocket sock = mSrvSocket.accept();
   ///...
   sockIn = new BufferedReader(
      new InputStreamReader(sock.getInputStream()));
   String s = sockIn.readLine();
   //...
} catch (IOException e) {
   e.printStackTrace();
}
```

The client side looks like this:

```java
SimWifiP2pSocket mCliSocket = null;
///...
try {
   mCliSocket = new SimWifiP2pSocket("192.168.0.2",10001);
} catch (UnknownHostException e) {
   return "Unknown Host:" + e.getMessage();
} catch (IOException e) {
   return "IO error:" + e.getMessage();
}
//...
try {
   mCliSocket.getOutputStream().write("Hello World\n");
} catch (IOException e) {
   Log.d("Error reading socket:", e.getMessage());
}
```

These classes do two things: (i) translate the virtual addresses to real addresses, and (ii) simulate the
communication paths expressed in the topology of the spontaneous network. Therefore, the IP address
that is fed to the client corresponds to the virtual IP address expressed in the topology script for the
targeted destination. Likewise for the virtual port. To reflect the topology changes of the network,
whenever the connections between peers are not possible, the connection will fail: the `connect` method
will produce an exception when starting a connection, of a `IOException` will be generated when reading
from the socket if the connection is active and is broken down.
