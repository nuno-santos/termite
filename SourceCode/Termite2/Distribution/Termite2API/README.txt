
- This new Termite2 API works the same way as the old one used on the Termite system.
- The only diference that one must be aware is that after creating the communication object:

SimWifiP2pSocket ObjName = new SimWifiP2pSocket(destIp, Integer.parseInt(destPort));

YOU MUST USE getObjectOutputStream / getObjectOutputStream as communication channels 
and you get them by doing ObjName.getObjectOutputStream() and ObjName.getObjectInputStream().

See the following example:


try {
	
	// create coms object
	SimWifiP2pSocket mCliSocket = new SimWifiP2pSocket(destIp, Integer.parseInt(destPort));

	// SEND MESSAGE
	Object dataToSend = new Object();
	mCliSocket.getObjectOutputStream().writeObject(dataToSend);
	mCliSocket.getObjectOutputStream().flush();

	// The response
	Object response = (Object) mCliSocket.getObjectInputStream().readObject();

} catch (UnknownHostException | ClassNotFoundException e) {
		e.fillInStackTrace();
} catch (IOException e) {
		e.printStackTrace();
}