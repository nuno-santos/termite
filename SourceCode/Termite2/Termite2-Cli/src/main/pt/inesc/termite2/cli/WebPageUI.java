package main.pt.inesc.termite2.cli;

import jline.console.ConsoleReader;
import main.pt.inesc.termite2.cli.commands.ListCommand;
import main.pt.inesc.termite2.cli.exceptions.WebPageErrorException;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class WebPageUI {

    private static final int DEFAULT_SERVER_PORT = 8081;

    private ConsoleReader mReader;
    private Context mContext;
    private String mTermite2Path;
    private String mPlatform;
    private String mTomcatPath;

    public WebPageUI(Context c) throws WebPageErrorException {
        mContext = c;
        mReader = mContext.getReader();
        mTermite2Path = mContext.mConfigManager.getTermitePath();
        mPlatform = mContext.mConfigManager.getTermitePlatform();
        mTomcatPath = mContext.mConfigManager.getTomcatPath();


        if(copyUifolderToTomcat()){
            if(!startTomcat()) {
                throw new WebPageErrorException("Error starting web page ui.");
            }
        }else{
            throw new WebPageErrorException("Error starting web page ui.");
        }
    }

    public void start() {

        Termite2UIWorker uiWorker = new Termite2UIWorker(mContext);
        Thread t = new Thread(uiWorker);
        t.start();

        String line = null;
        String[] tokens = null;
        try {
            while ((line = mReader.readLine()) != null) { // main process to receive control command quit and list

                tokens = line.split("\\s+");

                String cmd = tokens[0];
                if (cmd.equals("quit") || cmd.equals("q")) {
                    mContext.mRemoteAVDController.closeConnections();
                    stopTomcat();
                    System.exit(0);
                }
                for (Command command : mContext.getSharedCommands()) {
                    if (command.getName().equals(cmd) || (
                            !command.getAbvr().equals(Command.NULLABVR) &&
                                    command.getAbvr().equals(cmd))) {
                        if(cmd.equals("help") || cmd.equals("h")){
                            tokens = new String[]{cmd,"shared"};
                        }
                        String result = command.executeCommand(mContext,tokens);

                        if(result.equals("OK")){
                            mContext.getHistory().setLast(line);
                        }else{
                            System.out.println(result);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private boolean startTomcat() {
        System.out.println("\nstarting Tomcat...");
        try {
            if(mPlatform.equals("mac") || mPlatform.equals("linux") )
                Runtime.getRuntime().exec(mTomcatPath+"/bin/./startup.sh");
            else if(mPlatform.equals("windows")) {
                File tomcatBin = new File((mTomcatPath+"/bin"));
                Runtime.getRuntime().exec(new String[]{"cmd.exe", "/K", "start", "catalina.bat", "run"}, null, tomcatBin); //Works
            }
            System.out.println("Tomcat started.");
            return true;
        }catch (IOException e) {
            System.out.println("Problem starting Tomcat server.");
            e.printStackTrace();
            return false;
        }
    }

    private void stopTomcat() {
        System.out.println("\nstopping Tomcat...");
        try {
            if(mPlatform.equals("mac") || mPlatform.equals("linux") ){
                Runtime.getRuntime().exec(mTomcatPath+"/bin/./shutdown.sh");
            }
            /*else if(platfrom.equals("windows")){ //This is not needed the user just has to close the tomcat window
                File tomcatBin = new File((tomcatPath+"/bin"));
                Runtime.getRuntime().exec(new String[]{"cmd.exe", "/K", "start", "catalina.bat", "stop"}, null, tomcatBin); //works
            }*/;
            System.out.println("Tomcat stopped.");
        }catch (IOException e) {
            System.out.println("Problem shutting down Tomcat server.");
            e.printStackTrace();
        }
    }

    private boolean copyWarFileToTomcat() {
        File srcFile = new File(mTermite2Path + "/ui/Termite2UI.war");
        File dest = new File(mTomcatPath + "/webapps");
        File destWar = new File (mTomcatPath + "/webapps/Termite2UI.war");

        if(!srcFile.exists() || !dest.exists()){
            System.out.println("Interface ~/ui/Termite2UI.war file not found.");
            return false;
        }
        if(!dest.exists()){
            System.out.println("Tomcat webapps folder not found.");
            return false;
        }
        if(destWar.exists()){
            System.out.println("Termite2UI.war already exists.");
            return true;
        }
        else{
            System.out.println("Termite2UI.war does not exists. Copying it...");
            try {
                FileUtils.copyFileToDirectory(srcFile, dest, true);
                System.out.println("Termite2UI.war copied to: " + mTomcatPath + "/webapps");
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private boolean copyUifolderToTomcat(){
        String srcFilePath = mTermite2Path + File.separator + "ui" + File.separator + "Termite2UI";
        File srcFile = new File(srcFilePath);

        String destFilePath = mTomcatPath + File.separator + "webapps" + File.separator + "Termite2UI";
        File destFile = new File(destFilePath);

        try {
            FileUtils.copyDirectory(srcFile, destFile,true);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        System.out.println("Termite2UI folder copied to tomcat webapps folder.");
        return true;
    }




    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
    /* * * * * * * * * * * * * * * * * * * * UI THREAD * * * * * * * * * * * * * * * * * * * * * * * */
    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private static class Termite2UIWorker implements Runnable{

        protected final Context mContext;

        protected Termite2UIWorker(Context context){
            mContext = context;
        }

        @Override
        public void run() {
            try (ServerSocket webPageServer = new ServerSocket(DEFAULT_SERVER_PORT)) {

                while (!webPageServer.isClosed()) {
                    try (Socket webClient = webPageServer.accept()) {

                        DataInputStream in = new DataInputStream(new BufferedInputStream(webClient.getInputStream()));
                        String msgReceived = in.readUTF();
                        String cleanedMsg = msgReceived.replaceAll("\"", "");
                        System.out.println("Message received from Termite2 GUI:" + msgReceived);

                        if (cleanedMsg.equals("startup")) {
                            // refresh emulators
                            refreshEmus();
                            String startMsg = trimStringResult(String.join(",", getEmulatorsFullData()));
                            DataOutputStream msgOut = new DataOutputStream(webClient.getOutputStream());
                            //sending message
                            msgOut.writeUTF(startMsg);
                            System.out.println("Start message sent to tomcat server: " + startMsg);
                            msgOut.close();
                        } else { // commit, startemu, stopemu or stopall commands
                            String result = processCommandsReceived(cleanedMsg);
                            if (result != null) {
                                try {
                                    DataOutputStream msgOut = new DataOutputStream(webClient.getOutputStream());
                                    //sending message
                                    msgOut.writeUTF(result);
                                    System.out.println("Result sent to Termite2 GUI: " + result);
                                    msgOut.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    } catch (IOException e) {
                        System.out.println("Problem occurred receiving message from Termite2 GUI.");
                        //e.printStackTrace(System.out);
                    }
                }

            } catch (IOException e) {
                System.out.println("Problem creating server socket.");
                e.printStackTrace(System.out);
            }
        }

        private String processCommandsReceived(String cmds){

            ArrayList<String> cmdsArray = new ArrayList<>(Arrays.asList(cmds.split(";")));
            String result = null;

            for (String line : cmdsArray) {
                //System.out.println("TESTESTE" + line);
                String[] tokens = line.split("\\s+");
                String cmd = tokens[0];

                boolean found = false;
                String cmdResult;
                for (Command command : mContext.getCommands()) {
                    if (command.getName().equals(cmd) || (
                            !command.getAbvr().equals(Command.NULLABVR) &&
                                    command.getAbvr().equals(cmd))) {
                        found = true;
                        cmdResult = command.executeCommand(mContext, tokens);
                        if (cmdResult.equals("OK")) {
                            if (cmd.equals("commit")) {
                                result = "Commit result:\n" + trimStringResult(String.join(",", getCommitResults()));
                            }
                            else if(cmd.equals("refresh") || cmd.equals("startemus") || cmd.equals("stopemu") || cmd.equals("stopall")){
                                result = trimStringResult(String.join(",", getEmulatorsFullData()));
                            }
                            else if(cmd.equals("installed")){
                                result = mContext.getLatestInstalledResult();
                            }
                            else{
                                result = "Commands processed";
                            }
                            mContext.getHistory().setLast(line);
                            System.out.println("Command from UI: " + line + " | PROCESSED");
                        }else{
                            result = cmdResult;
                            break;
                        }
                    }
                }
                if (!found)
                    System.out.println("Error: Command \"" + cmd + "\" does not exist.");
            }

            return result;
        }

        private void refreshEmus(){
            for (Command command : mContext.getCommands()) {
                if (command.getName().equals("refresh")){
                    command.executeCommand(mContext, null);
                }
            }
        }

        // returns: ["network_ip", "e1,emu_name;port;cport;mport", "e2;name;port;cport;mport", "2ºnetworkip", "emudata...", ....]
        private ArrayList<String> getEmulatorsFullData(){
            ArrayList<String> result = new ArrayList<>();
            ArrayList<String> networks = mContext.mConfigManager.getControllerNetworks();
            HashMap<String, Emulator> emulators = mContext.mCurrentEmulation.getEmusTracker().getEmusList();

            for( String net : networks){
                result.add(net);
                for (Map.Entry<String, Emulator> emulator : emulators.entrySet()) {
                    Emulator emu = emulator.getValue();
                    if(emu.getIp().equals(net)){
                        String emuData = emulator.getKey() + ";" + emu.dataForUi();
                        result.add(emuData);
                    }
                }
            }

            return result;
        }

        private String trimStringResult(String result){
            return result.replace("[", "")  //remove the right bracket
                    .replace("]", "")  //remove the left bracket
                    .trim();
        }

        private String getCommitResults(){
            ArrayList<String> results = mContext.getLastCommitResult();
            return String.join(",", results);
        }

    }


}


