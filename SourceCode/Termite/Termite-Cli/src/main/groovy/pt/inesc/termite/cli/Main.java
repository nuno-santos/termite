package pt.inesc.termite.cli;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import pt.inesc.termite.cli.commands.*;
import pt.inesc.termite.cli.exceptions.ConfigErrorException;

public class Main {

	public static void main(String[] args) {

        // Supported commands
        Command[] commands = {
                new AssignAddrCommand(),
                new BeginEmulationCommand(),
                new BindDeviceCommand(),
                new ClearCommand(),
                new ClsCommand(),
                new ChangeBackendCommand(),
                new CommitCommand(),
                new CreateGroupCommand(),
                new DeleteDeviceCommand(),
                new DeleteGroupCommand(),
                new DeployEmulatorCommand(),
                new EndEmulationCommand(),
                new HelpCommand(),
                new InstallAppCommand(),
                new JoinGroupCommand(),
                new KillEmulatorCommand(),
                new LastCommand(),
                new LaunchAppCommand(),
                new LeaveGroupCommand(),
                new ListCommand(),
                new LoadCommand(),
                new MoveCommand(),
                new NewDeviceCommand(),
                new PingCommand(),
                new QuitCommand(),
                new RunAppCommand(),
                new SetCommand(),
                new UnassignAddrCommand(),
                new UnbindDeviceCommand(),
                new UnsetCommand(),
                new WaitCommand(),
                new LogTimeCommand()
        };

        try {

            /*
             * Parse the configuration files
             */

            ConfigManager configManager = new ConfigManager();
            configManager.loadConfiguration();

            if (args.length > 0 && args[0].equals("check")) {
                try {
                    configManager.checkConnectorDrivers();
                } catch (Exception e) {
                    System.out.println("Check failed:");
                    System.out.println(e.getMessage());
                    System.exit(0);
                }
                System.out.println("Check succeeded.");
                System.exit(0);
            }

            /*
             * If necessary, define your default properties here
             */

                /*
                 * Example:
                 *
                 *  Properties props = context.getProperties();
                 *  props.addProperty(Properties.WAIT_ITERATIVE);
                 *
                 */

            /*
             * Set up the command line parser
             */

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

            Context context = new Context(commands, reader);
            context.mConfigManager = configManager;
            context.mEmulations = new HashMap<>();

            /*
             * Execute the init script
             */

            if (context.mConfigManager.getInitScript() != null) {
                String[] cmdargs = {"load", context.mConfigManager.getInitScript(), "-q"};
                try {
                    if (!new LoadCommand().executeCommand(context,cmdargs)) {
                        System.out.println("Error: execution of init script failed.");
                        System.exit(0);
                    }
                } catch (Exception e) {
                    System.out.println("Error: execution of init script failed.\n" +
                            e.getMessage());
                    System.exit(0);
                }
            }

            /*
             * Sit on the command line loop
             */

            usage();

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
                boolean found = false;
                boolean ok;
                for (Command command : context.getCommands()) {
                    if (command.getName().equals(cmd) || (
                            !command.getAbvr().equals(Command.NULLABVR) &&
                                    command.getAbvr().equals(cmd))) {
                        found = true;
                        ok = command.executeCommand(context,tokens);
                        if (ok) context.getHistory().setLast(line);
                        break;
                    }
                }
                if (!found)
                    Command.printWrongCommand(cmd);
            }

        } catch (ConfigErrorException e) {
            System.out.println(e.getMessage());
        } catch(Throwable t) {
            t.printStackTrace();
            System.out.println("Error occurred.\n" + t.getMessage());
        }
	}

    private static void usage() {
        System.out.println("");
        System.out.println("  \u001B[1mTermite Testbed\u001B[0m");
        System.out.println("  Working Directory = " + System.getProperty("user.dir"));
        System.out.println("  Type \"help\" or \"h\" for the full command list");
        System.out.println("");
    }
}
