package pt.inesc.termite.server.receiversthreads;

import pt.inesc.termite.server.AVDControllerDriver;
import pt.inesc.termite.server.Command;
import pt.inesc.termite.server.commands.*;
import pt.inesc.termite.server.exceptions.ReceiverThreadException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ClientReceiverThread extends Thread {

    Command[] Commands = {
            new PingCommand(),
            new CommitCommand(),
            new RefreshEmulatorsCommand(),
            new InstalledEmulatorsCommand(),
            new CreateAvdsCommand(),
            new DestroyAvdsCommand(),
            new StartEmulatorsCommand(),
            new StopEmulatorCommand(),
            new InstallApplicationCommand(),
            new StartApplicationCommand(),
            new RunScriptCommand()
    };
    private int SERVER_PORT;
    private InetAddress localIpAddress;
    private AVDControllerDriver AVDController;
    private ServerSocket commitServer;

    public ClientReceiverThread(InetAddress localAddress, int commitPort, AVDControllerDriver AVDController) throws ReceiverThreadException {
        this.localIpAddress = localAddress;
        this.SERVER_PORT = commitPort;
        this.AVDController = AVDController;

        try {
            commitServer = new ServerSocket(SERVER_PORT, 0, localIpAddress);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ReceiverThreadException("Error: Problem occurred when creating ClientReceiver server socket. " +
                    "Invalid Ip address " + localIpAddress + ":" + SERVER_PORT + " .");
        }
    }

    @SuppressWarnings("unchecked")
    public void run() {
        System.out.println("    ClientReceiver started. Listening on " + localIpAddress + ":" + SERVER_PORT + " ...");

        ArrayList<String> messageReceived = new ArrayList<String>();
        ArrayList<String> response = new ArrayList<String>();

        while (!commitServer.isClosed()) {
            try (Socket commitClient = commitServer.accept()) {
                System.out.println("ClientReceiver: Connection to Termite-Cli established.");

                ObjectOutputStream out = new ObjectOutputStream(commitClient.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(commitClient.getInputStream());

                while (!commitClient.isClosed()) { //interact with termite-cli until connection closes.

                    System.out.println("ClientReceiver: Waiting for Termite2-Cli messages...");
                    Object object = in.readObject();
                    messageReceived = (ArrayList<String>) object;
                    System.out.println("\nClientReceiver: Message received: " + messageReceived.toString());

                    if (messageReceived.size() == 0) {
                        System.out.println("ClientReceiver: Message received with size 0 on termite2 server.");
                        response.add("Error: Message received with size 0 on termite2 server " + AVDController.getNetworkIp());
                        out.writeObject(response);
                    } else {
                        response = processMessage(messageReceived);
                        System.out.println("ClientReceiver: Response to termite2-Cli: " + response.toString());
                        out.writeObject(response);
                    }
                    out.flush();
                }

            } catch (IOException e) {
                System.out.println("\nAlert: Connection to Termite2-Cli lost.\n");
                //e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("Error: Unrecognized object received from termite2-Cli. Continue operation.");
                //e.printStackTrace();
            }
        }
    }

    private ArrayList<String> processMessage(ArrayList<String> message) {
        System.out.println("Processing message...");

        ArrayList<String> response = null;
        String operation = message.get(0);

        /*  This removes the command name from the message, so if we receive ["commit", "emuPort;commitMsg", "emuPort2;commitMsg", ...]
            we only process ["emuPort;commitMsg", "emuPort2;commitMsg" ...] */
        message.remove(0);
        for (Command command : Commands) {
            if (command.getName().equals(operation)) {
                response = command.execute(message, AVDController);
            }
        }

        if (response == null) {
            response = new ArrayList<>();
            response.add("Error");
        }
        return response;
    }

}

