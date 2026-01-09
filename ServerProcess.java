import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServerProcess {
    private ServerSocket serverSocket;
    private boolean running = true;

    public void start ( int port ) throws IOException {
        serverSocket = new ServerSocket( port );
        System.out.println( " ---- SERVER ---- " );
        System.out.println( "Server started on port " + port );

        while ( running ) {
            
            // Waits for connection, returns socket when connected
            Socket clientSocket = serverSocket.accept();

            // Set variables for socket input and output
            Scanner input = new Scanner( clientSocket.getInputStream() );
            PrintStream output = new PrintStream( clientSocket.getOutputStream() );

            // Display input from client
            String clientMessage = input.nextLine();
            System.out.println( "Client sent: " + clientMessage );

            // Send output to client
            String outgoingMessage = "I received the message \"" + clientMessage + "\"";
            output.println( outgoingMessage );

            // Close things
            input.close();
            clientSocket.close();
        }

        serverSocket.close();

    }

    public static void main( String[] args ) throws Exception {
        ServerProcess server = new ServerProcess();
        server.start( 5000 );
    }
}