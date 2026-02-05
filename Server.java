import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    protected ServerSocket serverSocket;
    protected boolean running = true;
    protected static int nextID = 1;
    protected int sum = 0;
    protected boolean isPrimary;
    protected int serverID;
    protected int port;
    protected Logger logger;

    protected ServerHeartbeatSender heartbeatSender;

    protected static final String host = "localhost";
    protected static final int monitorPort = 9000;
    protected static final long HEARTBEAT_INTERVAL = 500;

    // Constructor
    public Server( int port) {
        this.serverID = port;
        nextID++;
        this.isPrimary = false;
        this.logger = new Logger( "server" + port + ".log" );

        // Create heartbeat sender
        this.heartbeatSender = new ServerHeartbeatSender( serverID, this, logger );
    }
    
    // Main
    public static void main( String[] args ) throws IOException, InterruptedException {
        
        if ( args.length != 1 ) {
            System.out.println( "Incorrect Syntax. Enter \"java Server <port>\"." );
            return;
        }

        int port = Integer.parseInt( args[0] );
        
        // Start server
        Server server = new Server( port );
        server.start();
    }

    // Start Basic Server
    public void start () throws IOException, InterruptedException {
        // Start seperate thread for heartbeat
        Thread heartbeatThread = new Thread( heartbeatSender );
        heartbeatThread.setDaemon( true );
        heartbeatThread.start();

        // Open socket
        serverSocket = new ServerSocket( serverID );

        // Announce system running
        logger.log( "Server " + serverID + " started" );        
        
        while ( running ) {
            // Waits for client to connect
            Socket clientSocket = serverSocket.accept();

            // New thread for each client
            threadPool.submit( () -> threadClient( clientSocket ) );
        }

        serverSocket.close();
    }

    protected void threadClient( Socket clientSocket ) {
        try {
            // If primary
            if ( getIsPrimary() ) {
                // Set variables for socket input and output
                Scanner input = new Scanner( clientSocket.getInputStream() );
                PrintStream output = new PrintStream( clientSocket.getOutputStream() );

                // Get input from client
                String clientMessage = input.nextLine();
                
                // Process message
                String outgoingMessage = processMessage( clientMessage );
                
                // Send reply
                output.println( outgoingMessage );

                // Close things
                input.close();
                clientSocket.close();
            }

            // If secondary
            else {
                // Reject and log
                PrintStream output = new PrintStream( clientSocket.getOutputStream() );
                output.println( "NOT_PRIMARY" );
                output.close();
                logger.log( "Rejected client connection as secondary" );
            }
            
            clientSocket.close();

        } catch ( IOException e) {
            logger.log( "Error accepting client" );
        }
    }

    protected String processMessage( String clientMessage ) {
        int newSum;

        try {
            // Sanitize and convert message to integer
            int number = Integer.parseInt( clientMessage.trim() );

            // Track new sum in case it is updated before sending
            newSum = toSum( number );

            logger.log( "Recieved " + clientMessage );
            logger.log( "Sum updated to " + sum );

            return "SUCCESS " + newSum;
        } catch ( NumberFormatException e ) {
            logger.log( "Invalid input from client" );

            newSum = getSum();
            return "FAILED " + newSum;
        }
    }

    public synchronized void promote() {
        isPrimary = true;
    }

    protected synchronized boolean getIsPrimary() {
        // Should ask monitor is there is a primary already ???
        return isPrimary;
    }

    protected synchronized int toSum( int number ) {
        sum += number;
        return sum;
    }

    protected synchronized int getSum() {
        return sum;
    }

    // Set server sum to restored value
    public synchronized void setSum(int newSum) {
        if ( newSum != 0 && newSum > sum ) { 
            this.sum = newSum;
            logger.log("Sum set to " + newSum );
        }
    }
}