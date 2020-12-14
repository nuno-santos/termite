package pt.inesc.termite.wifidirect.sockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;


public class SimWifiP2pSocket implements SimWifiP2pSocketWrapper {
	
	public static String TAG = "SimWifiP2pSocket";

	public SimWifiP2pSocket() {
	}

	// Old Termite socket, use tag "old" to use this
	public SimWifiP2pSocket(String tag, String dstName, int dstPort)
			throws UnknownHostException, IOException {

		SimWifiP2pSocketManager sockManager = SimWifiP2pSocketManager.getSockManager();
		sockManager.sockOpenSocket(this, dstName, dstPort);
	}

	// New socket object to be used on Termite2 with termite2 Server(s)
	// Not that when using this socket ONLY USE ObjectOutputStream / getObjectInputStream for data transfers
	// get them by invoking the get methods on the socket object
	public SimWifiP2pSocket(String dstName, int dstPort) throws IOException, ClassNotFoundException {

		SimWifiP2pSocketManager sockManager = SimWifiP2pSocketManager.getSockManager();
		sockManager.sockOpenConnectSocket(this, dstName, dstPort);
	}

	public InputStream getInputStream () throws IOException {
		
		SimWifiP2pSocketManager sockManager = 
				SimWifiP2pSocketManager.getSockManager();
		return sockManager.sockGetInputStream(this);
	}

	public OutputStream getOutputStream() throws IOException {
		
		SimWifiP2pSocketManager sockManager = 
				SimWifiP2pSocketManager.getSockManager();
		return sockManager.sockGetOutputStream(this);
	}

	public ObjectInputStream getObjectInputStream () throws IOException {

		SimWifiP2pSocketManager sockManager =
				SimWifiP2pSocketManager.getSockManager();
		return sockManager.sockGetObjectInputStream(this);
	}

	public ObjectOutputStream getObjectOutputStream() throws IOException {

		SimWifiP2pSocketManager sockManager =
				SimWifiP2pSocketManager.getSockManager();
		return sockManager.sockGetObjectOutputStream(this);
	}

	public void close() throws IOException {
		
		SimWifiP2pSocketManager sockManager = 
				SimWifiP2pSocketManager.getSockManager();
		sockManager.sockClose(this);
	}

	public boolean isClosed() {
		
		SimWifiP2pSocketManager sockManager = 
				SimWifiP2pSocketManager.getSockManager();
		return sockManager.sockIsClosed(this);
	}
}
