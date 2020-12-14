package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.AddressSet;
import pt.inesc.termite.server.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;

public class PingCommand extends Command {

    public PingCommand() {
        super("ping");
    }

    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        System.out.println("Executing PingCommand...");

        String networkIp = AVDController.getNetworkIp();

        ArrayList<AddressSet> emulators = new ArrayList<>(AVDController.getAssignedEmulators().values());


        if (emulators.size() == 0) {
            return null;
        }

        Hashtable<Thread, PingHandler> pingHandlers = new Hashtable<>();
        for (AddressSet address : emulators) {
            PingHandler cH = new PingHandler(address.getRealCommitPort());
            Thread t = new Thread(cH);
            t.start();
            pingHandlers.put(t, cH);
        }

        // wait for the termination of all threads
        for (Thread t : pingHandlers.keySet()) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // display the result;
        ArrayList<String> pingResult = new ArrayList<>();
        for (PingHandler pingHandler : pingHandlers.values()) {

            String result = "";

            int emuPort = pingHandler.getEmuPort();
            String emuName = AVDController.getAssignedEmuName(emuPort);
            boolean success = pingHandler.getStatus();

            result = "" + emuName + " " + networkIp +
                    " " + (success ? "SUCCESS" : "FAIL");
            System.out.println(result);
            pingResult.add(result);
        }

        return pingResult;
    }

    @Override
    public String cmdSyntax() {
        return "ping";
    }

    @Override
    public String getExplanation() {
        return "Pings all online emulators to check if Wifi-Direct is online.";
    }

    public static class PingHandler implements Runnable {

        private static final String PING = "<PING>\n";

        private int emuPort;
        private boolean mSuccess = false;

        public PingHandler(Integer port) {
            this.emuPort = port;
            System.out.println("New PingHandler created. Emu target: " + emuPort);
        }

        public void run() {

            Socket EmuConnection;
            PrintWriter printwriter;

            try {
                System.out.println("Creating socket connection to emulator on port: " + emuPort + " ...");
                EmuConnection = new Socket("localhost", emuPort);

                // send ping message
                printwriter = new PrintWriter(EmuConnection.getOutputStream(), true);
                System.out.println("Sending ping message to emulator " + emuPort + " : " + PING);
                printwriter.write(PING);
                printwriter.flush();
                System.out.println("Ping sent.");

                System.out.println("Waiting for responses...");
                InputStreamReader inputStreamReader = new InputStreamReader(EmuConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String message = bufferedReader.readLine();

                // close the streams and the socket
                inputStreamReader.close();
                printwriter.close();
                EmuConnection.close();

                if (!"<OK>".equals(message)) {
                    System.out.println("Error: message unexpected '" + message + "'.");
                    mSuccess = false;
                } else {
                    mSuccess = true;
                }
            } catch (IOException e) {
                System.out.println("Error occurred when trying to send ping message to emulator on port: " + emuPort);
                //e.printStackTrace();
                mSuccess = false;
            }
        }

        public boolean getStatus() {
            return mSuccess;
        }

        public int getEmuPort() {
            return emuPort;
        }
    }

}
