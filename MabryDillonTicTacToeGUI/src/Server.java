/* Dillon Cole Mabry */

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

/**
 * Server side code for Client/Server Tic Tac Toe game. Followed certain steps
 * based off of Ray Toal's Networked 2 Player Tic Tac Toe Game here:
 * http://cs.lmu.edu/~ray/notes/javanetexamples/
 *
 */
public class Server {
    
    /**
     * Main method
     * @param args unused
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(4000);
        System.out.println("The Tic Tac Toe Server side is running on port: "+serverSocket.getLocalPort());
        System.out.println("Server socket address: "+serverSocket.getLocalSocketAddress());
        System.out.println("");
        try {
            while (true) {
                /* setup the actual game and create player threads */
                TicTacToe game = new TicTacToe();
                TicTacToe.Player playerOne = game.new Player(serverSocket.accept(), 'X');
                System.out.println("Player one connected on port " + playerOne.socket.getPort());
                TicTacToe.Player playerTwo = game.new Player(serverSocket.accept(), 'O');
                System.out.println("Player two connected on port " + playerTwo.socket.getPort());
                /* set player opponents opposite */
                playerOne.setOpponent(playerTwo);
                playerTwo.setOpponent(playerOne);
                /* set current player and start threads */
                game.currentPlayer = playerOne;
                playerOne.start();
                playerTwo.start();
            }
        } finally {
            serverSocket.close();
            System.out.println("Server Socket now closed....");
        }
    }
}

/**
 * Game class for the Tic Tac Toe game
 */
class TicTacToe {
    
    /* Setup the board with slots owned by each player as null */
    private final Player[] board = {null, null, null,
        null, null, null,
        null, null, null};

    /* The current player */
    Player currentPlayer;

    /* Returns if a winner has been made by checking each board slot and return
     true if there is a winner false otherwise */
    public boolean gameWinner() {
        return (board[0] != null && board[0] == board[1] && board[0] == board[2])
                || (board[3] != null && board[3] == board[4] && board[3] == board[5])
                || (board[6] != null && board[6] == board[7] && board[6] == board[8])
                || (board[0] != null && board[0] == board[3] && board[0] == board[6])
                || (board[1] != null && board[1] == board[4] && board[1] == board[7])
                || (board[2] != null && board[2] == board[5] && board[2] == board[8])
                || (board[0] != null && board[0] == board[4] && board[0] == board[8])
                || (board[2] != null && board[2] == board[4] && board[2] == board[6]);
    }
    

    /* Returns if the board has been filled or not */
    public boolean boardFilledUp() {
        for (Player gameBoard : board) {
            if (gameBoard == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method to check if the player is available to move
     * If the player is the current player and the board location
     * is free then the board location is now owned by the current player
     * and the current player is now the opponent
     * @param location the board location
     * @param player the player 
     * @return true if the player can move, false otherwise
     */
    public synchronized boolean canMove(int location, Player player) {
        if (player == currentPlayer && board[location] == null) {
            board[location] = currentPlayer;
            currentPlayer = currentPlayer.opponent;
            currentPlayer.otherPlayerMoved(location);
            return true;
        } else {
        }
        return false;
    }

    /**
     * Player class for each thread
     */
    class Player extends Thread {

        /* Setup buffer fields */
        Player opponent;
        char playerMark;
        Socket socket;
        BufferedReader playerIn;
        PrintWriter playerOut;

        /**
         * Constructs a handler thread for a given socket and mark initializes
         * the stream fields, displays the first two welcoming messages.
         */
        public Player(Socket socket, char playerMark) throws IOException {
            this.socket = socket;
            this.playerMark = playerMark;
            try {
                playerIn = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                playerOut = new PrintWriter(socket.getOutputStream(), true);
                playerOut.println("HELLO " + playerMark);
                playerOut.println("MESSAGE Waiting for opponent to connect");
            } catch (IOException e) {
                System.out.println("Player IOException: " + e);  
                System.out.println("Oh no! A player has been disconnected!");
            }
        }
        
        /**
         * Accepts notification of who the opponent is.
         */
        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        /**
         * Helper method for canMove method
         */
        public void otherPlayerMoved(int location) {
            playerOut.println("OPPONENT_MOVED " + location);
            playerOut.println(
                    gameWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
        }

        /**
         * The run method of this thread.
         */
        public void run() {

            try {

                playerOut.println("MESSAGE Players are connected. Player 2 wait"
                        + " on Player 1");

                if (playerMark == 'X') {
                    playerOut.println("MESSAGE Player 1 Your move");
                }

                // Repeatedly get commands from the client and process them.
                while (true) {
                    String command = playerIn.readLine();
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(5));
                        System.out.println("Player move is "+location);
                        if (canMove(location, this)) {
                            playerOut.println("CAN_MOVE");
                            playerOut.println(gameWinner() ? "VICTORY"
                                    : boardFilledUp() ? "TIE"
                                            : "");
                        } else {
                            playerOut.println("MESSAGE Cannot Move");
                        }
                    } else if (command.startsWith("QUIT")) {
                        return; 
                    }
            
                }
            } catch (IOException e) {
                System.out.println("Player IOException: " + e);
                System.out.println("Oh no! A player has been disconnected!");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
