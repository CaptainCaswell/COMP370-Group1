import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class HeartbeatProcess implements Runnable {
    private final Socket socket;
    private final Monitor monitor;
    private final Logger logger;

    public HeartbeatProcess( Socket socket, Monitor monitor, Logger logger ) {
        this.socket = socket;
        this.monitor = monitor;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            // Set variables for socket input and output
            Scanner input = new Scanner( socket.getInputStream() );
            PrintStream output = new PrintStream( socket.getOutputStream() );

            // Get input from client
            String message = input.nextLine();
            
            // Process and reply
            String outgoingMessage = monitor.processHeartbeat( message );
            output.println( outgoingMessage );

            // Close things
            input.close();
            socket.close();

        } catch (IOException e) {
            logger.log( "Attempted heartbeat connection failed" );
        }
    }
}