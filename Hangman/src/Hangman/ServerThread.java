package Hangman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ServerThread extends Thread {
	/*
	 * MEMBER VARIABLES~~ how fun
	 */
	static String ServerHostname;
	static String ServerPort;

    Socket mClientSocket;
    ServerMain mServer;
    Lock mLock;
	Condition mCondition;

	GameRoom mGameRoom; 
	String mUsername;
	boolean mLoggedIn = false;
	boolean isPlaying = false;
	
	PrintWriter output;
	BufferedReader input;
	Scanner in = new Scanner(System.in);
	
	static LocalTime myTimeStamp;
	
	public ServerThread(Socket socket, ServerMain server, Lock lock, Condition condition) { 		
		try {
			mClientSocket = socket;
			mServer = server;
			
			output = new PrintWriter(mClientSocket.getOutputStream());
			input = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream()));
			
			mLock = lock;
			mCondition = condition;

		} catch (IOException ioe) {
			System.out.println("ioe in ServerThread constructor: " + ioe.getMessage());
			ioe.printStackTrace();
			return;
		}
		
		this.start();
	}

	public void run() {
		try {
			this.sendMessage("What's your name?"); 
			mUsername = input.readLine();
			this.sendMessage("Great! You are now logged in as " + mUsername + "!");
			mLoggedIn = true;
			
			//After login... ask to start or join a game!
			this.StartOrJoin(mUsername);
			
			//After this user started a game or joined a game, check if its ready to start
			//If it is, then it will start!
			//If not, keep waiting.
			mGameRoom.CheckIfReady();		
			
		} catch (IOException ioe) {
			System.out.println("ioe in ServerThread.run(): " + ioe.getMessage());
			ioe.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("e in ServerThread.run(): " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void StartOrJoin(String username) throws IOException {
		//Start or Join a Game	
		while (true) {
			this.sendMessage("\t1) Start a Game");
			this.sendMessage("\t2) Join a Game");
			this.sendMessage("Would you like to start a game or join a game?");
			
			String gameOption = input.readLine();
			String gameName = "";
			
			//CREATING A NEW GAME !
			if (gameOption.equals("1")) {
				this.sendMessage("What is the name of the game?");
				gameName=input.readLine();
				if (!mServer.FindGame(username, gameName)) {	
					int numPlayers;
					while (true) {
						this.sendMessage("How many users will be playing (1-4)?");
						try {
							numPlayers = Integer.parseInt(input.readLine());				
							while (numPlayers < 1 || numPlayers > 4) {
								this.sendMessage("Please enter a value between 1 and 4");
								numPlayers = Integer.parseInt(input.readLine());
							}
							break;
						}
						catch (Exception e) {
							e.printStackTrace();
							this.sendMessage("Please enter a value between 1 and 4");
						}
					}		
					
					//Create this game and set mGameRoom
					mGameRoom = mServer.CreateGame(username, gameName, numPlayers, this);
					break;
				}
				else {
					this.sendMessage(gameName + " already exists.");
				} 
			}
			//JOINING A GAME
			else if (gameOption.equals("2")) {				
				this.sendMessage("What is the name of the game?");
				gameName=input.readLine();
				//Check if a game with that name already exists
				//Call method FindGame in server to find the game
				if (mServer.FindGame(username, gameName)) {
					this.sendMessage("Found game " + gameName + "!");
					//Check if game is full
					if (mServer.IsGameFull(username, gameName)) {
						this.sendMessage("The game "+gameName+" does not have space for another user to join.");
					}
					else {
						//Yay! Join room!
						mGameRoom = mServer.JoinGame(username,gameName, this);
						//If not null...
						if (mGameRoom!=null) {
							break;
						}
					}
				}
				else {
					this.sendMessage("There is no game with name " + gameName + ".");
				}
			}
			else {
				this.sendMessage("Please enter one or two.");
			}
		}	
	}
	
	//Send message to US!! (The Players!!)
	public void sendMessage(String message) {
		output.println(message);
		output.flush();
	}
	
	
}

