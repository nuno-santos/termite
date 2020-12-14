package pt.inesc.termite.server.receiversthreads;


import pt.inesc.termite.server.exceptions.ReceiverThreadException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class LocalReceiverThread extends Thread {

    private int SERVER_PORT;
    private int MESSAGE_PORT;
    private ServerSocket localReceiver;

    public LocalReceiverThread(int localPort, int messagePort) throws ReceiverThreadException {
        this.SERVER_PORT = localPort;
        this.MESSAGE_PORT = messagePort;
        try {
            localReceiver = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ReceiverThreadException("Error: Problem occurred when creating LocalReceiverT server socket localhost:" + SERVER_PORT + " .");
        }
    }

    public void run() {
        System.out.println("    LocalReceiver started. Listening on localhost:" + SERVER_PORT + " ...");

        try {
            while (!localReceiver.isClosed()) {
                // socket object to receive incoming client requests
                Socket EmuConnection = localReceiver.accept();
                System.out.println("LocalReceiver: A new emulator is trying to connect : " + EmuConnection);
                System.out.println("LocalReceiver: Assigning new thread for this emulator.");
                Thread receiverHandler = new LocalReceiverHandler(EmuConnection, MESSAGE_PORT);
                receiverHandler.start();
            }
        } catch (IOException e) {
            System.out.println("LocalReceiver Error: Connection problem with the emulator.");
            e.printStackTrace();
        }
    }

    /*
     *
     * Handler thread class
     *
     * */
    public static class LocalReceiverHandler extends Thread {

        private String TAG = "LocalReceiverHandler";
        private String THREAD_ID ="";

        private static final String ERROR = "ERROR";
        private static final String OK = "OK";
        private int target_server_port;
        private Socket socket;
        private ObjectOutputStream out = null;
        private ObjectInputStream in = null;

        private String destIp;
        private String destPort;

        private Socket destinationSocket;
        private ObjectOutputStream destObjectOutput = null;
        private ObjectInputStream destObjectInput = null;

        public LocalReceiverHandler(Socket socket, int messagePort) {
            this.socket = socket;
            this.target_server_port = messagePort;
            //We create incoming communication streams
            try {
                System.out.println("Creating communication streams...");
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("Communication streams created.");
            } catch (IOException e) {
                System.out.println("Error receiveRegisterDestination(): Not able create communication streams from incoming emulator connection.");
                e.printStackTrace();
            }
        }

        public void run() {
            THREAD_ID = Thread.currentThread().getName();

            if (RegisterDestination()) {
                while (!socket.isClosed() && !destinationSocket.isClosed()) {
                    printMsg(THREAD_ID, TAG,"Reading subsequent messages from local emulator...");
                    try {
                        // Read new message content
                        Object object = in.readObject();
                        printMsg(THREAD_ID, TAG,"Message received: " + object.toString());

                        // Redirect message to destination
                        printMsg(THREAD_ID, TAG,"Redirecting message to destination...");
                        destObjectOutput.writeObject(object);
                        destObjectOutput.flush();
                        printMsg(THREAD_ID, TAG,"Redirection done.");

                    } catch (IOException | ClassNotFoundException e) {
                        closeAllSockets();
                        printMsg(THREAD_ID, TAG,"Error: Lost socket connection to local emulator.");
                        //e.printStackTrace();
                        return;
                    }
                }
                closeAllSockets();
                printMsg(THREAD_ID, TAG,"finished.");
            } else {
                printMsg(THREAD_ID, TAG,"Error destination registration failed.");
                closeAllSockets();
            }
        }

        private void startResponseChannel(Socket controller, Socket emu, ObjectInputStream in, ObjectOutputStream out) {
            Thread responseChannelThread = new ReceiverResponseThread(controller, emu, in, out, THREAD_ID);
            responseChannelThread.start();
            printMsg(THREAD_ID, TAG,"ReceiverResponseThread started....");
        }

        private boolean RegisterDestination() {
            printMsg(THREAD_ID, TAG,"Registering destination...");

            try {
                // 1st we read register destination message, this tells us were to send all incoming message after this initial one
                Object object = in.readObject();
                String register = (String) object;
                printMsg(THREAD_ID, TAG,"Register message received with = " + register);

                String[] fullIp = getIpAndPort(register);
                if (fullIp == null) {
                    return false;
                }
                this.destIp = fullIp[0];
                this.destPort = fullIp[1];

                printMsg(THREAD_ID, TAG,"Starting destination registration sequence for external server IP =  " + destIp + " and emulator on PORT = " + destPort);
                if (openExternalControllerConnection(destIp)) { // If connection to external termite2 server True
                    if (registerPortOnExternalController(destPort)) { // If port registering on external termite servef True
                        printMsg(THREAD_ID, TAG,"Registration sequence done with success.");
                        return true;
                    }
                }

                out.writeObject(ERROR);
                out.flush();
                return false;

            } catch (IOException | ClassNotFoundException e) {
                printMsg(THREAD_ID, TAG,"Error: Not able to read incoming termite2 server connection.");
                e.printStackTrace();
                return false;
            }
        }

        private boolean openExternalControllerConnection(String ip) {
            printMsg(THREAD_ID, TAG, "Opening connection to external server destination on ip \"" + ip + "\"...");
            try {
                //We create the external termite2 server destination connection for all incoming messages on this local receiver thread
                InetAddress destAddress = InetAddress.getByName(destIp);
                destinationSocket = new Socket(destAddress, target_server_port);
                printMsg(THREAD_ID, TAG,"Socket connection to remote termite2 server on " + ip + ":8095 created.");
                destObjectOutput = new ObjectOutputStream(destinationSocket.getOutputStream());
                destObjectOutput.flush();
                destObjectInput = new ObjectInputStream(destinationSocket.getInputStream());

                return true;
            } catch (IOException e) {
                printMsg(THREAD_ID, TAG,"An Error occurred when trying to establish connection to remote termite2 server on " + ip + ":8095");
                //e.printStackTrace();
                return false;
            }
        }

        private boolean registerPortOnExternalController(String port) {
            printMsg(THREAD_ID, TAG," trying to register connection to port \"" + port + "\" on external emulator...");
            try {
                // we send port destination message to termite2 server target
                destObjectOutput.writeObject(destPort);
                destObjectOutput.flush();

                // We wait for ok response from termite2 server target
                printMsg(THREAD_ID, TAG,"Waiting for response...");
                Object destResponse = destObjectInput.readObject();
                if (!destResponse.equals(OK)) {
                    printMsg(THREAD_ID, TAG,"Error: Invalid registration result for destination port " + port + " on remote termite2 server.");
                    return false;
                }

                printMsg(THREAD_ID, TAG,"Response " + destResponse + " received.");
                startResponseChannel(socket, destinationSocket, destObjectInput, out);
                return true;

            } catch (IOException | ClassNotFoundException e) {
                printMsg(THREAD_ID, TAG,"Error: Connection problem occurred when trying to register emu port: " + port + " destination on remote termite2 server.");
                return false;
            }
        }

        private void closeAllSockets() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (destinationSocket != null && !destinationSocket.isClosed()) {
                try {
                    destinationSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String[] getIpAndPort(String fullIp) {
            String[] split = fullIp.split(":"); // expected fullIp in the form of "ip:port"
            if (split.length != 2) {
                return null;
            }
            return split;
        }

        private void printMsg(String id, String tag, String msg){
            String print = "[" + tag + " " + id + "] - " + msg;
            System.out.println(print);
        }

        /*
         *
         * Handler response thread class
         *
         * */

        private static class ReceiverResponseThread extends Thread {
            private String TAG = "ReceiverResponseThread";
            private String THREAD_ID ="";

            Socket controllerConnection;
            Socket emuConnection;
            ObjectInputStream inResponse;
            ObjectOutputStream outResponse;

            public ReceiverResponseThread(Socket controller, Socket emu, ObjectInputStream in, ObjectOutputStream out, String id) {
                controllerConnection = controller;
                emuConnection = emu;
                inResponse = in;
                outResponse = out;
                THREAD_ID = id;
            }

            @Override
            public void run() {
                while (!controllerConnection.isClosed() && !emuConnection.isClosed()) {
                    try {
                        printMsg(THREAD_ID, TAG,"Waiting for response from external termite2 server....");
                        Object response = inResponse.readObject();

                        printMsg(THREAD_ID, TAG,"Response received from external termite2 server sending it back to emulator...");

                        outResponse.writeObject(response);
                        outResponse.flush();
                        printMsg(THREAD_ID, TAG,"Response sent back to emulator.");
                    } catch (IOException | ClassNotFoundException e) {
                        //e.printStackTrace();
                        printMsg(THREAD_ID, TAG,"Error: Lost socket connection to external termite2 server.");
                    }
                }
            }

            private void printMsg(String id, String tag, String msg){
                String print = "[" + tag + " " + id + "] - " + msg;
                System.out.println(print);
            }

        }
    }

}


/* Explanation
- receive connection from local emulator
- first message registers destination of termite2 server ip and port of target emulator
- we open connection to external termite2 server ip and send emulator port
- wait for response if of we keep input connection open for subsequent messages
- and redirenct incoming responses from target termite2 server to emulator requesting message forwarding
- this must be handled on diferent threads
* */