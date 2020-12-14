package pt.inesc.termite.server.receiversthreads;

import pt.inesc.termite.server.exceptions.ReceiverThreadException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerReceiverThread extends Thread {

    private int SERVER_PORT;
    private InetAddress localIpAddress;

    private ServerSocket messageReceiverS;

    public ServerReceiverThread(InetAddress localAddress, int messagePort) throws ReceiverThreadException {
        this.localIpAddress = localAddress;
        this.SERVER_PORT = messagePort;
        try {
            messageReceiverS = new ServerSocket(SERVER_PORT, 0, localIpAddress);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ReceiverThreadException("Error: Problem occurred when creating ServerReceiver server socket. " +
                    "Invalid Ip address " + localIpAddress + ":" + SERVER_PORT + " .");
        }
    }

    public void run() {
        System.out.println("    ServerReceiver started. Listening on " + localIpAddress + ":" + SERVER_PORT + " ...");

        try {
            while (!messageReceiverS.isClosed()) {
                // socket object to receive incoming client requests
                Socket controllerConnection = messageReceiverS.accept();
                System.out.println("ServerReceiver: Connection received from external Termite2 Server : " + controllerConnection);
                System.out.println("ServerReceiver: Assigning new connection thread for this termite2 server.");
                Thread receiverHandler = new ServerReceiverHandler(controllerConnection);
                receiverHandler.start();
            }
        } catch (IOException e) {
            System.out.println("ServerReceiver Error: Problem trying to create connection to external termite2 server.");
            e.printStackTrace();
        }
    }


    /*
     *
     * Handler thread class
     *
     * */
    public static class ServerReceiverHandler extends Thread {

        private String TAG = "ServerReceiverHandler";
        private String THREAD_ID ="";
        private static final String ERROR = "ERROR";
        private static final String OK = "OK";

        Socket controllerConnection;
        ObjectOutputStream out;
        ObjectInputStream in;

        Socket emuConnection;
        ObjectOutputStream emuOut;
        ObjectInputStream emuIn;

        public ServerReceiverHandler(Socket controllerSocket) {
            this.controllerConnection = controllerSocket;
            try {
                out = new ObjectOutputStream(controllerConnection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(controllerConnection.getInputStream());
            } catch (IOException e) {
                printMsg(THREAD_ID, TAG,"Error: Not able to create communication streams with external termite2 server.");
                e.printStackTrace();
            }
        }

        public void run() {
            THREAD_ID = Thread.currentThread().getName();

            if (openEmulatorConnection()) {

                while (!controllerConnection.isClosed() && !emuConnection.isClosed()) {
                    printMsg(THREAD_ID, TAG,"Reading subsequent messages from external termite2 server...");
                    try {

                        // Read new message content from external termite2 server
                        Object object = in.readObject();
                        printMsg(THREAD_ID, TAG,"message received: \"" + object.toString() +"\".");

                        // Redirect message to  emu
                        printMsg(THREAD_ID, TAG,"Redirecting message to emulator...");
                        emuOut.writeObject(object);
                        emuOut.flush();
                        printMsg(THREAD_ID, TAG,"Redirection done.");

                    } catch (IOException | ClassNotFoundException e) {
                        printMsg(THREAD_ID, TAG,"Error: problem found when waiting for new messages and redirecting them to the registered emulator, probable cause socket closed on emulator.");
                        closeAllSockets();
                        //e.printStackTrace();
                        return;
                    }
                }
                closeAllSockets();
                printMsg(THREAD_ID, TAG, "finished.");

            } else {
                try {
                    out.writeObject(ERROR);
                    out.flush();
                    printMsg(THREAD_ID, TAG,"ERROR message sent to external termite2 server.");
                } catch (IOException e) {
                    printMsg(THREAD_ID, TAG,"Error: Not able to send error message to external termite2 server.");
                    e.printStackTrace();
                }
            }
            closeAllSockets();
        }

        private void startResponseChannel(Socket connection, ObjectInputStream in, ObjectOutputStream out) {
            Thread responseChannelThread = new ReceiverResponseThread(connection, in, out, THREAD_ID);
            responseChannelThread.start();
            printMsg(THREAD_ID, TAG,"ReceiverResponseThread started....");
        }

        private boolean openEmulatorConnection() {

            try {
                // 1st we read register port destination message, this tells the emulator were we need to send all incoming message after this initial one
                Object object = in.readObject();
                String portString = (String) object;
                int emuPort = Integer.parseInt(portString);

                printMsg(THREAD_ID, TAG,"Opening socket connection to emulator on localhost port:" + emuPort + " ...");
                emuConnection = new Socket("localhost", emuPort);
                emuOut = new ObjectOutputStream(emuConnection.getOutputStream());
                printMsg(THREAD_ID, TAG,"Socket connection to emulator opened with success.");

                out.writeObject(OK);
                out.flush();
                printMsg(THREAD_ID, TAG, "OK message sent to external termite2 server.");

                emuOut.flush();
                emuIn = new ObjectInputStream(emuConnection.getInputStream());
                printMsg(THREAD_ID, TAG, "Connection with between external termite2 server and emulator on port " + portString + " established, opening ReceiverResponseThread channel.");

                startResponseChannel(emuConnection, emuIn, out);
                return true;

            } catch (IOException | ClassNotFoundException e) {
                printMsg(THREAD_ID, TAG, "Error: Not able to set socket connection with emulator.");
                e.printStackTrace();
                return false;
            }

        }

        private void closeAllSockets() {
            if (controllerConnection != null && !controllerConnection.isClosed()) {
                try {
                    controllerConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (emuConnection != null && !emuConnection.isClosed()) {
                try {
                    emuConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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

            Socket emuConnection;
            ObjectInputStream inResponseEmu;
            ObjectOutputStream outResponseController;

            public ReceiverResponseThread(Socket connection, ObjectInputStream in, ObjectOutputStream out, String id) {
                emuConnection = connection;
                inResponseEmu = in;
                outResponseController = out;
                THREAD_ID = id;
            }

            @Override
            public void run() {
                while (!emuConnection.isClosed()) {
                    try {
                        printMsg(THREAD_ID, TAG, "Waiting for emulator responses...");
                        Object response = inResponseEmu.readObject();

                        printMsg(THREAD_ID, TAG, "Response received from emulator sending it back to external termite2 server....");

                        outResponseController.writeObject(response);
                        outResponseController.flush();
                    } catch (IOException | ClassNotFoundException e) {
                        printMsg(THREAD_ID, TAG, "Error: Lost socket connection to external emulator.");
                        return;
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





/* Explanation:
 * This component is responsible for receiving connections from external termite2 server that want to redirect message to a local emulator, operation is done has follows:
 * - First open socket connection on ( network_ip : 8095 ).
 * - Them when a external termite2 server connects we create a thread with the socket connection and open communication streams on the constructor.
 * - This thread now will handle this external termite2 server interaction until the end, while the main thread keeps looking for other connections.
 *
 * - When the handler thread start we first wait for the port register message (port of the emulator that the external server is trying to send the message) .
 * - When the port message is received we try to establish connection to the emulator-PORT.
 *   - If the port is not valid we send an ERROR message and close the connection.
 *   - If the port is valid, then we open a socket connection to the emulator ( localhost : port_received ).
 *   - From now on we redirect all subsequent messages to this emulator.
 *   - This message redirection continues until the socket is closed or an error occurs.
 * */