package pt.inesc.termite.server;

import pt.inesc.termite.server.exceptions.ConfigErrorException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class ConfigController {

    public static final String PLATFORM_MAC = "mac";
    public static final String PLATFORM_LINUX = "linux";
    public static final String PLATFORM_WINDOWS = "windows";
    // Default Port values on receivers
    private static int CLIENT_PORT = 8085;
    private static int TSERVER_PORT = 8095;
    private static int LOCAL_PORT = 7000;
    private static InetAddress localIpAdress;
    private static String termiteServerPath, tServerPlatform, androidSDKPath, localNetworkIp;

    public ConfigController() {
    }

    /*
     * Get methods
     */

    public static String getLocalNetworkIp() {
        return localNetworkIp;
    }

    public static InetAddress getLocalInetAddress() {
        return localIpAdress;
    }

    public static String getAndroidSDKPath() {
        return androidSDKPath;
    }

    public static String getTermiteServerPath() {
        return termiteServerPath;
    }

    public static String getServerPlatform() {
        return tServerPlatform;
    }

    public static int getClientPort() {
        return CLIENT_PORT;
    }

    public static int getTserverPort() {
        return TSERVER_PORT;
    }

    public static int getLocalPort() {
        return LOCAL_PORT;
    }

    /*
     * set methods
     */

    public static void loadConfiguration() throws ConfigErrorException {

        androidSDKPath = System.getenv("ANDROID_SDK_PATH");
        if (androidSDKPath == null) {
            throw new ConfigErrorException("Error: environment variable ANDROID_SDK_PATH undefined.");
        }

        termiteServerPath = System.getenv("TERMITE2_SERVER_PATH");
        if (termiteServerPath == null) {
            throw new ConfigErrorException("Error: environment variable TERMITE_SERVER_PATH undefined.");
        }
        setOS();
        setLocalIpAddress();
        setCommunicationPorts();
    }

    private static void setOS() throws ConfigErrorException{
            String os = System.getProperty("os.name").toLowerCase();
            if(os.contains("windows"))
                tServerPlatform = PLATFORM_WINDOWS;
            else if (os.contains("mac"))
                tServerPlatform = PLATFORM_MAC;
            else if(os.contains("linux"))
                tServerPlatform = PLATFORM_LINUX;
            else {
                throw new ConfigErrorException("Error: Invalid platform found, Termite2 is only supported on Windows, Linux or MacOS.");
            }
    }

    private static void setLocalIpAddress() throws ConfigErrorException{
        String filePath = termiteServerPath + File.separator + "config" + File.separator + "localnetwork.txt";
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                } else {
                    localNetworkIp = line;
                    System.out.println("Ip found on localnetwork.txt file.");
                }
            }
        }catch (Exception ex){
            System.out.println("Error: Problem reading config/localnetwork.txt.");
        } finally {
            try {
                if (reader != null)
                    reader.close();
            }catch (IOException e) { e.printStackTrace();}
        }

        if (localNetworkIp == null) {
            System.out.println("Getting automatic ip address...");
            try {
                localNetworkIp = Inet4Address.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                throw new ConfigErrorException("Error: Could not get automatic ip address.");
            }
        }

        try {
            localIpAdress = InetAddress.getByName(localNetworkIp);
            System.out.println("Created InetAddress on ip: " + localNetworkIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new ConfigErrorException("Error: Not able to create InetAddress from ip " + localNetworkIp);
        }
    }

    private static void setCommunicationPorts() throws ConfigErrorException{
        System.out.println("Setting communication ports from config/communicationports.txt file...");
        String filePath = termiteServerPath + File.separator + "config" + File.separator + "communicationports.txt";
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            boolean stop = false;
            while (((line = reader.readLine()) != null ) && !stop) {
                if (line.startsWith("#") || line.length() == 0) {
                    //comment line
                } else {
                    String[] port = line.split(":");
                    if(port.length != 2){
                        System.out.println("Error: Wrong line format '" + line + "'\n Default port values set.");
                        break;
                    }
                    try {
                        switch (port[0]){
                            case "clientport":
                                CLIENT_PORT = Integer.parseInt(port[1]);
                                break;
                            case "serverport":
                                TSERVER_PORT = Integer.parseInt(port[1]);
                                break;
                            //case "localport":
                            //    LOCAL_PORT = Integer.parseInt(port[1]);
                            //    break;
                            default:
                                System.out.println("Error: Wrong port value on line '" + line + "'\n Default port values set.");
                                stop = true;
                                break;
                        }
                    }catch (NumberFormatException e2){
                        System.out.println("Error: Wrong port value on line '" + line + "'\n Default port values set.");
                        stop = true;
                    }
                }
            }
        } catch (Exception ex){
            throw new ConfigErrorException("Error: Problem reading config/communicationports.txt.");
        } finally {
            try {
                if (reader != null)
                    reader.close();
            }catch (IOException e) { e.printStackTrace();}
        }
    }
}
