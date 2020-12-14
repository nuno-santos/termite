package com.termiteserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ReceiveCommands
 */
@WebServlet("/ReceiveCommands")
public class CommunicationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private CommandSender Sender;
       
    /**
     * @throws IOException 
     * @throws UnknownHostException 
     * @see HttpServlet#HttpServlet()
     */
    public CommunicationServlet(){
        Sender = new CommandSender();
    }

	/**
	 * Used to get startup data
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Servltet received GET(startup) request");
		
		String startupDataString = Sender.startup();
		
		OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
		
		writer.write(startupDataString);
	    writer.flush();
	    writer.close();
	    
	    System.out.println("GET(startup) response to termite2UI: " + startupDataString );
	    
	}

	/**
	 * Used to process commit and all other communications with termite2-cli
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Servltet received POST(commands) request");
		
		StringBuilder sb = new StringBuilder();
	    BufferedReader reader = request.getReader();
	    try {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            sb.append(line);
	        }
	    } finally {
	        reader.close();
	    }
	    
	    String msgreceived = sb.toString();
	    System.out.println("Message received on POST servlet: " + msgreceived );
	    
	    String postResult = Sender.send(msgreceived);
	    
	    OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
		writer.write(postResult);
	    writer.flush();
	    writer.close();
	    
	    System.out.println("POST(commands) response to termite2UI: " + postResult );
	}
	
}

