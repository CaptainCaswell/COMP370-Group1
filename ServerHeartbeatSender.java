import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class ServerHeartbeatSender implements Runnable {
    private static final String host = "localhost";
    private static final int monitorPort = 9000;
    private static final long HEARTBEAT_INTERVAL = 500;
    
    private final int serverID;
    private final Server server;
    private final Logger logger;
    private volatile boolean running = true;

    public ServerHeartbeatSender(int serverID, Server server, Logger logger) {
        this.serverID = serverID;
        this.server = server;
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
            output.println("SERVER_HEARTBEAT " + serverID + " " + server.getSum());

            // Get monitor response
            String response = input.nextLine();

            // Break heartbeat into parts
            String[] parts = response.split(" ");
            String serverType = null;
            int updateSum = -1;

            // Check proper number of parts
            if (parts.length != 2) {
                logger.log("Unknown response to heartbeat: " + response);
            }
            
            // Record parts
            else {
                serverType = parts[0]; 
                updateSum = Integer.parseInt(parts[1]);
                server.setSum(updateSum);
            }
            
            // If PRIMARY received
            if (serverType.equals("PRIMARY")) {
                // If not already primary
                if (!server.getIsPrimary()) {
                    server.promote();
                    logger.log("Server promoted");
                }
                // If already primary
                else {
                    logger.log("Heartbeat acknowledged.");
                }
            }
            
            // If SECONDARY received
            else if (serverType.equals("SECONDARY")) {
                // Verify sum was is valid
                if (updateSum >= 0) {
                    // Update sum if sum is higher
                    server.setSum(updateSum);
                    logger.log("Heartbeat acknowledged");
                }
                
                // Invalid sum
                else {
                    logger.log("Heartbeat acknowledged but sum update was invalid");
                }
            }

            // Bad response
            else {
                logger.log("Unknown response to heartbeat: " + response);
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