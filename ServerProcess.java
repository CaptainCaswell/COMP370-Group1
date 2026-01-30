import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServerProcess {
    protected ServerSocket serverSocket;
    protected boolean running = true;
    protected static int nextID = 1;
    protected int sum = 0;
    protected boolean isPrimary;
    protected int serverID;
    protected int port;

    protected static final String host = "localhost";
    protected static final int monitorPort = 9000;
    protected static final long HEARTBEAT_INTERVAL = 2000;

    // Constructor
    public ServerProcess() {
        this.serverID = nextID;
        this.port = nextID + 1000;
        nextID++;
        this.isPrimary = false;
    }
    
    // Main
    public static void main( String[] args ) throws IOException {
        // Start server
        ServerProcess server = new ServerProcess();
        server.start();
    }

    // Start Basic Server
    public void start () throws IOException {
        // Start sending heartbeat
        Thread heartbeatThread = new Thread(() ->{
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
        serverSocket = new ServerSocket( port );

        // Announce system running
        System.out.println( " ---- SERVER " + serverID + " ---- " );        
        
        while ( running ) {
            
            // What a primary server does
            if ( getIsPrimary() ) {
                // Waits for connection, returns socket when connected
                Socket clientSocket = serverSocket.accept();

                // Set variables for socket input and output
                Scanner input = new Scanner( clientSocket.getInputStream() );
                PrintStream output = new PrintStream( clientSocket.getOutputStream() );

                // Get input from client
                String clientMessage = input.nextLine();
                
                // Process and reply
                String outgoingMessage = processMessage( clientMessage );
                System.out.println( "Client sent: " + clientMessage + ". New sum is " + sum + "." );
                output.println( outgoingMessage );

                // Close things
                input.close();
                clientSocket.close();
            }

            // What a secondary server does
            else {
                try {
                    System.out.println( "Waiting in standby mode..." );
                    Thread.sleep( 1000 );
                } catch ( InterruptedException e ) {
                    break;
                }
            }
        }

        serverSocket.close();

    }

    protected void sendHeartbeat() {
        try {
            // Setup Socket with input and output
            Socket socket = new Socket( host, monitorPort );
            Scanner input = new Scanner( socket.getInputStream() );
            PrintStream output = new PrintStream( socket.getOutputStream() );

            // Request primary or send heartbeat
            output.println( "SERVER_HEARTBEAT " + serverID + " " + port + " " + sum );

            // Get monitor response
            String response = input.nextLine();

            if ( response.equals( "PRIMARY" ) && !getIsPrimary() ) {
                promote();
                System.out.println( "Server promoted" );
            } else if ( response.equals( "ACK" ) || response.equals( "SECONDARY" ) ) {
                System.out.println( "Heartbeat acknowledged." );
            }

            input.close();
            output.close();
            socket.close();
        } catch ( Exception e ) {
            System.out.println( "### Heartbeat failed ###" );
        }
    }

    protected String processMessage( String clientMessage ) {
        // If message can be cast to number
        try {
            // Sanitize and convert message to integer
            int number = Integer.parseInt( clientMessage.trim() );

            toSum( number );

            return "Sum sucessful. New sum: " + sum;
        } catch ( NumberFormatException e ) {
            return "Sum failed. Current sum: " + sum;
        }
    }

    public synchronized void promote() {
        isPrimary = true;
        System.out.println( "--- System promoted ---" );
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

}