package pt.inesc.termite.server;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;
import pt.inesc.termite.server.exceptions.ConfigErrorException;
import pt.inesc.termite.server.exceptions.ReceiverThreadException;
import pt.inesc.termite.server.receiversthreads.ClientReceiverThread;
import pt.inesc.termite.server.receiversthreads.LocalReceiverThread;
import pt.inesc.termite.server.receiversthreads.ServerReceiverThread;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        AVDControllerDriver AVDController = null;

        try {
            /*
             * Process initial configuration
             */
            ConfigController.loadConfiguration();
            AVDController = new AVDControllerDriver(ConfigController.getLocalNetworkIp(),
                    ConfigController.getAndroidSDKPath(),
                    ConfigController.getServerPlatform(),
                    ConfigController.getTermiteServerPath());

        } catch (ConfigErrorException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }

        /*
         * Creates and starts receivers threads.
         */
        try {
            System.out.println("Creating receiver Threads:");

            ClientReceiverThread commitT = new ClientReceiverThread(ConfigController.getLocalInetAddress(), ConfigController.getClientPort(), AVDController);
            commitT.start();

            ServerReceiverThread messagesT = new ServerReceiverThread(ConfigController.getLocalInetAddress(), ConfigController.getTserverPort());
            messagesT.start();

            LocalReceiverThread localT = new LocalReceiverThread(ConfigController.getLocalPort(), ConfigController.getTserverPort());
            localT.start();

        } catch (ReceiverThreadException e) {
            e.printStackTrace();
            System.out.println("TermiteServer Stopped.");
            System.exit(0);
        }


        /*
         * Set up the command line parser
         */
        try {
            jline.TerminalFactory.registerFlavor(jline.TerminalFactory.Flavor.WINDOWS, UnsupportedTerminal.class);
            ConsoleReader reader = new ConsoleReader();
            reader.setPrompt("\u001B[1m>\u001B[0m ");


            ConsoleUI consoleUi = new ConsoleUI(AVDController, reader);
            printStartMessage();
            consoleUi.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printResult(ArrayList<String> res) {
        if (res != null) {
            System.out.println("Result = " + res.toString());
        }
    }

    private static void printStartMessage() {
        System.out.println("");
        System.out.println("  \u001B[1mTermite2 Server ONLINE on network " + ConfigController.getLocalNetworkIp() + ":\u001B[0m");
        System.out.println("  \u001B[1mTo register this Termite2 Server on Termite2 Client use the address: " + ConfigController.getLocalNetworkIp() +":"+ ConfigController.getClientPort() + "\u001B[0m");
        System.out.println("  Working Directory = " + System.getProperty("user.dir"));
        System.out.println("  TERMITE2_SERVER_PATH = " + ConfigController.getTermiteServerPath());
        System.out.println("  TERMITE2_PLATFORM = " + ConfigController.getServerPlatform());
        System.out.println("  ANDROID_SDK_PATH = " + ConfigController.getAndroidSDKPath());
        System.out.println("  Type \"help\" or \"h\" for the full command list");
        System.out.println("");
    }
}