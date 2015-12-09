/* Dillon Cole Mabry */

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import javax.swing.*;

/**
 * Client side code for Client/Server Tic Tac Toe game. 
 * Followed certain steps based off of Ray Toal's Networked 2 
 * Player Tic Tac Toe Game here: http://cs.lmu.edu/~ray/notes/javanetexamples/
 * X and O buttons designed using https://www.youtube.com/watch?v=Db3cC5iPrOM
 */
public class Client {
    /* Setup jframe for GUI */
    private JFrame clientFrame = new JFrame("Welcome to Tic Tac Toe!");
    private JLabel messageLabel = new JLabel("");
    /* Setup imageicons for X and Os for labels */
    private ImageIcon icon;
    private ImageIcon opponentIcon;
    /* Sreate the grid for the board */
    private Grid[] board = new Grid[9];
    private Grid currentLocation;
    /* Socket and port fields */
    private static int PORT = 4000;
    private static Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    /**
     * Main method
     * @param args unused
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        try {
            while (true) {
                String serverAddress = "localhost";
                Client client = new Client(serverAddress);
                client.clientFrame.setSize(500, 500);
                client.clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                client.clientFrame.setVisible(true);
                client.clientFrame.setResizable(false);
                System.out.println("Client is running on "+ serverAddress);
                client.run();
                if (!client.replay()) {
                    break;
                }
            }
            /* finally close client socket upon exit */
        } finally {
            socket.close();
            System.out.println("Client socket on port "
                    + socket.getLocalPort() + " closed");
        }
    }

    /**
     * Constructor for client: includes in/out buffers, socket, and necessary
     * GUI grid for Tic Tac Toe grid
     *
     * @param serverAddress the server address to connect to
     * @throws Exception
     */
    public Client(String serverAddress) throws Exception {
        /* set up client sockets and in/out buffers */
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        /* setup graphical interface */
        /* setup message label fonts colors */
        messageLabel.setBackground(Color.black);
        Font font = new Font("SansSerif", Font.BOLD, 18);
        messageLabel.setFont(font);
        clientFrame.getContentPane().add(messageLabel, "North");
        /* setup client container and JPanel for board locations */
        Container c = clientFrame.getContentPane();
        c.setBackground(Color.LIGHT_GRAY);
        JPanel clientPanel = new JPanel();
        clientPanel.setBackground(Color.DARK_GRAY);
        clientPanel.setLayout(new GridLayout(3, 3, 3, 3));
        for (int i = 0; i < board.length; i++) {
            final int j = i;
            board[i] = new Grid();
            board[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    currentLocation = board[j];
                    out.println("MOVE " + j);
                }
            });
            clientPanel.add(board[i]);
        }
        clientFrame.getContentPane().add(clientPanel, "Center");
    }
    
    /**
     * The main thread of execution for the clients. Clients will listen to
     * response from the server and create appropriate input. Player sockets
     * will be closed at the end of execution.
     *
     * @throws java.lang.Exception
     */
    public void run() throws Exception {
        
        String serverResponse;
        try {
            /* read the server response from beginning welcome game and set
            up X and O icons accordingly 
            */
            serverResponse = in.readLine();
            if (serverResponse.startsWith("HELLO")) {
                char playerMark = serverResponse.charAt(6);
                /* get the X and O images for GUI */
                java.net.URL imageURL = Client.class.getResource("/images/X.png");
                icon = new ImageIcon(imageURL);
                java.net.URL oppImageURL = Client.class.getResource("/images/O.png");
                opponentIcon = new ImageIcon(oppImageURL);
                
                clientFrame.setTitle("Tic Tac Toe");
            }
            while (true) {
                serverResponse = in.readLine();
                /* if the player can move update the board current location
                using the grid methods and set the icon to the player icon
                */
                if (serverResponse.startsWith("CAN_MOVE")) {
                    messageLabel.setText("Available move, wait on other player");
                    currentLocation.setIcon(icon);
                    currentLocation.repaint();
                    currentLocation.setBackground(Color.BLUE);
                    currentLocation.repaint();
                /* if the opponent has moved set the icon and repaint the board
                    location the opponent moved from by parsing the integer
                    substring
                */
                } else if (serverResponse.startsWith("OPPONENT_MOVED")) {
                    int loc = Integer.parseInt(serverResponse.substring(15));
                    board[loc].setIcon(opponentIcon);
                    board[loc].repaint();
                    board[loc].setBackground(Color.DARK_GRAY);
                    board[loc].repaint();
                    messageLabel.setText("Opponent moved, your turn");
                /* if the server response is game over victory end the game
                    for the client and set messagelabel GUI to win
                    */
                } else if (serverResponse.startsWith("VICTORY")) {
                    messageLabel.setText("You are the winner!");
                    break;
                /* if server response is defeat update message with lost */
                } else if (serverResponse.startsWith("DEFEAT")) {
                    messageLabel.setText("You have lost!");
                    break;
                /* if the message is tie update that both players have tied */
                } else if (serverResponse.startsWith("TIE")) {
                    messageLabel.setText("You tied with the other player!");
                    break;
                /* if server response is message update substring */
                } else if (serverResponse.startsWith("MESSAGE")) {
                    messageLabel.setText(serverResponse.substring(8));
                }
            }
            out.println("QUIT");
        } finally {
            /* close client sockets */
            socket.close();
        }
    }
    
    /**
     * Reply method to determine if client wants to play again or not
     * 
     * @return true if yes false if no client doesn't want to play again
     */
    boolean replay() {
        int dialogResult = JOptionPane.showConfirmDialog(clientFrame,
                "Play again?", "Press Yes to restart.",
                JOptionPane.YES_NO_OPTION);
        clientFrame.dispose();
        if(dialogResult == JOptionPane.YES_OPTION) {
            return true;
        }
        return false;
    }

    /**
     * Grid class to create grid sections for the JPanel panel
     * for the client board setup. Each grid has a setIcon method
     * to create the X or O
     */
    static class Grid extends JPanel {

        JLabel label = new JLabel((Icon) null);

        public Grid() {
            setBackground(Color.LIGHT_GRAY);
            add(label);
        }

        public void setIcon(Icon icon) {
            label.setIcon(icon);
        }
    }

    /**
     * Class to create the X or O buttons based on using the get resource
     * methods and ImageIcon constructors from Java UI
     */
    class XOButton extends JButton implements ActionListener {

        ImageIcon X, O;
        byte value = 0;

        public XOButton() {
            X = new ImageIcon(this.getClass().getResource("X.png"));
            O = new ImageIcon(this.getClass().getResource("O.png"));
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            value++;
            value %= 3;
            switch (value) {
                case 0:
                    setIcon(null);
                    break;
                case 1:
                    setIcon(X);
                    break;
                case 2:
                    setIcon(O);
                    break;
            }
        }

    }

}
