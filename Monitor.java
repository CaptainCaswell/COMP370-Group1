import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServerProcess {
    protected ServerSocket serverSocket;
    protected int monitorPort = 900;
    protected boolean running = true;
    protected static int nextID = 1;
    protected int sum = 0;
    protected boolean isPrimary;
    protected int serverID;
    protected int port;

    // Constructor
    public ServerProcess() {
        this.serverID = nextID;
        this.port = nextID + 1000;
        nextID++;
        this.isPrimary = !( checkPrimary() );
    }
    
    public static void main( String[] args ) throws IOException {
        // Start server
        ServerProcess server = new ServerProcess();
        server.start();
    }

    // Start Basic Server
    public void start () throws IOException {
        // Open socket
        serverSocket = new ServerSocket( port );

        // Announce system running
        System.out.println( " ---- SERVER " + serverID + " ---- " );
        System.out.println( ( isPrimary ? "Primary" : "Secondary" ) + " server started on port " + port );

        
        
        while ( running ) {
            
            // What a primary server does
            if ( isPrimary ) {
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

    public void promote() {
        isPrimary = true;
        System.out.println( "--- System promoted ---" );
        // Add return confirmation?
    }

    protected boolean checkPrimary() {
        // Should ask monitor is there is a primary already
        return false;
    }

}