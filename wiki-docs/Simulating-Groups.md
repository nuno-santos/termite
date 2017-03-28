# Lesson 5 - Simulating WiFi groups
In this lesson, we're going to test the creation of networks and message exchanges between devices on a virtual WiFi Direct network emulated on Termite.
Like in [lesson 3](Simulating-Movement.html), we're going to test these two features by simulating a simple network topology, while interacting with it through MsgSender Android application, covered in the [previous lesson](WiFi-Groups-&-Messages.html).

During this lesson we assume you have Termite up and running, and have already ran the MsgSender application.
In case you have yet to meet these two requirements, we advise you to check [lesson 1](Termite-Configuration.html) and [lesson 4](WiFi-Groups-&-Messages.html) to meet these requirements respectively.

***

### Virtual network topology and states
Similarly to [lesson 3](Simulating-Movement.html), the virtual network topology chosen for this example is very simple. It comprises two virtual Android devices (A and B), both of which running the MsgSender application. By the figure bellow we can see that states 1 and 2 are similar to the ones presented in [lesson 3](Simulating-Movement.html), with difference being that here the two nodes are Android devices, i.e., none are WiFi beacons. In State 3, a group is formed between the nodes, being A the group owner (GO). At this point, they are assigned virtual IP addresses and can open regular TCP/IP connections based on their respective virtual IPs.

![](../wiki-images/two-node-group.png?raw=true)

### Setup state 2's network topology
Following the commands presented in [lesson 3](Simulating-Movement.html) create the topology corresponding to state 2 as shown bellow. **Note:** this time, both nodes are Android devices, which means you need to launch two emulators, deploy MsgSender to the both of them, and click the "WiFi On" button again in the both of them.

```
avd:simplechat> newdevice A
avd:simplechat> newdevice B
avd:simplechat> assignaddr e1
avd:simplechat> assignaddr e2
avd:simplechat> binddevice A e1
avd:simplechat> binddevice B e2
avd:simplechat> ping
A	127.0.0.1	9011	ONLINE
B	127.0.0.1	9021	ONLINE
avd:simplechat> move A (B)
avd:simplechat> commit
B	127.0.0.1	9021	SUCCESS
A	127.0.0.1	9011	SUCCESS
```

### Emulate group formation and peer-to-peer communication
To create groups perform the following steps:

1 - Create a group containing nodes A and B, with A being the group owner (GO) of the network, through the `creategroup` command:

`avd:simplechat> creategroup A (B)`

2 - Make sure that both emulators are visible while you execute the following commands, so that you can see the notifications from them resulting.

3 - Browse the information on the existing groups by executing the `list groups` command, and propagate this new network configuration to the nodes:

```
avd:simplechat> list groups
A => B
avd:simplechat> commit
B 127.0.0.1 9021 SUCCESS
A 127.0.0.1 9011 SUCCESS
```

4 - Observe the toast messages reading "Network membership changed", and "Group owner changed" for node A.

5 - On A, learn B's address (in theory should be 192.168.0.2).

6 - Type B's address on A's EditText component, and then hit the "Connect" button.

This will open sockets between both nodes enabling them to exchange messages in a conversational fashion. Test this feature:

7 - On A, write a message and send it to B by clicking the "Send" button.

### Emulate group destruction and communication breaks
To delete the group, perform the following steps:

1 - Make sure that both emulators are visible while you execute the following commands, so that you can see the notifications from them resulting.

2 - Delete the group using the `deletegroup` command, and propagate the changes to the nodes:

```
avd:simplechat> deletegroup A
avd:simplechat> commit
B 127.0.0.1 9021  SUCCESS
A 127.0.0.1 9011  SUCCESS
```

3 - Observe the toast messages in both devices

It is also possible to emulate nodes moving away and leaving the group without explicitly destroying the group. 

4 - To test this, recreate the group:

```
avd:simplechat> creategroup A (B)
avd:simplechat> list groups
A => B
avd:simplechat> commit
B 127.0.0.1 9021 SUCCESS
A 127.0.0.1 9011 SUCCESS
```

5 - Observe the toast messages in both devices

6 - On B, open a connection to A.

7 - Move the nodes apart from each other, effectively causing the group to be automatically destroyed:

```
avd:simplechat> move A ()
avd:simplechat> list n
A => 
B => 
avd:simplechat> list groups
A => 
avd:simplechat> commit
B 127.0.0.1 9021  SUCCESS
A 127.0.0.1 9011  SUCCESS
```

### Adding members to pre-existing groups
Test the re-addition of nodes to pre-existing groups by performing the following steps:

1 - Make sure that both emulators are visible while you execute the following commands, so that you can see the notifications from them resulting.

2 - Add B to A's group by moving it again near B and by rejoining the group by executing the `joingroup` command:

```
avd:simplechat> move B (A)
avd:simplechat> joingroup B (A)
```

3 - Browse the information on the existing groups, and propagate the changes to the nodes:

```
avd:simplechat> list groups
A => B
avd:simplechat> commit
B 127.0.0.1 9021 SUCCESS
A 127.0.0.1 9011 SUCCESS
```

4 - Observe the toast messages in both devices

5 - Verify that you can again open a channel between A and B and have them communicating.

**Tips:** To redeploy the application, you only need to: deploy it to both emulators, and execute the `commit` command on the Termite client, to propagate the current network state to the devices. It is not necessary to repeat all the network formation steps from scratch if the network topology you need to emulate is already in memory in the termite tool. To know what's the current network state execute: `list network`.
