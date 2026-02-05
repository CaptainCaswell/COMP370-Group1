import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientHeartbeatSender implements Runnable {
    private static final String host = "localhost";
    private static final int monitorPort = 9000;
    private static final long HEARTBEAT_INTERVAL = 500;
    
    private final int clientID;
    private final Client client;
    private final Logger logger;
    private volatile boolean running = true;

    public ClientHeartbeatSender(int clientID, Client client, Logger logger) {
        this.clientID = clientID;
        this.client = client;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            while (running) {
                sendHeartbeat();
                Thread.sleep(HEARTBEAT_INTERVAL);
            }
        } catch (Exception e) {
            logger.log("Heartbeat thread error: " + e.getMessage());
        }
    }

    private void sendHeartbeat() {
        try {
            // Setup Socket with input and output
            Socket socket = new Socket(host, monitorPort);
            Scanner input = new Scanner(socket.getInputStream());
            PrintStream output = new PrintStream(socket.getOutputStream());

            // Request primary or send heartbeat
            output.println("CLIENT_HEARTBEAT " + clientID);

            // Get monitor response
            String response = input.nextLine();

            if (response.startsWith("CURRENT_PRIMARY")) {
                client.setPrimary(Integer.parseInt(response.split(" ")[1]));
            } else if (response.equals("ACK")) {
                logger.log("Heartbeat acknowledged.");
            } else if (response.equals("NONE")) {
                logger.log("No primary server available.");
            }

            input.close();
            output.close();
            socket.close();
        } catch (Exception e) {
            logger.log("Heartbeat failed");
        }
    }

    public void stop() {
        running = false;
    }
}