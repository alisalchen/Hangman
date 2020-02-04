package Hangman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.util.Properties;
import java.util.Scanner;

public class ClientMain extends Thread {
	
	Socket clientSocket;
	String mServerHostname;
	String mServerPort;
	PrintWriter pw;
	Scanner in = new Scanner(System.in); //man
	BufferedReader br;
    
	static LocalTime myTimeStamp;
	
    public static void main(String[] Args) throws IOException {
    	System.out.println("In ClientMain, you are running the program as a player!");
    	new ClientMain();		
    }
    
    public ClientMain() throws IOException {
    	//output = new PrintWriter(System.out);
    	AskForConfigFile();
    	//After successfully starting up, establish a connection to the server
    	System.out.println("Trying to connect to server...");
    	try {
    		clientSocket = new Socket(mServerHostname, Integer.parseInt(mServerPort));
    		
    		br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    		pw = new PrintWriter(clientSocket.getOutputStream());
    	}
        catch (Exception e) {
        	System.out.println("Unable to connect to server " + mServerHostname + " on port " + mServerPort + ".");
        	return;
        }
    	System.out.println("Connected to server!\n");
    	
    	//Start the Serverthread
    	this.start();
  
		//Get the message from serverThread and print it
		while(true) {
			String line = in.next();
			pw.println(line);
			pw.flush();
		}
    }
    
    //We also need this. Why? it won't output what we want from serverThread
    public void run() {
		try {
			String line = "";
			while(true) {
				line = br.readLine();
				if (line==null||line.equals("terminate")) {
					System.exit(0);
				}
				else {
					System.out.println(line);
				}
			}
		} catch (IOException ioe) {
			System.out.println("ioe in ChatClient.run(): " + ioe.getMessage());
		}
	}
    
    public void AskForConfigFile() throws IOException {
    	//First, ask for the config.txt file!
		boolean successConfig=false;
		//Keep looping until user enters in correct config.txt file
		do {
			System.out.println("What is the name of the configuration file?");
			String filename = in.next(); //File input
			System.out.println("Reading config file...");
			File file = new File(filename);
			while (!file.exists()) {
				System.out.println("Configuration file " + filename + " could not be found.");
				System.out.println("Please enter another filename.");
				filename = in.next();
				file = new File(filename);
			}
			//If the file exists, begin to read in the file
			successConfig=ClientReadInFile(filename);
			if (!successConfig) {
				System.out.println("Please substitute in any required parameters.");
			}
		} while(!successConfig);
    }

   
   //Reads in the config.txt file
   public boolean ClientReadInFile(String filename) {
 		//Create a file object from user input
 		FileReader fileReader = null;
 		try { //try to see if file worked
 			fileReader = new FileReader(filename);
 			
 			Properties p = new Properties();
 			String SecretWordFile;
 			try {
 				p.load(fileReader);
 			} catch (IOException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 			if (p.getProperty("ServerHostname")==null || p.getProperty("ServerHostname").equals("")) {
 				System.out.println("ServerHostname is a required parameter in the configuration file.");
 				return false;
 			}
 			else {
 				mServerHostname = p.getProperty("ServerHostname");
 			}
 			if (p.getProperty("ServerPort")==null || p.getProperty("ServerPort").equals("")) {
 				System.out.println("ServerPort is a required parameter in the configuration file.");
 				return false;
 			}
 			else {
 				mServerPort = p.getProperty("ServerPort");
 			}
 			if (p.getProperty("SecretWordFile") == null || p.getProperty("SecretWordFile").equals("")) {
 				System.out.println("SecretWordFile is a required parameter in the configuration file.");
 				return false;
 			}
 			else {
 				SecretWordFile = p.getProperty("SecretWordFile");
 			}	
 			System.out.println("Server Hostname - " + mServerHostname);
 			System.out.println("Server Port - " + mServerPort);
 			System.out.println("Secret Word File - " + SecretWordFile);
 			
 			try {
 				fileReader.close();
 			} catch (IOException e) {
 				e.printStackTrace();
 			}
 			//Everything succeeded!
 	        return true;
 		}
 		catch (FileNotFoundException error){
 			System.out.println("The file " + filename + " could not be found.\n");
 			return false;
 		}
 		finally {
 			if (fileReader != null) {
 				try {
 					fileReader.close();
 				} catch (IOException e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 				}
 			}
 		}
 	}
}
