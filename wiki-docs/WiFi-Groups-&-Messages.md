# Lesson 4 - WiFi group formation and message exchange
In this lesson, we're going to demonstrate how to build Android applications that can set up WiFi groups and exchange messages. In order to do so, we will follow the idea of [lesson 2](Device-Detection.html), and inspect the internals of a demo application called MsgSender. Note that this app extends the functionality of PeerScanner, the demo app from [lesson 2](Device-Detection.html).

For a visual representation of the interactions involved in this lesson, click [here](../group-management.html).

***

### Setup the MsgSender application
1. Download the [MsgSender app](http://www.gsd.inesc-id.pt/~wiki/courses/cmu1516/lab04/Termite-WifiP2P-MsgSender-20160329.tgz) and decompress it on a local directory.
2. Open MsgSender on Android Studio.
3. Compile the application and run it on an Android Studio emulator.

### Exploring MsgSender
MsgSender is an application that allows devices to form a WiFi group and enables their respective users to exchange messages. To send a message to another device, the user must write the virtual IP address of the destination node, press "Connect", then write a message and press "Send".

Now that you setted up MsgSender, lets take a careful look at its internals:

   * **Message handling asynctasks:** The MsgSender activity handles the sending and reception of messages through a set of dedicated asynctasks. Here's a brief explanation on each one of them:

      * The `IncommingCommTask` receives incoming connections and renders the message received on the activity's output TextView element:
      
```java
public class IncommingCommTask extends AsyncTask<Void, String, Void> {

   @Override
   protected Void doInBackground(Void... params) {			
      Log.d(TAG, "IncommingCommTask started (" + this.hashCode() +
          ").");

      try {
         mSrvSocket = new SimWifiP2pSocketServer(
              Integer.parseInt(getString(R.string.port)));
      } catch (IOException e) {
         e.printStackTrace();
      }
      while (!Thread.currentThread().isInterrupted()) {
         try {
            SimWifiP2pSocket sock = mSrvSocket.accept();
            try {
               BufferedReader sockIn = new BufferedReader(
                   new InputStreamReader(sock.getInputStream()));
               String st = sockIn.readLine();
               publishProgress(st);
               sock.getOutputStream().write(("\n").getBytes());
            } catch (IOException e) {
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
   protected void onProgressUpdate(String... values) {
      mTextOutput.append(values[0] + "\n");
   }
}
```

  * The `OutgoingCommTask` opens a connection to another device, renders the result of the operation on the activity's output TextView element, and sets the state of the activity's buttons:
      
```java
public class OutgoingCommTask extends AsyncTask<String, Void, String> {

   @Override
   protected void onPreExecute() {
      mTextOutput.setText("Connecting...");
   }

   @Override
   protected String doInBackground(String... params) {
      try {
         mCliSocket = new SimWifiP2pSocket(params[0],
         Integer.parseInt(getString(R.string.port)));
      } catch (UnknownHostException e) {
         return "Unknown Host:" + e.getMessage();
      } catch (IOException e) {
         return "IO error:" + e.getMessage();
      }
      return null;
   }

   @Override
   protected void onPostExecute(String result) {
      if (result != null) {
         guiUpdateDisconnectedState();
         mTextOutput.setText(result);
      } else {
         findViewById(R.id.idDisconnectButton).setEnabled(true);
         findViewById(R.id.idConnectButton).setEnabled(false);
         findViewById(R.id.idSendButton).setEnabled(true);
         mTextInput.setHint("");
         mTextInput.setText("");
         mTextOutput.setText("");
      }
   }
}
```

   * The `SendCommTask` uses the socket previously opened by the `OutgoingCommTask` to send the message to the intended device, and it sets the state of the activity's buttons:
      
```java
public class SendCommTask extends AsyncTask<String, String, Void> {

   @Override
   protected Void doInBackground(String... msg) {
      try {
         mCliSocket.getOutputStream().write(
             (msg[0] + "\n").getBytes());
         BufferedReader sockIn = new BufferedReader(
             new InputStreamReader(mCliSocket.getInputStream()));
         sockIn.readLine();
         mCliSocket.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
      mCliSocket = null;
      return null;
   }

   @Override
   protected void onPostExecute(Void result) {
      mTextInput.setText("");
      guiUpdateDisconnectedState();
   }
}
```

   * **Group change listener:** In order for an activity to be notified of group change detections by the Termite dedicated service, it must implement the `GroupInfoListener` interface, more specifically the `void onGroupInfoAvailable(SimWifiP2pDeviceList devices, SimWifiP2pInfo groupInfo)` method. In MsgSender's activity, the method is implemented in a way that displays the new list of devices in the group after the last cast change, through a system dialog. For a detailed description on the information made available by Termite's API, click [here](Network-Probing.html).
   
```java
public class MsgSenderActivity extends Activity implements
    PeerListListener, GroupInfoListener {
    //...
   @Override
   public void onGroupInfoAvailable(SimWifiP2pDeviceList devices,
       SimWifiP2pInfo groupInfo) {
      // compile list of network members
      StringBuilder peersStr = new StringBuilder();
      for (String deviceName : groupInfo.getDevicesInNetwork()) {
         SimWifiP2pDevice device = devices.getByName(deviceName);
         String devstr = "" + deviceName + " (" + 
             ((device == null) ? "??" : device.getVirtIp()) + ")\n";
         peersStr.append(devstr);
      }

      // display list of network members
      new AlertDialog.Builder(this)
      .setTitle("Devices in WiFi Network")
      .setMessage(peersStr.toString())
      .setNeutralButton("Dismiss",
          new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int which) { 
         }
      }).show();
   }
   //...
}
```

Now that you know how an application can set up WiFi groups and exchange messages, try a test interaction in [lesson 5](Simulating-Groups.html).

**Note:** If you have yet to configure Termite, we suggest you to visit <a href="Termite-Configuration.html">lesson 1</a>, before moving to [lesson 5](Simulating-Groups.html).
