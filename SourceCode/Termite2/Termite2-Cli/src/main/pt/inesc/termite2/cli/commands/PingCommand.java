package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class PingCommand extends Command {

    public PingCommand() {
        super("ping", Command.NULLABVR);
    }

    public String executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (args.length != 1) {
            return returnError("Wrong number of input arguments.");
        }

        // nothing to do if true
        if (context.mRemoteAVDController.getEmulators().size() == 0) {
            return returnError("No emulator instances to ping");
        }

        /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
        /* Create pingWorker threads
         * */
        Hashtable<Thread, PingWorker> workers = new Hashtable<Thread, PingWorker>();
        for(String serverIp : context.mRemoteAVDController.getNetworksIps()){
            // We only send ping command to the controllers that are running binded emulators
            if(emulatorExistsOn(serverIp, context)){
                PingWorker pw = new PingWorker(serverIp, context.mRemoteAVDController.getControllerConnection(serverIp));
                Thread t = new Thread(pw);
                t.start();
                workers.put(t, pw);
            }
        }

        // wait for the termination of all threads
        for (Thread t : workers.keySet()) {
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        }

        // Get the ping result from all PingWorker threads
        ArrayList<ArrayList<String>> fullPingResult = new ArrayList<>();
        for(PingWorker worker : workers.values()) {
            ArrayList<String> pingResult = worker.getCommitResults();
            fullPingResult.add(pingResult);
        }

        //print results
        System.out.println("Commit Results:");
        ArrayList<String> mergedCommitResult = new ArrayList<>();
        for(ArrayList<String> results : fullPingResult){
            for(String result : results){
                System.out.println("    " + formatPingResponse(result, context));
            }
        }
        return OK;
    }

    private boolean emulatorExistsOn(String ip, Context context){
        for(Emulator emulator : context.mCurrentEmulation.getEmusTracker().getEmusList().values()){
            if(emulator.getIp().equals(ip))
                return true;
        }
        return false;
    }

    private String formatPingResponse(String result, Context context){
        String[] splitResult = result.split("\\s+"); // emuName,EmuIP,success?
        for (Map.Entry<String, Emulator> entry : context.mCurrentEmulation.getEmusTracker().getEmusList().entrySet()) {
            Emulator emu = entry.getValue();
            if(emu.getName().equals(splitResult[0]) && emu.getIp().equals(splitResult[1])){
                return "" + entry.getKey() + " (" + splitResult[0] + " " + splitResult[1] + ") - " + splitResult[2];
            }
        }
        return result;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * *  Nested Class  * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    static class PingWorker implements Runnable{

        private String mControllerIp;
        private ControllerConnection mControllerConnection;
        private ArrayList<String> mPingResults;

        public PingWorker(String ip, ControllerConnection connection){
            mControllerIp = ip;
            mControllerConnection = connection;
            mPingResults = new ArrayList<>();
        }

        public ArrayList<String> getCommitResults() {
            return mPingResults;
        }

        @Override
        public void run() {

            ArrayList<String> mPingMsg = new ArrayList<>();
            mPingMsg.add("ping");

            try{
                System.out.println("Sending ping message to controller: " + mControllerIp + " ...");
                mControllerConnection.getOut().writeObject(mPingMsg);
                mControllerConnection.getOut().flush();

                Object response = mControllerConnection.getIn().readObject();
                mPingResults = (ArrayList<String>) response;
                //System.out.println("Response = " + mPingResults.toString());

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void printHelp() {
        System.out.println("Syntax: ping");
    }

    public String getExplanation(){
        return "Pings all currently registered emulators to check if wifi-direct is on.";
    }
}
