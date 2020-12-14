package main.pt.inesc.termite2.cli;

import main.pt.inesc.termite2.cli.exceptions.ConfigErrorException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ConfigManager {

    private ArrayList<String> mControllerNetworks = new ArrayList<>();
    private ArrayList<String[]> mControllerFullNetworks = new ArrayList<>(); // this also stores the ports, only used to set up the connections

    private String tomcatPath, termitePath, termitePlatform;

    public static final String PLATFORM_MAC = "mac";
    public static final String PLATFORM_LINUX = "linux";
    public static final String PLATFORM_WINDOWS = "windows";

    public ConfigManager() {
    }

    public ArrayList<String> getControllerNetworks() {
        return mControllerNetworks;
    }

    public ArrayList<String[]> getControllerFullNetworks() {
        return mControllerFullNetworks;
    }

    public String getTomcatPath() {
        return tomcatPath;
    }

    public String getTermitePath() {
        return termitePath;
    }

    public String getTermitePlatform() {
        return termitePlatform;
    }

    public void loadConfigurations() throws ConfigErrorException {

        termitePath = System.getenv("TERMITE2_CLI_PATH");
        if (termitePath == null) {
            throw new ConfigErrorException("Error: environment variable TERMITE2_CLI_PATH undefined.");
        }

        tomcatPath = System.getenv("TOMCAT_PATH");
        if (tomcatPath == null) {
            System.out.println("Warning: environment variable TOMCAT_PATH undefined. Termite2 GUI will not be available.");
            tomcatPath = "undefined";
        }

        setOS();
        loadNetworks();
    }

    private void setOS() throws ConfigErrorException{
        String os = System.getProperty("os.name").toLowerCase();
        if(os.contains("windows"))
            termitePlatform = PLATFORM_WINDOWS;
        else if (os.contains("mac"))
            termitePlatform = PLATFORM_MAC;
        else if(os.contains("linux"))
            termitePlatform = PLATFORM_LINUX;
        else {
            throw new ConfigErrorException("Error: Invalid platform found, Termite2 is only supported on Windows, Linux or MacOS.");
        }
    }

    private void loadNetworks() throws ConfigErrorException {
        String file = "" + termitePath + File.separator + "config" + File.separator + "networks.txt";

        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if(line.startsWith("#") || line.length() == 0){
                    continue;
                }

                String[] ipport = line.split(":");
                int port;

                if(ipport.length == 2){
                    try {
                        port = Integer.parseInt(ipport[1]);
                    }catch (NumberFormatException e){
                        System.out.println("Warning: Invalid port number for network on line: " + line + ".");
                        break;
                    }
                    if(validIpString(ipport[0]) && port > 0){
                        mControllerNetworks.add(ipport[0]);
                        mControllerFullNetworks.add(ipport);
                    }
                }
                else{
                    System.out.println("Warning: Invalid Termite2 Server address format found: " + line + ". Value not registered.");
                }
            }
            reader.close();
        }catch (IOException ex){
            throw new ConfigErrorException("Error: Unable to read network.txt file to load Termite2 Server(s) network(s) to be used.");
        }

        if(mControllerNetworks.size() == 0 ){
            System.out.println("Error: No valid Termite2 Server address(es) found inside networks.txt file.");
            throw new ConfigErrorException("Error: Unable to configure Termite2 Server(s) to be used.");
        }

        System.out.println("Termite2 server networks loaded: " + mControllerNetworks.toString());
    }
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * *   Helper methods  * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private static boolean validIpString(String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {
                    return false;
                }
            }
            return !ip.endsWith(".");
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private static String[] validApplication(String appInfo){
        //trim blank spaces
        appInfo = appInfo.replaceAll("\\s","");
        // split app info on /
        String[] appData = appInfo.split("/");
        if(appData.length == 2){
            return appData;
        }
        return null;
    }
}
