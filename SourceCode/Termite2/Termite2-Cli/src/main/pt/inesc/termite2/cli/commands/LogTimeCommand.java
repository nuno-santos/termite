package main.pt.inesc.termite2.cli.commands;

import main.pt.inesc.termite2.cli.Command;
import main.pt.inesc.termite2.cli.Context;

import java.util.ArrayList;

public class LogTimeCommand extends Command {

    private ArrayList<Long> times = new ArrayList<Long>();
    private long timeSet;

    public LogTimeCommand() {
        super("time",Command.NULLABVR);
    }

    @Override
    public String executeCommand(Context context, String[] args) {
        assert context != null && args != null;

        if (args.length != 2) {
            printHelp();
            return returnError("Wrong number of input arguments.");
        }

        String operation = args[1];
        if(operation.equals("set")){
            timeSet = System.currentTimeMillis();
        }
        else if(operation.equals("log")){
            long timetolog = (System.currentTimeMillis() - timeSet);
            System.out.println("Time passed: " + timetolog + " ms");
            times.add(timetolog);

        }
        else if(operation.equals("show")){
            long average = 0;
            int timesSize = times.size();
            if(timesSize != 0){
                for(Long time : times){
                    average = average + time;
                }
                System.out.println(timesSize + " logged times: " + times.toString());
                printMeanVarianceDeviation();
            }
        }
        else if(operation.equals("reset")){
            times = new ArrayList<Long>();
        }
        else{
            printHelp();
            return returnError("Wrong time operation.");
        }

        return OK;
    }

    public void printHelp() {
        System.out.println("Syntax: time <set|log|show|reset>");
    }

    public String getExplanation(){
        return "This command works like a stop watch, use 'set' to initiate time and then 'log' to see how much time has passed in ms, 'show' displays all logged values, 'reset' resets 'show' values.";
    }

    public void printMeanVarianceDeviation(){
        if(times.size() != 0) {

            // The mean average
            double mean = 0.0;
            for (int i = 0; i < times.size(); i++) {
                mean += times.get(i);
            }
            mean /= times.size();
            System.out.println("Mean average: " + mean);

            // The variance
            double variance = 0;
            for (int i = 0; i < times.size(); i++) {
                variance += Math.pow(times.get(i) - mean, 2);
            }
            variance /= times.size();
            System.out.println("Variance: " + variance);

            // Standard Deviation
            double std = Math.sqrt(variance);
            System.out.println("Standard deviation: " + std);

            //Coefficient of Variation
            double cv = (std/mean)*100;
            System.out.println("Coefficient of Variation: " + ((int)cv) +"%");
        }
    }
}
