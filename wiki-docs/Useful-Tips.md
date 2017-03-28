# Useful Tips
Here we provide tips on how to import the Termite API into your project, and how to use the Genymotion connector.

***

### Importing the Termite API into your project
In order to use the Termite API in your project, perform the following steps:

1 - Unzip the library file [Termite-WifiP2P-API-20160329.tgz](http://www.gsd.inesc-id.pt/~wiki/courses/cmu1516/lab04/Termite-WifiP2P-API-20160329.tgz) into the subdirectory `libs` of your project (e.g., `MyProject/app/libs`).

2 - Add the following code in your file `MyProject/app/build.gradle`:

```
dependencies {
  compile(name:'Termite-WifiP2P-API', ext:'aar')
}
repositories{
  flatDir{
    dirs 'libs'
  }
}
```

### Using the Genymotion connector
Genymotion is a Virtual Machine manager based on VirtualBox. It provides a similar functionality to AVD's, but performs much better and consumes less resources than AVD. To manage Genymode emulators in Termite, follow these steps:

1 - Install [VirtualBox](https://www.virtualbox.org/).

2 - Install [Genymotion](https://www.genymotion.com/).

3 - Start Genymotion, and create a new virtual device from the image "Custom Phone - 5.0.0 - API 21 - 768x1280". 

4 - Assign that virtual device a name, e.g. "TVD - 5.0.0 - API 21 - 768x1280".

5 - Set the virtual device's memory to 512MB.

6 - Check "Use virtual keyboard for input" on the virtual device's settings.

7 - Create two clones of this virtual device image using the command line, as shown below:

```
$ VBoxManage clonevm "TVD - 5.0.0 - API 21 - 768x1280" --name "TVD - 5.0.0 - API 21 - 768x1280-Clone0" --register
$ VBoxManage clonevm "TVD - 5.0.0 - API 21 - 768x1280" --name "TVD - 5.0.0 - API 21 - 768x1280-Clone1" --register
$ VBoxManage list vms
```

These clones will be the images of the emulators to be launched by Termite.

**Note:** it is very important that the name of each clone follows the convention `vdname-Clone#`, otherwise, Termite won't be able to spawn emulators. The clone numbering # starts in 0.
