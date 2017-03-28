# Lesson 3 - Simulating device movement
In this lesson, we're going to test node movement on a virtual WiFi Direct network emulated on Termite.
Based on a simple network topology, we're going to test the device detector Android application PeerScanner, covered in [lesson 2](Device-Detection.html), in detecting surrounding devices.

During this lesson we assume you have Termite up and running, and have already ran the PeerScanner application.
In case you have yet to meet these two requirements, we advise you to check [lesson 1](Termite-Configuration.html) and [lesson 2](Device-Detection.html) to meet these requirements respectively.

In short, you'll need the Termite client running, as well as an Android emulator running the PeerScanner application.

Next we present a simple topology we'll use throughout this lesson.

***

### Virtual network topology and states
The virtual network topology chosen for this example is very simple. It comprises two nodes only: A and B. A is an Android device and B is a WiFi beacon. The nodes are firstly located apart from each other, and in a second state they become reachable within their respective WiFi signal ranges. We want to simulate this virtual network as depicted in the figure below such that by running PeerScanner on node A, B is not detected in State 1, but is detected in State 2:

![](../wiki-images/two-node-net.png?raw=true)

### Create the virtual nodes
To create virtual nodes perform the following steps:

1 - On Termite, create two virtual devices, one for A, and other for B, by executing the command `newdevice`.

```
avd:simplechat> newdevice A
avd:simplechat> newdevice B
```

2 - List the devices of the current virtual network with the `list` command.

```
avd:simplechat> list devices
A 0.0.0.0:1 0.0.0.0:1 0.0.0.0:1
B 0.0.0.0:2 0.0.0.0:2 0.0.0.0:2
```

For now, ignore the IP addresses displayed by `list devices`. These addresses will be used for communication between emulated Android devices. You can delete a device using a specific command (`deletedevice [name]`).

### Associate an Android emulator to node A
Associate the Android emulator that will run the PeerScanner application to node A. To perform this association, we need to carry out several steps:

1 - Make sure that one Android emulator is running.

2 - Check that Termite detects that emulator is running by executing the command `list emus`:

```
avd:simplechat> list emus
e1 => online
    name: emulator-5554
```

Then, it is necessary to configure the network addresses of the emulator. In order for the PeerScanner application to be able to communicate with other virtual devices and for the Termite client to communicate with the emulators, it is necessary to perform some port redirection operations. The Termite client can help in this task:

3 - Assign network addresses to each emulator individually using the `assignaddr` command as follows:

```
avd:simplechat> assignaddr e1
avd:simplechat> list emus
e1 => netok
    name: emulator-5554
    addr: [ avaddr = 192.168.0.1:10001, araddr = 10.0.2.2:10011, cvaddr = 127.0.0.1:9001, craddr = 127.0.0.1:9011]
```

From this listing, what is important to understand for now is the `avaddr` attribute which means: application virtual address. Essentially these are the virtual addresses that Termite emulates for the application running on this particular emulator. For example, for emulator-5554, the virtual IP address seen by the application is 192.168.0.1, and the port number where the application will be listening is 10001. The PeerScanner application will be listening for connections on this port. These addresses are specified in the Termite configuration file `etc/netprofiles.conf`.

4 - Bind the emulator to the virtual device A using the `binddevice` command as illustrated next:

```
avd:simplechat> binddevice A e1
avd:simplechat> list devices
A 192.168.0.1:10001 10.0.2.2:10011  127.0.0.1:9011
B 0.0.0.0:2 0.0.0.0:2 0.0.0.0:2
```

The `list devices` command provides details of your virtual network. In the network emulation, these are the names to be used when referring to the virtual nodes, not emulator names. You can see that after the bind operation, the virtual addresses of device A have changed. We are now ready to deploy the application on the emulator and emulate the network states depicted in the figure above.

### Emulate the virtual network - state 1
To emulate the virtual network and simulate device movement perform the following steps:

1 - From Android Studio, deploy the PeerScanner application on the emulator.

2 - Press the "WiFi On" button to start the Termite service on the application.

3 - On the Termite console, execute the `ping` command and verify that the node is online:

```
avd:simplechat> ping
A 127.0.0.1 9011  ONLINE
B 0.0.0.0 2 OFFLINE
```

This means that the Termite service is waiting on port 9011 of the localhost. These ports are internally redirected to the port 9001 inside the emulator. Naturally, since no Android emulator is bound to node B, B appears to be offline.

The initial network state is set so that each node in the network is isolated from each other. If you press "In Range", the list will be empty. This corresponds to State 1 (see the figure above).

### Emulate the virtual network - state 2
From the Termite client, it is possible to modify the topology of the network by triggering certain events. The first relevant event we need to know is "movement", which tells a particular node to move to the neighborhood of another node enabling them to become reachable. Thus, to emulate State 2, we can instruct Termite to "move" A close to node B as shown next:

1 - Make sure that the emulator is visible while you execute the following commands, so that you can see the notifications from them resulting.

2 - Use the commands `move [nodeA] ([nodeB])` and `list neighbors` to "move" the node and check devices' relationships with neighbors:

```
avd:simplechat> move A (B)
avd:simplechat> list neighbors
A => B
B => A
```

Note that while performing the sequence of commands above, the changes to the virtual network topology are performed locally in the termite console only. 

3 - In order to propagate the topology information to the nodes use the `commit` command after `move [nodeA] ([nodeB])`:

```
avd:simplechat> commit
A 127.0.0.1 9011  SUCCESS
B 0.0.0.0 2 FAIL
```

4 - Observe the toast messages reading "Peer list changed".

5 - Click on "In range" to see which peers are available.

In the [following lesson](WiFi-Groups-&-Messages.html), we're going to address the concept of network, more specifically by showing how to create a group.

Meanwhile, we encourage you to try [some exercises](DIY.html#movement-simulation-exercises) covering this section.


