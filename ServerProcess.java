import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerProcess {
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    protected ServerSocket serverSocket;
    protected boolean running = true;
    protected static int nextID = 1;
    protected int sum = 0;
    protected boolean isPrimary;
    protected int serverID;
    protected int port;
    protected Logger logger;

    protected static final String host = "localhost";
    protected static final int monitorPort = 9000;
    protected static final long HEARTBEAT_INTERVAL = 1000;

    // Constructor
    public ServerProcess( int port) {
        this.serverID = port;
        nextID++;
        this.isPrimary = false;
        this.logger = new Logger( "server" + port + ".log" );
    }
    
    // Main
    public static void main( String[] args ) throws IOException, InterruptedException {
        
        if ( args.length != 1 ) {
            System.out.println( "Incorrect Syntax. Enter \"java ServerProcess <port>\"." );
            return;
        }

        int port = Integer.parseInt( args[0] );
        
        // Start server
        ServerProcess server = new ServerProcess( port );
        server.start();
    }

    // Start Basic Server
    public void start () throws IOException, InterruptedException {
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
                
                // Process and reply
                String outgoingMessage = processMessage( clientMessage );
                logger.log( "Recieved " + clientMessage );
                logger.log( "Sum updated to " + sum );
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

    protected void sendHeartbeat() {
        try {
            // Setup Socket with input and output
            Socket socket = new Socket( host, monitorPort );
            Scanner input = new Scanner( socket.getInputStream() );
            PrintStream output = new PrintStream( socket.getOutputStream() );

            // Request primary or send heartbeat
            output.println( "SERVER_HEARTBEAT " + serverID + " " + sum );

            // Get monitor response
            String response = input.nextLine();

            if ( response.startsWith( "PRIMARY" ) && !getIsPrimary() ) {
                promote();

                // Get restored sum if monitor sent it
                String[] parts = response.split(" ");
                if (parts.length > 1) {
                    int restoredSum = Integer.parseInt(parts[1]);
                    setSum(restoredSum);
                }

                logger.log( "Server promoted" );
            } else if ( response.equals( "ACK" ) || response.equals( "SECONDARY" ) ) {
                logger.log( "Heartbeat acknowledged." );
            }

            input.close();
            output.close();
            socket.close();
        } catch ( Exception e ) {
            logger.log( "### Heartbeat failed ###" );
        }
    }

    protected String processMessage( String clientMessage ) {
        // If message can be cast to number
        try {
            // Sanitize and convert message to integer
            int number = Integer.parseInt( clientMessage.trim() );

            toSum( number );

            return "Sum successful. New sum: " + sum;
        } catch ( NumberFormatException e ) {
            return "Sum failed. Current sum: " + sum;
        }
    }

    public synchronized void promote() {
        isPrimary = true;
    }



    protected synchronized boolean getIsPrimary() {
        // Should ask monitor is there is a primary already
        return isPrimary;
    }

    protected synchronized void toSum( int number ) {
        sum += number;
    }

    protected synchronized int getSum() {
        return sum;
    }

    // Set server sum to restored value
    public synchronized void setSum(int restoredSum) {
        if ( restoredSum != 0 ) { 
            this.sum = restoredSum;
            logger.log("Starting sum set to " + restoredSum );
        }
    }


}