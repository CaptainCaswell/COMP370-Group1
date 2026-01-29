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

    // Constructor
    public ServerProcess() {
        this.serverID = nextID;
        this.port = nextID + 1000;
        nextID++;
        this.isPrimary = !( checkPrimary() );
    }

    // Start Basic Server
    public void start () throws IOException {
        // Open socket
        serverSocket = new ServerSocket( port );

        // Announce system running
        System.out.println( " ---- SERVER " + serverID + " ---- " );
        System.out.println( ( isPrimary ? "Primary" : "Secondary" ) + " server started on port " + port );

        // What a primary server does
        if ( isPrimary ) {
            while ( running ) {
                
                // Waits for connection, returns socket when connected
                Socket clientSocket = serverSocket.accept();

                // Set variables for socket input and output
                Scanner input = new Scanner( clientSocket.getInputStream() );
                PrintStream output = new PrintStream( clientSocket.getOutputStream() );

                // Display input from client
                String clientMessage = input.nextLine();
                System.out.println( "Client sent: " + clientMessage + ". New sum is " + sum + "." );

                // Process and reply
                String outgoingMessage = processMessage( clientMessage );
                output.println( outgoingMessage );

                // Close things
                input.close();
                clientSocket.close();
            }
        }

        // What a secondary server does
        else {
            while( running ) {
                System.out.println( "Waiting in standby mode..." );
                running = false;
            }
        }

        serverSocket.close();

    }

    protected String processMessage( String clientMessage ) {
        // If message can be cast to number
        try {
            // Sanitize and convert message to integer
            int number = Integer.parseInt( clientMessage.trim() );

            sum += number;

            return "Sum sucessful. New sum: " + sum;
        } catch ( NumberFormatException e ) {
            return "Sum failed. Current sum: " + sum;
        }
    }

    public void promote() {
        isPrimary = true;
        System.out.println( "--- System promoted ---" );
        // Add return confirmation?
    }

    public static void main( String[] args ) throws IOException {
        // Start server
        ServerProcess server = new ServerProcess();
        server.start();
    }

    protected boolean checkPrimary() {
        // Should ask monitor is there is a primary already
        return false;
    }

}