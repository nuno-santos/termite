package main.pt.inesc.termite2.cli;

import main.pt.inesc.termite2.cli.exceptions.RemoteAVDControllerException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class RemoteAVDController {

    private ArrayList<String[]> mNetworksFullIps;
    private ArrayList<String> mNetworksIps;

    private HashMap<String, ControllerConnection> mSocketConnections; // Controller_Ip -> Socket_to_controller
    private ArrayList<Emulator> mEmulators; // Emulator_obj

    public RemoteAVDController(ArrayList<String> networksIps, ArrayList<String[]> fullips){
        mNetworksIps = networksIps;
        mNetworksFullIps = fullips;
        mSocketConnections = new HashMap<>();
        mEmulators = new ArrayList<>();
    }

    /*
     * Setup commands and gets
     */

    public void startSocketConnections() throws RemoteAVDControllerException {

        for (String[] address: mNetworksFullIps){
            String ip = address[0];
            int port = Integer.parseInt(address[1]);
            System.out.println("Establishing socket connections to Termite2 Server on '" +ip+ ":" +port+ "'.");
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 10000);
                mSocketConnections.put(ip, new ControllerConnection(socket));
            } catch (IOException e) {
                //e.printStackTrace();
                throw new RemoteAVDControllerException("Error: Termite2 Server on '" +ip+ ":" +port+ "' not reachable, socket connection not created.");
            }
        }
        System.out.println("Connection/s to Termite2 Server/s established without errors.");
    }

    public void closeConnections(){
        for(ControllerConnection connection : mSocketConnections.values()){
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<Emulator> getEmulators(){
        return mEmulators;
    }

    public ArrayList<String> getNetworksIps() {
        return mNetworksIps;
    }

    public ControllerConnection getControllerConnection(String ip){
        return mSocketConnections.get(ip);
    }

    /*
     * Emulators operations
     */

    public String createAvds(String ip, int num, String name, int api){
        // create command request object
        ArrayList<String> request = new ArrayList<>();
        request.add("createavds");
        request.add(""+num);
        request.add(name);
        request.add(""+api);

        ArrayList<String> result = sendMessageToTermiteServer(ip, request);
        if(result == null ){
            return "Error: Failed to send command request to termite2 server " + ip + ".";
        }
        return result.toString();
    }

    public String destroyAvds(String ip, ArrayList<String> avdsNames){
        ArrayList<String> request = new ArrayList<>();
        request.add("destroyavds");
        request.addAll(avdsNames);

        ArrayList<String> result = sendMessageToTermiteServer(ip, request);
        if(result == null ){
            return "Error: Failed to send command request to termite2 server " + ip + ".";
        }
        if(arrayListToString(result).contains("Error")){
            return arrayListToString(result);
        }

        String sresult = "";
        for(String r : result){
            sresult = sresult + r + "\n";
        }
        return sresult;
    }

    public String getInstalled(String ip){
        // create command request object
        ArrayList<String> request = new ArrayList<>();
        request.add("installed");

        ArrayList<String> result = sendMessageToTermiteServer(ip, request);
        if(result == null ){
            return "Error: Failed to send command request to termite2 server " + ip + ".";
        }
        return result.toString();

    }

    public String startEmulators(String ip, String numEmus, String appPackage) {

        // create command request object
        ArrayList<String> request = new ArrayList<>();
        request.add("startemus");
        request.add(numEmus);
        if(appPackage.length() != 0){
            request.add(appPackage);
        }

        ArrayList<String> result = sendMessageToTermiteServer(ip, request);
        if(result == null ){
            return "Error: Failed to send startemus command request to termite2 server.";
        }
        else if(arrayListToString(result).contains("Error")){
            return arrayListToString(result);
        }
        else{
            ArrayList<String> emulators = new ArrayList<>(result);
            removeEmulatorForNet(ip);
            createEmulatorObjects(emulators);
            return "OK";
        }
    }

    public String stopEmulator(String ip, int emuPort) {

        // create command request object
        ArrayList<String> request = new ArrayList<>();
        request.add("stopemu");
        request.add(""+emuPort);

        ArrayList<String> result = sendMessageToTermiteServer(ip, request);
        if(result == null ){
            return "Error: Failed to send command request to termite2 server " + ip + ".";
        }
        else if(arrayListToString(result).contains("Error")){
            return arrayListToString(result);
        }else{
            ArrayList<String> emulators = new ArrayList<>(result);
            removeEmulatorForNet(ip);
            createEmulatorObjects(emulators);
            return "OK";
        }
    }

    public String stopAllEmulators(String ip) {
        ArrayList<String> request = new ArrayList<>();
        request.add("stopemu");
        request.add("all");

        ArrayList<String> result = sendMessageToTermiteServer(ip, request);
        if(result == null ){
            return "Error: Failed to send command request to termite2 server " + ip + ".";
        }
        else if(arrayListToString(result).contains("Error")){
            return arrayListToString(result);
        }else{
            //removeEmulatorForNet(ip);
            return "OK";
        }
    }

    public String installApplication(String ip, String apkName, ArrayList<String> emus){
        ArrayList<String> request = new ArrayList<>();
        request.add("installapp");
        request.add(apkName);
        request.addAll(emus);

        ArrayList<String> result = sendMessageToTermiteServer(ip, request);
        if(result == null ){
            return "Error: Failed to send command request to termite2 server " + ip + ".";
        }

        String sresult = "";
        for(int i = 0; i < result.size(); i++){
            sresult = sresult + result.get(i);
            if(i != result.size()-1){
                sresult = sresult + "\n";
            }
        }
        return sresult;
    }

    public String startApplication(String ip, String appPackage, ArrayList<String> emus){
        ArrayList<String> request = new ArrayList<>();
        request.add("startapp");
        request.add(appPackage);
        request.addAll(emus);

        ArrayList<String> result = sendMessageToTermiteServer(ip, request);
        if(result == null ){
            return "Error: Failed to send command request to termite2 server " + ip + ".";
        }

        String sresult = "";
        for(int i = 0; i < result.size(); i++){
            sresult = sresult + result.get(i);
            if(i != result.size()-1){
                sresult = sresult + "\n";
            }
        }
        return sresult;
    }

    public String runScriptFile(String ip, String scriptName, ArrayList<String> args){
        ArrayList<String> request = new ArrayList<>();
        request.add("runscript");
        request.add(scriptName);
        if(args != null){
            request.addAll(args);
        }


        ArrayList<String> result = sendMessageToTermiteServer(ip, request);
        if(result == null ){
            return "Error: Failed to send command request to termite2 server " + ip + ".";
        }

        String sresult = "";
        for(int i = 0; i < request.size(); i++){
            sresult = sresult + request.get(i);
            if(i != request.size()-1){
                sresult = sresult + "\n";
            }
        }
        return sresult;
    }

    public boolean refreshEmulators() { // Acts has refresh
        ArrayList<String> emulators = new ArrayList<>();
        ArrayList<String> request =  new ArrayList<>();
        request.add("refresh");

        for(String ip : mSocketConnections.keySet()){
            ArrayList<String> result = new ArrayList<>();
            result = sendMessageToTermiteServer(ip, request);
            if(result == null ){
                System.out.println("Connection Error. Emulators instances running on controller " + ip + " not refreshed.");
            }
            else if(arrayListToString(result).contains("Error")){
                System.out.println(arrayListToString(result) + ". Emulators instances running on controller \" + ip + \" not refreshed.");
            }else{
                emulators.addAll(result);
            }
        }

        mEmulators.clear();
        createEmulatorObjects(emulators);
        return true;
    }

    /*
     * Send Command used to send command messages to the seletec controller
     */

    private ArrayList<String> sendMessageToTermiteServer(String ip, ArrayList<String> cmdMsg){
        try {
            mSocketConnections.get(ip).getOut().writeObject(cmdMsg);
            mSocketConnections.get(ip).getOut().flush();
            System.out.println("Command " + cmdMsg.toString() + " sent to termite2 server \"" + ip + "\" ...");

            Object obj = mSocketConnections.get(ip).getIn().readObject();
            return (ArrayList<String>) obj;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * *   Helper methods  * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    // allEmus = [ (serverIp -> "emu_name|emuPort|emuIP|commitPort|messagePort"), (...), (...) ]
    private void createEmulatorObjects(ArrayList<String> allEmus){
        //System.out.println("createEmulatorObjects: " + allEmus.toString());
        for(String emu : allEmus){
            //System.out.println("Emu: " + emu);
            String[] data = emu.split("\\|"); // [Pixel_API_21, 5554, 192.168.1.15, 9011, 10011]
            Emulator newEmu = new Emulator(data[0], // Emu_name
                    Integer.parseInt(data[1]),      // Emu_ip
                    data[2],                        // Emu_port
                    Integer.parseInt(data[3]),      // Emu_commit_port
                    Integer.parseInt(data[4]));     // Emu_message_port

            mEmulators.add(newEmu);
        }
    }

    public void removeEmulatorForNet(String ip){
        mEmulators.removeIf(emulator -> emulator.getIp().equals(ip));
    }

    private String arrayListToString(ArrayList<String> array){
        return Arrays.toString(array.toArray()).replace("[", "").replace("]", "");
    }
}
