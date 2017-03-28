# Lesson 1 - Configuring Termite
In this lesson we're going to learn how to configure Termite.

Note that throught this lesson we provide information on how to configure Termite on three different platforms, namely: Windows, Linux, and Mac.


***


### Install Termite
To install Termite, perform the following steps:

1 - Download the [Termite client](http://www.gsd.inesc-id.pt/~wiki/courses/cmu1516/lab04/Termite-Cli-20160329.tgz) and decompress it on a local directory.

2 - Configure attributes of your Termite installation. Under the `Termite-Cli` directory, open the file `etc/platform/X/backends.conf`, where X is your target platform: `windows`, `linux`, or `mac`. By default, this file looks as follows:

```
{
    "backends" : [
        {
            "id" : "avd",
            "connector" : "avd",
            "config" : {
                "sdk" : "/Users/nsantos/Library/Android/sdk",
                "vmi" : "Nexus_5_API_21_x86"
            }
        }
    ]
}
```

3 - You need to update the attributes `sdk` and `vmi`. The `sdk` attribute must be updated with the correct Android SDK path. A simple way to determine this is by opening Android Studio and opening the project settings. (Note: on Windows, you must write the SDK path using doubled backslashes, (e.g., `c:\\my\\path\\to\\sdk`).

4 - For now ignore the `vmi` attribute.

### Run the termite tool
Run Termite by performing the following steps:

1 - Open a terminal window and navigate to directory `Termite-Cli`.

2 - Set the environment variables `TERMITE_CLI_PATH` to point to the location of the Termite CLI module, i.e., this path, and `TERMITE_PLATFORM` to the platform's name.

   * In Linux or Mac OS:
   
      * Update file `etc/platform/{linux,mac}/env_setup.sh` and then, on the terminal window, execute the command: `source etc/platform/{linux,mac}/env_setup.sh`. Naturally, you can also create your own "env" file.
      * In that same file, setup the environment variable `TERMITE_PLATFORM` to either: `linux`, or `mac`.
      * To execute the Termite client, run the script ./termite.sh.
      
   * On Windows:
      * Use the command `set TERMITE_CLI_PATH=...`.
      * Use the command `set TERMITE_PLATFORM=windows`.
      * To execute the Termite client, run the batch file `termite.bat`.

3 - Run the Termite client:

   * In Linux or Mac OS:
      * Run the script `termite.sh`.
   * On Windows:
      * Run the batch file `termite.bat`.

4 - If everything goes well, the following output is expected:

```
  Termite Testbed
  Working Directory = /Users/nsantos/Desktop/Termite-Cli
  Type "help" or "h" for the full command list

avd:simplechat>
``` 
Termite is now up and ready!

Learn how Android applications can detect the presence of nearby devices using WiFi Direct with Termite, in [lesson 2](Device-Detection.html).
