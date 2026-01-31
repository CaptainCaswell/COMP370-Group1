import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    protected int clientID;
    protected static int nextID = 1;

    protected int primaryServerPort;
    protected boolean running = true;

    protected static final String host = "localhost";
    protected static final int monitorPort = 9000;

    protected static final long HEARTBEAT_INTERVAL = 2000;
    protected static final long DATA_INTERVAL = 5000;

    // Constructor
    public Client() {
        this.clientID = nextID;
        nextID++;
    }

    public static void main ( String[] args ) throws Exception {
        Client client = new Client();
        client.start();
    }

    protected void start() throws Exception {
        // Start sending heartbeat
        Thread heartbeatThread = new Thread(() -> {
            try {
                while ( running ) {
                    sendHeartbeat();
                    Thread.sleep( HEARTBEAT_INTERVAL );
                }
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        });

        heartbeatThread.setDaemon( true );
        heartbeatThread.start();

        while ( running ) {
            // Get random number from 1-100
            int data = (int)(Math.random() * 100) + 1;

            if ( getPrimary() == 0 ) {
                System.out.println( "Primary not found, requesting from monitor...");
                // Request done by heartbeat
            }

            if ( getPrimary() == 0 ) {
                System.out.println( "Unable to find primary. Retrying..." );
            } else {
                sendData( data );
            }
        
            Thread.sleep( DATA_INTERVAL );
        }
    }

    protected void sendHeartbeat() {
        try {
            // Setup Socket with input and output
            Socket socket = new Socket( host, monitorPort );
            Scanner input = new Scanner( socket.getInputStream() );
            PrintStream output = new PrintStream( socket.getOutputStream() );

            // Request primary or send heartbeat
            output.println( "CLIENT_HEARTBEAT " + clientID );

            // Get monitor response
            String response = input.nextLine();

            if ( response.startsWith( "CURRENT_PRIMARY" ) ) {
                setPrimary( Integer.parseInt( response.split( " " )[1] ) );
            } else if ( response.equals( "ACK" ) ) {
                System.out.println( "Heartbeat acknowledged." );
            } else if ( response.equals( "NONE" ) ) {
                System.out.println( "No primary server available." );
            }

            input.close();
            output.close();
            socket.close();
        } catch ( Exception e ) {
            System.out.println( "### Heartbeat failed ###" );
        }
    }

    protected void sendData( int data ) throws Exception {
        try {
            // Setup Socket with input and output
            Socket socket = new Socket( host, getPrimary() );
            Scanner input = new Scanner( socket.getInputStream() );
            PrintStream output = new PrintStream( socket.getOutputStream() );

            // Send data
            System.out.println( "Sending the following to server: " + Integer.toString( data ) );
            output.println( data );

            // Display response from server
            String response = input.nextLine();
            System.out.println( "Server response: " + response );

            // Close things
            input.close();
            output.close();
            socket.close();
        } catch ( Exception e ) {
            System.out.println( "### Send data failed ###" );
            System.out.println( "### Trying to send to " + getPrimary() + " ###" );
        }
    }

    protected synchronized int getPrimary() {
        return primaryServerPort;
    }

    protected synchronized void setPrimary(int port) {
        if ( primaryServerPort != port ) {
            primaryServerPort = port;
        }
    }

}