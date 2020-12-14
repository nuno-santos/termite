package main.pt.inesc.termite2.cli;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import main.pt.inesc.termite2.cli.commands.*;
import main.pt.inesc.termite2.cli.exceptions.ConfigErrorException;
import main.pt.inesc.termite2.cli.exceptions.RemoteAVDControllerException;
import main.pt.inesc.termite2.cli.exceptions.WebPageErrorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Main {

    private static String tomcatPath;
    private static String termite2Path;
    private static String platform;

    public static void main(String[] args) {

        // Supported commands
        Command[] commands = {
                //Network commands
                new NewDeviceCommand(),
                new DeleteDeviceCommand(),
                new BindDeviceCommand(),
                new UnbindDeviceCommand(),
                new CreateGroupCommand(),
                new JoinGroupCommand(),
                new LeaveGroupCommand(),
                new DeleteGroupCommand(),
                new MoveCommand(),
                new CommitCommand(),
                new WaitCommand(),
                new ClearCommand(),
                //emulator cmds
                new RefreshCommand(),
                new CreateAvdsCommand(),
                new DestroyAvdsCommand(),
                new InstalledEmulatorsCommand(),
                new StartEmulatorsCommand(),
                new StopEmulatorCommand(),
                new StopAllEmulatorsCommand(),
                new InstallApplicationCommand(),
                new StartApplicationCommand(),
                new runRemoteScriptCommand(),
                //program commands
                new ListCommand(),
                new LoadCommand(),
                new RunLocalScriptCommand(),
                new LogTimeCommand(),
                new PingCommand(),
                new HelpCommand(),
                new ClsCommand(),
                new QuitCommand(),
        };

        Command [] sharedCommands = {
                //emulator cmds
                new RefreshCommand(),
                new CreateAvdsCommand(),
                new DestroyAvdsCommand(),
                new InstalledEmulatorsCommand(),
                new StartEmulatorsCommand(),
                new StopEmulatorCommand(),
                new StopAllEmulatorsCommand(),
                new InstallApplicationCommand(),
                new StartApplicationCommand(),
                new runRemoteScriptCommand(),
                //program commands
                new ListCommand(),
                new RunLocalScriptCommand(),
                new LogTimeCommand(),
                new PingCommand(),
                new HelpCommand(),
                new ClsCommand(),
                new QuitCommand(),
        };

        try {

            /*
             * Initiates the configuration files
            */
            ConfigManager configManager = new ConfigManager();
            configManager.loadConfigurations();

            //Gets needed global variables
            tomcatPath = configManager.getTomcatPath();
            termite2Path = configManager.getTermitePath();
            platform = configManager.getTermitePlatform();


            /*
             * Initiates connections to remote controller
             */
            RemoteAVDController remoteAVDController = new RemoteAVDController(configManager.getControllerNetworks(), configManager.getControllerFullNetworks());
            remoteAVDController.startSocketConnections();

            /*
             * Set up the command line parser
             */
            jline.TerminalFactory.registerFlavor(jline.TerminalFactory.Flavor.WINDOWS, UnsupportedTerminal.class);
            ConsoleReader reader = new ConsoleReader();
            List<String> cmdNames = new LinkedList<String>();
            for (Command cmd : commands) {
                for (String s : cmdNames) {
                    assert !s.equals(cmd.getName()) && !s.equals(cmd.getAbvr());
                }
                cmdNames.add(cmd.getName());
                if (!cmd.getAbvr().equals(Command.NULLABVR)) {
                    cmdNames.add(cmd.getAbvr());
                }
            }
            reader.addCompleter(new StringsCompleter(cmdNames));
            reader.setPrompt("\u001B[1m>\u001B[0m ");


            /*
             * Initialize a globally-shared context object
             */
            Context context = new Context(commands, sharedCommands, reader);
            context.mConfigManager = configManager;
            context.mRemoteAVDController = remoteAVDController;


            /*
             * Gets online emulators on start
             */
            if(remoteAVDController.refreshEmulators()){
                //Refresh emuTracker
                ArrayList<Emulator> emulators = context.mRemoteAVDController.getEmulators();
                EmusTracker et = context.mCurrentEmulation.getEmusTracker();
                et.updateEmuList(new ArrayList<>(emulators));
            }

            /*
             * If tomcat not set
             */
            if(tomcatPath.equals("undefined")){
                startConsoleUi(context);
            }
            /*
             * Start chosen interface made
             */
            askMode(); // Ask for interface mode

            String line = null;
            String[] tokens = null;
            while ((line = reader.readLine()) != null) {

                tokens = line.split("\\s+");
                if (tokens.length == 0) {
                    continue;
                }
                String cmd = tokens[0];
                if (cmd.equals("")) {
                    continue;
                }
                if (cmd.startsWith("#")) {
                    continue;
                }
                switch (cmd) {
                    case "1":
                        startConsoleUi(context);
                        break;
                    case "2":
                        startWebPageUi(context);
                        break;
                    case "quit":
                        context.mRemoteAVDController.closeConnections();
                        System.exit(0);
                    default:
                        System.out.println("  Invalid interface option.");
                        break;
                }
            }

        } catch (ConfigErrorException e) {
            System.out.println(e.getMessage());
        } catch(IOException t) {
            t.printStackTrace();
            System.out.println("Error starting Console UI.");
        } catch (RemoteAVDControllerException e) {
            e.printStackTrace();
        }
    }

    private static void startConsoleUi(Context context) throws IOException {
        ConsoleUI consoleUi = new ConsoleUI(context);
        printConsoleUiStart();
        consoleUi.start();
    }

    private static void startWebPageUi(Context context) throws IOException{
        try{
            WebPageUI webPageUi = new WebPageUI(context);
            printWebPageUiStart();
            webPageUi.start();
        } catch (WebPageErrorException e){
            e.printStackTrace();
            System.out.println("Starting console ui instead...");
            startConsoleUi(context);
        }
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * *   Printer methods  * * * * * * * * * * * * * * * * * * * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private static void askMode(){
        System.out.println("");
        System.out.println("  \u001B[1mTermite2 UI Options:\u001B[0m");
        System.out.println("  Type '1' to use the default console UI.");
        System.out.println("  Type '2' to use the Webpage UI.");
        System.out.println("");
    }

    private static void printConsoleUiStart(){
        System.out.println("");
        System.out.println("  \u001B[1mTermite2 Testbed - Console UI selected\u001B[0m");
        System.out.println("  Working Directory = " + System.getProperty("user.dir"));
        System.out.println("  TERMITE2_CLI_PATH = " + termite2Path);
        System.out.println("  TERMITE2_PLATFORM = " + platform);
        System.out.println("  Type \"help\" or \"h\" for the full command list");
        System.out.println("");
    }

    private static void printWebPageUiStart(){
        System.out.println("");
        System.out.println("  \u001B[1mTermite2 Testbed - Webpage UI selected\u001B[0m");
        System.out.println("  Working Directory = " + System.getProperty("user.dir"));
        System.out.println("  TOMCAT_PATH = " + tomcatPath);
        System.out.println("  TERMITE2_CLI_PATH = " + termite2Path);
        System.out.println("  TERMITE2_PLATFORM = " + platform);
        System.out.println("  Access the ui with Google Chrome at localhost:8080/Termite2UI/index.html");
        System.out.println("  Type \"help\" or \"h\" for the shared command list");
        System.out.println("");
    }
}
