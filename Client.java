import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main ( String[] args ) throws Exception {
        String host = "localhost";
        int port = 5000;

        System.out.println( " ---- CLIENT ---- " );

        // Open connection to server
        Socket socket = new Socket( host, port );

        // Set variables for socket input and output
        Scanner input = new Scanner( socket.getInputStream() );
        PrintStream output = new PrintStream( socket.getOutputStream() );

        // Send this to the server
        String outgoingMessage = "12345";
        System.out.println( "Sending the following to server: " + outgoingMessage );
        output.println( outgoingMessage );

        // Display response from server
        String clientMessage = input.nextLine();
        System.out.println( "Server response: " + clientMessage );

        // Close things
        input.close();
        socket.close();
    }
}