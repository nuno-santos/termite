package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;

import java.util.concurrent.TimeUnit;

public class WaitCommand extends Command {

    public WaitCommand() {
        super("wait", "w");
    }

    public String executeCommand(Context context, String [] args) {

        assert context != null && args != null;

        if (args.length != 2 && args.length != 3) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        // check the input time
        int time = 1;
        try {
            time = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return returnError("Wrong time format.");
        }

        // check the time units
        if (args.length == 3) {
            String unit = args[2];
            if (!unit.equals("ms")) {
                if(unit.equals("s")){
                    time *= 1000;
                }
                else if (unit.equals("m")) {
                    time *= 6000;
                }
                else {
                    return returnError("Wrong time unit.");
                }
            }
        }

        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            return returnError("Wait interrupted.");
        }
        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: wait|w [<time> [ms|s|m]]");
    }

    @Override
    public String getExplanation() {
        return "";
    }
}
