import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    protected int clientID;
    protected static int nextID = 1;

    protected int primaryServerPort;
    protected boolean running = true;
    protected Logger logger;

    protected HeartbeatSenderClient heartbeatSender;

    protected static final String host = "localhost";
    protected static final int monitorPort = 9000;

    protected static final long DATA_INTERVAL = 5000;
    protected static final int MAX_CLIENTS = 1999;

    // Constructor
    public Client( int clientID) {
        this.clientID = clientID;
        this.logger = new Logger( "client" + clientID + ".log" );

        // Create heartbeat sender component
        this.heartbeatSender = new HeartbeatSenderClient( clientID, this, logger );
    }

    public static void main ( String[] args ) throws Exception {
        // Confirm args is correct length
        if ( args.length != 1 ) {
            System.out.println( "Incorrect Syntax. Enter \"java Client <number>\"." );
            return;
        }

        // Cast int and store
        int clientID = Integer.parseInt( args[0] );

        // Confirm clientID is in valid range
        if ( clientID <= 0 || clientID > MAX_CLIENTS ) {
            System.out.println( "Invalid ClientID. Must be 1 - 1999." );
            return;
        }

        // Create new client instance
        Client client = new Client( clientID );

        // Start new client
        client.start();
    }

    protected void start() throws Exception {
        // Start sending heartbeat
        Thread heartbeatThread = new Thread( heartbeatSender );

        heartbeatThread.setDaemon( true );
        heartbeatThread.start();

        logger.log( "Client " + clientID + " started" );

        while ( running ) {
            // Get random number from 1-100
            int data = (int)(Math.random() * 100) + 1;

            int primaryID = getPrimary();

            // Check if known primary
            if ( primaryID == 0 ) {
                logger.log( "No know primary" );
            }
            
            // Check valid range
            else if ( primaryID < 2000 || primaryID > 9999 ) {
                logger.log( "Invalid primary ID" );
            }
            
            // Check
            else {
                sendData( data );
            }
        
            Thread.sleep( DATA_INTERVAL );
        }
    }

    protected void sendData( int data ) throws Exception {
        boolean sent = false;
        
        while( !sent ) {
            try {
                int port = getPrimary();

                // Setup Socket with input and output
                Socket socket = new Socket( host, port );
                Scanner input = new Scanner( socket.getInputStream() );
                PrintStream output = new PrintStream( socket.getOutputStream() );

                // Send data
                output.println( data );
                logger.log( "Sent " + Integer.toString( data ) );

                // Get response from server
                String response = input.nextLine();

                // Check errors
                if ( response.equals( "NOT_PRIMARY" ) ) {
                    logger.log( "Data sent to secondary server, retrying" );

                    input.close();
                    output.close();
                    socket.close();
                    
                    Thread.sleep( 1000 );
                    continue;
                }

                // Check response for errors
                if ( response.startsWith( "SUCCESS" ) ) {
                    logger.log( "Server " + port + " recieved data, output is " + response, 1 );
                } else if ( response.startsWith( "FAILED" ) ) {
                    logger.log( "Data sent not integer" );
                } else {
                    logger.log( "Unknown response: " + response );
                }

                // Close things
                input.close();
                output.close();
                socket.close();

                // Mark sent to stop loop
                sent = true;
            } catch ( Exception e ) {
                logger.log( "Sending data to " + getPrimary() + " failed" );
                Thread.sleep( 1000 );
            }
        }
    }

    protected synchronized int getPrimary() {
        return primaryServerPort;
    }

    protected synchronized boolean setPrimary(int port) {
        if ( primaryServerPort != port ) {
            primaryServerPort = port;
            return true;
        }

        return false;
    }
}