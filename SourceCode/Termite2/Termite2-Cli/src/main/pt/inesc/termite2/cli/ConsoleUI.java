package main.pt.inesc.termite2.cli;

import jline.console.ConsoleReader;

import java.io.IOException;

public class ConsoleUI {

    ConsoleReader mReader;
    Context mContext;

    public ConsoleUI(Context c){
        mContext = c;
        mReader = mContext.getReader();
    }

    public void start() throws IOException {
        String line;
        while ((line = mReader.readLine()) != null) {

            String[] tokens = line.split("\\s+");
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
            String result;
            for (Command command : mContext.getCommands()) {
                if (command.getName().equals(cmd) || (
                        !command.getAbvr().equals(Command.NULLABVR) &&
                                command.getAbvr().equals(cmd))) {
                    found = true;
                    result = command.executeCommand(mContext,tokens);
                    if (result.equals("OK")){
                        mContext.getHistory().setLast(line);
                    }
                    else{
                        System.out.println(cmd + " " + result);
                        break;
                    }
                }
            }
            if (!found)
                System.out.println("Error: Command \"" + cmd + "\" does not exist.");
        }
    }
}
