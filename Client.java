import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main ( String[] args ) throws Exception {
        boolean running = true;

        if ( args.length != 1 ) {
            System.out.println( "Incorrect arguments. Include port number and message." );
            return;
        }
        
        String host = "localhost";
        int port = Integer.parseInt( args[0] );

        Scanner user = new Scanner( System.in );

        while( running ) {
            System.out.println( "What do you want to send?" );

            String outgoingMessage = user.nextLine();

            System.out.println( " ---- CLIENT ---- " );

            // Make connection
            Socket socket = new Socket( host, port );

            // Set variables for socket input and output
            Scanner input = new Scanner( socket.getInputStream() );
            PrintStream output = new PrintStream( socket.getOutputStream() );

            // Send this to the server
            System.out.println( "Sending the following to server: " + outgoingMessage );
            output.println( outgoingMessage );

            // Display response from server
            String clientMessage = input.nextLine();
            System.out.println( "Server response: " + clientMessage );

            // Close things
            input.close();
            socket.close();
            
        }

        user.close();
    }
}