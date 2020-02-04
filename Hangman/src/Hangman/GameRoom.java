package Hangman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class GameRoom {
	
	Vector<Lock> mLocks = new Vector<Lock>(); 
	Vector<Condition> mConditions = new Vector<Condition>();
	int mTurn = 0;
	int mWrongGuesses =7;
	
	Scanner in = new Scanner(System.in);
	
	ServerMain mServer;
	String mGameName;
	int mNumPlayers;
	String mSecretWord;
	ArrayList<ServerThread> mPlayers = new ArrayList<ServerThread>();;
	ArrayList<String> mWords = new ArrayList<String>();
	boolean isStarted = false;
	boolean isFirst = true;
	
	public GameRoom(ServerMain server, String gameName, int numPlayers, ArrayList<String> words) throws IOException {
		System.out.println("DEBUG: In " + gameName + "'s constructor.");
		mServer = server;
		mGameName = gameName;
		mWords = words;
		mNumPlayers = numPlayers;
	}
	
	public void CheckIfReady() throws IOException, InterruptedException {
		if (mNumPlayers!=mPlayers.size()) {
			this.broadcast("Waiting for "+(mNumPlayers-mPlayers.size())+" other user(s) to join...");
		}
		else {
			this.RunGame();
		}
	}
	
	public void RunGame() throws IOException, InterruptedException {
		//Time to Start
		//Start the game once full!
		this.broadcast("All users have joined.");
		//Initializing stuff~
		for (ServerThread player : mPlayers) {
			player.isPlaying = true;
			mLocks.add(player.mLock);
			mConditions.add(player.mCondition);
		}
		
		isStarted = true;
		
		this.broadcast("Determining secret word...");
		mSecretWord = mWords.get((int) (Math.random() * mWords.size()));
		boolean isWordGuessed = false;
        String guessedWord="";
        ServerThread currentPlayer = mPlayers.get(mTurn);
        
        char[] secretWordArray = mSecretWord.toCharArray();
        char[] playerGuess = new char[mSecretWord.length()];

        for (int i = 0; i < playerGuess.length; i++) {
            playerGuess[i] = '_';
        }
        
        this.broadcast("\nStart Game!\n");
        
        //Game Loop
        while (!isWordGuessed && mWrongGuesses != 0) {
        	currentPlayer = mPlayers.get(mTurn);
        	//Make sure the player is playing!
        	if (currentPlayer.isPlaying) {
        		for (ServerThread player : mPlayers) {
					player.mLock.lock();
				}
        		
				if (isFirst) {
					isFirst=false;
					this.broadcast("First turn!");
				}
				else {
					this.broadcastOthers("It is not your turn.", currentPlayer);
					currentPlayer.sendMessage("It is your turn!");
				}
                this.broadcast("Secret Word: " + CharArrayToString(playerGuess));
                this.broadcast("You have " + mWrongGuesses + " incorrect guesses remaining.");
                //If not their turn, display waiting for ....            
                this.broadcastOthers("\nWaiting for " +currentPlayer.mUsername + " to do something...\n", currentPlayer);
                //Current Player
                currentPlayer.sendMessage("\t1) Guess a Letter");
                currentPlayer.sendMessage("\t2) Guess the Word");
                currentPlayer.sendMessage("What would you like to do?");
                        
                String choice = currentPlayer.input.readLine();
                
                if (choice.equals("1")) {
                	currentPlayer.sendMessage("Letter to guess – ");
                	String guessedLetter = "";
                	do {
                		guessedLetter = currentPlayer.input.readLine();
                		if (guessedLetter.length()!=1) {
                			this.broadcast("Enter a single letter.");
                		}
                	} while (guessedLetter.length()!=1);
                	
                    char letter = guessedLetter.charAt(0);
                    
                    this.broadcastOthers(currentPlayer.mUsername + " has guessed letter '"+letter+"'.", currentPlayer);
                    
                    //Did the letter match any of those in the secret word?
                	boolean hit = false;
                    for (int i = 0; i < secretWordArray.length; i++) {
                        if (secretWordArray[i] == letter) {
                            playerGuess[i] = Character.toUpperCase(letter);
                            hit = true;
                        }
                    }
                    if (hit) {
                    	this.broadcast("\nThe letter '"+letter+"' is in the secret word.");
                    }
                    else {
                    	this.broadcast("\nThe letter '"+letter+"' is not in the secret word.");
                    	mWrongGuesses--;
                    }
                }
                else if (choice.equals("2")){
                	currentPlayer.sendMessage("What is the secret word?");
                	guessedWord = currentPlayer.input.readLine();
                    this.broadcastOthers(currentPlayer.mUsername + " has guessed the word '" + mSecretWord + "'.", currentPlayer);
                    if (guessedWord.equalsIgnoreCase(mSecretWord)) {
                    	isWordGuessed = true;
                    }
                    else {
                    	currentPlayer.sendMessage("That is incorrect! You lose!");
                    	currentPlayer.sendMessage("The word was \"" + mSecretWord + "\".");
                    	currentPlayer.isPlaying = false;
                    }
                }
                else {
                	currentPlayer.sendMessage("Enter 1 or 2.");
                }
                
                for (ServerThread player : mPlayers) {
					player.mLock.unlock();
				}
        	}
        	//End of the turn
        	//Check to see if all players are playing still
        	boolean noMorePlayers=true;
        	for (ServerThread player: mPlayers) {
        		if (player.isPlaying) {
        			noMorePlayers=false;
        		}
        	}
        	if (noMorePlayers) {
        		break;
        	}
        	//If players r still playing then signal next!
            this.SignalNext();

        }          
        
        //Out of the while loop (while (!isWordGuessed && mWrongGuesses != 0))
        
        //Check if word has been guessed yet
        if (isWordGuessed) {
            currentPlayer.sendMessage("That is correct! You win!");          
            this.broadcastOthers(currentPlayer.mUsername + " guessed the word correctly. You lose!", currentPlayer);
        }
        //Word hasn't been guessed! (AKA ran out of guesses)
        else if (mWrongGuesses==0){
        	this.broadcast("You all ran out of guesses! You all lose!");
        	this.broadcast("The word was \"" + mSecretWord + "\".");
        }
        
        /*
         * END OF GAME
         */
        
        this.broadcast("Thank you for playing hangman!");
        //Terminate client's programs!
        for (ServerThread player : mPlayers) {
	    	try {
	    		player.mLoggedIn = false;
	    		player.mClientSocket.close();
	    		for (int i = 0; i < mServer.mServerThreads.size(); i++) {
	            	if (mServer.mServerThreads.get(i)==player) {
	            		mServer.mServerThreads.remove(i);
	            		break;
	            	}
	            }
	    	} catch (IOException e) {
	    		System.out.println("Unable to disconnect! D:");
	      	}	
        }
        this.broadcast("terminate");
        //Remove this game from the server's mGames
        for (int i = 0; i < mServer.mGames.size(); i++) {
        	if (mServer.mGames.get(i)==this) {
        		mServer.mGames.remove(i);
        		break;
        	}
        }
        
	}
	
	public String CharArrayToString(char[] array) {
		String playerGuess = "";
        for (int i = 0; i < array.length; i++) {
            playerGuess+=(array[i] + " ");
        }
		return (playerGuess);
	}
	
	
	//BROADCAST TO ALL!
	public void broadcast(String message) {
		if (message != null) {
			for(ServerThread player : mPlayers) {
				player.sendMessage(message);
			}
		}
	}
	//BROADCAST TO OTHERS!
	public void broadcastOthers(String message, ServerThread currentPlayer) {
		if (message != null) {
			for(ServerThread player : mPlayers) {
				if (currentPlayer!=player) {
					player.sendMessage(message);
				}
			}
		}
	}
	
	public void SignalNext() {
		mTurn++;
		if (mTurn == mPlayers.size()) {
			mTurn=0;
		}
	}
	
}
