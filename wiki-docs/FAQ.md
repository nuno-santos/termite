# FAQ
Here you can find a list of the most common issues faced when using Termite.

***

**1 - What version of Android Studio should I use?**

We encourage you to use the most up-to-date version of [Android Studio](https://developer.android.com/studio/install.html).

**2 - When I open either of the two demo apps from the tutorial lessons, Android Studio asks whether I want to update both the Gradle and Android Studio Gradle plugin versions. Should I proceed with the updates?**

Yes, these two updates will be performed on the demo apps' configuration files, and they ensure compatibility with the most up-to-date Android Studio.

**3 - When I execute the Termite script (or Termite bash file for Windows), I receive a `java.lang.UnsupportedClassVersionError` exception. How can I solve this?**

```
nsantos@laptop:~/Termite-Cli$ ./termite.sh 
Exception in thread "main" java.lang.UnsupportedClassVersionError: pt/inesc/termite/cli/Main : Unsupported major.minor version 52.0
	at java.lang.ClassLoader.defineClass1(Native Method)
	at java.lang.ClassLoader.defineClassCond(ClassLoader.java:631)
	at java.lang.ClassLoader.defineClass(ClassLoader.java:615)
	at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:141)
	at java.net.URLClassLoader.defineClass(URLClassLoader.java:283)
	at java.net.URLClassLoader.access$000(URLClassLoader.java:58)
	at java.net.URLClassLoader$1.run(URLClassLoader.java:197)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.net.URLClassLoader.findClass(URLClassLoader.java:190)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:306)
	at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:301)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:247)
Could not find the main class: pt.inesc.termite.cli.Main.  Program will exit.
```

Make sure your machine is running the most up-to-date version of JDK. Download it [here](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html).

**4 - Should I ignore the following error when I execute the `assignaddr` command on the Termite client?**

```
avd:simplechat> assignaddr e1
Error: KO: bad redirection format, try (tcp|udp):hostport
```

Yes.

**5 - Should I ignore the following error when I execute the `assignaddr` command on the Termite client?**

```
avd:simplechat> assignaddr e1
Error: KO: unknown command, try 'help'
```

No. This means ADB is not allowing Termite to perform the necessary port redirections. The problem is due to an authentication step required in the newer versions of Android Studio's emulators. In order to solve this issue, find the location of the authentication token file on your machine via telnet, and delete its contents. Here's an example in Linux, where the emulator's port is 5554:

```
nsantos@laptop:~/Termite-Cli$ telnet localhost 5554
Trying 127.0.0.1...
Connected to localhost.
Escape character is '^]'.
Android Console: Authentication required
Android Console: type 'auth <auth_token>' to authenticate
Android Console: you can find your <auth_token> in
'/home/nsantos/.emulator_console_auth_token'
OK
```

At this point, make a backup of the file and delete its contents:

```
nsantos@laptop:~/Termite-Cli$ cp /home/nsantos/.emulator_console_auth_token /home/nsantos/.emulator_console_auth_token_ backup
nsantos@laptop:~/Termite-Cli$ echo  > ~/.emulator_console_auth_token
```

Terminate the Termite client and try again.

**6 - For some reason I was forced to relaunch one or more emulators that were running my Termite-compatible application. Do I need to repeat all the network formation steps from scratch to able to continue testing?**

It depends. If your Termite client is still running and your network topology is already in its memory, the answer is no. In this case you simply need to deploy your application to all emulators, and execute the `commit` command on the Termite client, to propagate the current network state to the devices. To know what's the current network state of the Termite client, execute: `list network`. If your Termite client was terminated, then you're forced to repeat all the network formation steps from scratch.
