package com.termiteserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class CommandSender {
	
	private Socket _socket;
	
	public CommandSender() {
	}
	
	public String send(String msg) {
		String response;
		
		try {
			_socket = new Socket("127.0.0.1", 8081);
			DataOutputStream msgOut = new DataOutputStream(_socket.getOutputStream());
			
			//Sending commit message
			msgOut.writeUTF(msg);
			System.out.println("Command message: "+ msg + " sent to termite2-cli.");
			
			
			System.out.println("Waiting for response...");
			// Receive message from termite2-cli
			DataInputStream in = new DataInputStream(_socket.getInputStream());
			response = in.readUTF();
		    _socket.close();
		    System.out.println("Response received from termite2-cli: |" + response + "|");
		    
		    return response;
			
			
		} catch (IOException e) {
			System.out.println("Error: Failed to send/receive cmd message to/from termite2-cli.");
			return null;
		}
	}
	
	public String startup() {
		try {
			_socket = new Socket("127.0.0.1", 8081);
			
			//Sending startup message to termite2-cli
			DataOutputStream msgOut = new DataOutputStream(_socket.getOutputStream());
			msgOut.writeUTF("startup");
			System.out.println("startup request sent\n");
			
			//Receiving startup response from termite2-cli
			DataInputStream in = new DataInputStream(_socket.getInputStream());
		    String dataResponse = in.readUTF();
		    in.close();
		    _socket.close();
		    
			return dataResponse;
			
		} catch (IOException e) {
			System.out.println("Error: Failed to send/receive startup message to/from termite2-cli.");
			return null;
		}
	}
	
}
