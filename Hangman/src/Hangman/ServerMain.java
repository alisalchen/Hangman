package Hangman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerMain {

	static ServerSocket serverSocket;
	private String mServerPort;
	private String mSecretWordFile;

	
	Scanner in = new Scanner(System.in);
	
	//All the players, ever!
	Vector<ServerThread> mServerThreads = new Vector<ServerThread>();
	//All the games!
	ArrayList<GameRoom> mGames = new ArrayList<GameRoom>();


	//Establishes itself on the given port and
	//continuously listens for clients and sends them to ClientHandler
	public static void main(String[] Args) throws IOException {
		System.out.println("In ServerMain, you are starting the server!");
		new ServerMain();
	}
	
	public ServerMain() {
	  	AskForConfigFile();
	
	  	//After successfully reading in the file
	  	//Starting the server!
	  	System.out.println("Starting server!...");
	  	System.out.println("Binding to port " + mServerPort);
		try {
			serverSocket = new ServerSocket(Integer.parseInt(mServerPort));
			System.out.println("Bound to port " + mServerPort);
		  	System.out.println("Running!");
		  	while(true) {
		  		Socket client = serverSocket.accept();
		  		System.out.println("Client with " + client.toString() + " accepted. \n");
		  		
		  		Lock lock = new ReentrantLock();
		  		Condition condition =  lock.newCondition();
				
		  		ServerThread serverThread = new ServerThread(client, this, lock, condition); 
		  		mServerThreads.add(serverThread);
		  	}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
	}
	
	/*
	 * CREATING AND JOINING A GAME
	 */
	
	//CREATE a game
	//Here a game is created
	//is added to the server's vector of games
	//and adds the player to the gameroom.
	public GameRoom CreateGame(String username, String gameName, int numPlayers, ServerThread player) throws IOException {		
		ArrayList<String> words = new ArrayList<String>();
		BufferedReader br;
        FileReader fr;

        try {
            fr = new FileReader(mSecretWordFile);
            br = new BufferedReader(fr);
            String word;
            while ((word = br.readLine()) != null) {
                words.add(word);
            }
        } catch (IOException e) {
        	e.printStackTrace();
        }

		GameRoom game = new GameRoom(this, gameName, numPlayers, words);
		//Add to the vector of games!
		mGames.add(game);
		//Adding this player to the gameroom!
		game.mPlayers.add(player);
		return game;
	}
	
	//JOIN a game
	//Add player to the game!
	//Here should be where you broadcast to this player any existing players in the game
	//Here should also be where u broadcast to others that this player has joined
	public GameRoom JoinGame(String username, String gameName, ServerThread player) {
		int gameIndex=-1;
		for (int i = 0; i < mGames.size(); i++) {
			if (gameName.equalsIgnoreCase(mGames.get(i).mGameName)) {
				gameIndex = i;
			}
		}
		//Realistically, we will only need this. Lol~
		
		//Has the game already started? If so, then don't join!
		if (mGames.get(gameIndex).isStarted) {
			player.sendMessage("Cannot join game " + gameName + " because it has already started.");
			return null;
		}
		
		//Adding this user to the game room!
		mGames.get(gameIndex).mPlayers.add(player);
		//Send message to this player other player's info
		for (ServerThread otherPlayer : mGames.get(gameIndex).mPlayers) {
			if (otherPlayer!=player) {
				player.sendMessage("User " + otherPlayer.mUsername + " is in the game.");
			}
		}
		//Broadcasting to others that user has joined
		mGames.get(gameIndex).broadcastOthers("User " + username + " is in the game.", player);
		return mGames.get(gameIndex);
	}
	
	//Checks to see if the game exists
	public boolean FindGame(String username, String gameName) {
		//Loop through game objects
		for (int i = 0; i < mGames.size(); i++) {
			if (gameName.equalsIgnoreCase(mGames.get(i).mGameName)) {
				return true;
			}
		}
		return false;
	}
	
	//Checks if the game is full
	public boolean IsGameFull(String username, String gameName) {
		int gameIndex=-1;
		for (int i = 0; i < mGames.size(); i++) {
			if (gameName.equalsIgnoreCase(mGames.get(i).mGameName)) {
				gameIndex = i;
			}
		}
		if (mGames.get(gameIndex).mNumPlayers == mGames.get(gameIndex).mPlayers.size())
		{
			return true;
		}
		return false;
	}
	
	public void AskForConfigFile() {
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
			successConfig=ServerReadInFile(filename);
			if (!successConfig) {
				System.out.println("Please substitute in any required parameters.");
			}
		} while(!successConfig);
	}
	
	//Reads in the config.txt file
	public boolean ServerReadInFile(String filename) {
		//Create a file object from user input
		FileReader fileReader = null;
		try { //try to see if file worked
			fileReader = new FileReader(filename);
			
			Properties p = new Properties();
			String ServerHostname;
			try {
				p.load(fileReader);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (p.getProperty("ServerHostname")==null || p.getProperty("ServerHostname").equals("")) {
				System.out.println("ServerHostname is a required parameter in the configuration file.");
				return false;
			}
			else {
				ServerHostname = p.getProperty("ServerHostname");
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
				mSecretWordFile = p.getProperty("SecretWordFile");
			}	
			System.out.println("Server Hostname - " + ServerHostname);
			System.out.println("Server Port - " + mServerPort);
			System.out.println("Secret Word File - " + mSecretWordFile);
			
			try {
				fileReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//Everything succeeded!
	        return true;
		}
		catch (FileNotFoundException error){ //File not found... but should be caught earlier anyways lol
			System.out.println("The file " + filename + " could not be found.\n");
			return false;
		}
		finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
