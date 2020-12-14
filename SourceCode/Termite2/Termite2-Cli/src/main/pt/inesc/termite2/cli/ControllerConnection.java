package main.pt.inesc.termite2.cli;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ControllerConnection {

    private Socket mSocketConnection;
    private ObjectOutputStream mOut;
    private ObjectInputStream mIn;

    public ControllerConnection(Socket connection) throws IOException {
        mSocketConnection = connection;
        mOut = new ObjectOutputStream(mSocketConnection.getOutputStream());
        mIn = new ObjectInputStream(mSocketConnection.getInputStream());
    }

    public synchronized Socket getSocketConnection() {
        return mSocketConnection;
    }

    public synchronized ObjectOutputStream getOut() {
        return mOut;
    }

    public synchronized ObjectInputStream getIn() {
        return mIn;
    }

    public void close() throws IOException {
        mSocketConnection.close();
    }
}
