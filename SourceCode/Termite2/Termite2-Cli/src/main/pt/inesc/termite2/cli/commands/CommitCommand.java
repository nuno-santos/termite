package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

public class CommitCommand extends Command {

    public CommitCommand() {
        super("commit", "c");
    }

    public String executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        Network network = context.mCurrentEmulation.getNetwork();
        Devices devices = network.getDevices();

        if (args.length != 1) {
            return returnError("Wrong number of input arguments.");
        }

        // nothing to do if true
        if (devices.numDevices() == 0) {
            return returnError("No devices.");
        }

        /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
        /* Create commitWorker threads
        * - This threads generate the commit messages for the emulators and group them based on controller ip
        * - Then the message is sent to the correct TermiteServer
        * */
        Hashtable<Thread,CommitWorker> workers = new Hashtable<Thread,CommitWorker>();
        for(String serverIp : context.mRemoteAVDController.getNetworksIps()){
            CommitWorker cw = new CommitWorker(serverIp,
                    context.mRemoteAVDController.getControllerConnection(serverIp),
                    devices,
                    network);
            Thread t = new Thread(cw);
            t.start();
            workers.put(t, cw);
        }

        // wait for the termination of all threads
        for (Thread t : workers.keySet()) {
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        }

        // Get the commit result from all CommitWorker threads
        ArrayList<ArrayList<String>> fullCommitResult = new ArrayList<>();
        for(CommitWorker worker : workers.values()) {
            ArrayList<String> commitResult;
            commitResult = worker.getCommitResults();
            if(commitResult != null){
                fullCommitResult.add(commitResult);
            }
        }

        ArrayList<String> mergedCommitResult = new ArrayList<>();
        if(fullCommitResult.size() == 0){
            System.out.println("Commit Results:");
            System.out.println("Warning: There is no binded devices.");
            mergedCommitResult.add("Warning: There is no binded devices.");
        }
        else{
            //print results
            System.out.println("Commit Results:");
            for(ArrayList<String> results : fullCommitResult){
                for(String result : results){
                    System.out.println("    " + result);
                    mergedCommitResult.add(result);
                }
            }
        }

        //save commit result (used on the ui)
        context.setLastCommitResult(mergedCommitResult);

        return OK;
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * *  Nested Class  * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    static class CommitWorker implements Runnable{

        private ControllerConnection mControllerConnection;
        ArrayList<String> mCommitResults;

        private String mGroupIp;
        private Devices mDevices;
        private Groups mGroups;
        private ArrayList<String> mMarshalled;

        public CommitWorker(String ip, ControllerConnection connection, Devices devices, Network network){
            mGroupIp = ip;
            mControllerConnection = connection;
            mDevices = devices;
            mGroups = network.getGroups();
            mMarshalled = new ArrayList<>();
            mCommitResults = new ArrayList<>();
        }

        public ArrayList<String> getCommitResults() {
            return mCommitResults;
        }

        @Override
        public void run() {
            for(Device device : mDevices.getDevices()){
                if(device.getIp().equals("0.0.0.0")){
                    System.out.println("Warning: Device " + device.getName() + " is not binded. Commit skipped for this device.");
                }
                else if(device.getIp().equals(mGroupIp)){
                    String marshaledInfo = "" + device.getCommitPort() + ";";
                    marshaledInfo = marshaledInfo + marshalDeviceInfo(device);
                    mMarshalled.add(marshaledInfo);
                }
            }
            //System.out.println("All message for network " + mGroupIp + " marshaled, result = " + mMarshalled.toString());

            if(mMarshalled.size() == 0){
                System.out.println("Network " + mGroupIp + " has no binded emulators");
                mCommitResults = null;
            }
            else{
                mMarshalled.add(0, "commit");
                try{
                    //System.out.println("Sending commit message message: " + mMarshalled.toString() + " ...");
                    mControllerConnection.getOut().writeObject(mMarshalled);
                    mControllerConnection.getOut().flush();
                    //System.out.println("Commit message message sent.\n Reading response...");

                    Object response = mControllerConnection.getIn().readObject();
                    mCommitResults = (ArrayList<String>) response;
                    //System.out.println("Response = " + mCommitResults.toString());

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        public String marshalDeviceInfo(Device device) {
            //System.out.println("marshalDeviceInfo.");
            StringBuilder sb = new StringBuilder();

            sb.append(mGroups.marshalDeviceGroups(device));
            //System.out.println("mGroups.marshalDeviceGroups(mDevice) = " + sb);
            sb.append("+");

            sb.append(mDevices.marshalDeviceNeighbors(device));
            //System.out.println("mDevices.marshalDeviceNeighbors(mDevice) = " + sb);
            sb.append("+");

            sb.append(mDevices.marshalDevices());
            //System.out.println("mDevices.marshalDevices() = " + sb);
            sb.append("+");
            return sb.toString();
        }

        public boolean isDeviceBinded(Device device){
            if(device.getIp().equals("0.0.0.0")){
                return false;
            }
            return true;
        }
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: commit");
    }

    public String getExplanation(){
        return "Commits the current termite2 network state to all binded emulators.";
    }
}
