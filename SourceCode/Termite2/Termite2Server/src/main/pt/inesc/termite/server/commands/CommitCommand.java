package pt.inesc.termite.server.commands;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;

public class CommitCommand extends Command {

    public CommitCommand() {
        super("commit");
    }

    public ArrayList<String> execute(ArrayList<String> msg, AVDControllerDriver AVDController) {
        System.out.println("Executing CommitCommand...");

        String networkIp = AVDController.getNetworkIp();

        if (AVDController.getAssignedEmulators().size() == 0) {
            return null;
        }

        Hashtable<Thread, CommitHandler> commitHandlers = new Hashtable<Thread, CommitHandler>();
        for (String commit : msg) {
            CommitHandler cH = new CommitHandler(commit);
            Thread t = new Thread(cH);
            t.start();
            commitHandlers.put(t, cH);
        }

        // wait for the termination of all threads
        for (Thread t : commitHandlers.keySet()) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // display the result;
        ArrayList<String> commitResult = new ArrayList<String>();
        for (CommitHandler commitHandler : commitHandlers.values()) {

            String result = "";

            int emuCommitPort = commitHandler.getEmuPort();
            String deviceId = commitHandler.getDeviceId();
            String emuName = AVDController.getAssignedEmuName(emuCommitPort);
            boolean success = commitHandler.getStatus();

            // a ( Pixel_API_21 192.168.1.250 ) - success?
            result = "" + deviceId + " (" + emuName + " " + networkIp +
                    ") - " + (success ? "SUCCESS" : "FAIL");
            System.out.println(result);
            commitResult.add(result);
        }

        return commitResult;
    }

    @Override
    public String cmdSyntax() {
        return "commit <commit_msg>";
    }

    @Override
    public String getExplanation() {
        return "Commits the network state received from termite2-cli to the binded emulators on this machine.";
    }

    public static class CommitHandler implements Runnable {

        private int mEmuPort;
        private String mCommitMsg;
        private String mDeviceId;

        private boolean mSuccess = false;

        public CommitHandler(String commit) {
            String[] split = commit.split(";");
            mEmuPort = Integer.parseInt(split[0]);
            mCommitMsg = split[1] + "\n";
            mDeviceId = mCommitMsg.substring(0, 1);
            System.out.println("New commitHandler created for target emu on port: " + mEmuPort);
        }

        public void run() {

            Socket EmuConnection;
            PrintWriter printwriter;

            try {
                System.out.println("Creating socket connection to emulator on port: " + mEmuPort + " ...");
                EmuConnection = new Socket("localhost", mEmuPort);

                // push device info to the emulator
                printwriter = new PrintWriter(EmuConnection.getOutputStream(), true);
                System.out.println("Sending commit message to emulator " + mEmuPort);
                printwriter.write(mCommitMsg);
                printwriter.flush();

                System.out.println("Waiting for responses...");
                // receive the acknowledgment (OK)
                InputStreamReader inputStreamReader = new InputStreamReader(EmuConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String message = bufferedReader.readLine();

                // close the streams and the socket
                inputStreamReader.close();
                printwriter.close();
                EmuConnection.close();

                if (!"<OK>".equals(message)) {
                    System.out.println("Error: commit message response from emulator with unexpected result: '" + message + "'.");
                    mSuccess = false;
                } else {
                    mSuccess = true;
                }

            } catch (IOException e) {
                System.out.println("Error occurred when trying to send commit message to emulator on port: " + mEmuPort);
                e.printStackTrace();
                mSuccess = false;
            }
        }

        public boolean getStatus() {
            return mSuccess;
        }

        public int getEmuPort() {
            return mEmuPort;
        }

        public String getDeviceId() {
            return mDeviceId;
        }

    }
}
