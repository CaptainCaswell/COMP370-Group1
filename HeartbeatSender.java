import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class HeartbeatSender implements Runnable {
    private final ServerState state;
    private final Logger logger;

    private final String host;
    private final int monitorPort;
    private final long intervalMs;

    public HeartbeatSender(ServerState state, Logger logger, String host, int monitorPort, long intervalMs) {
        this.state = state;
        this.logger = logger;
        this.host = host;
        this.monitorPort = monitorPort;
        this.intervalMs = intervalMs;
    }

    @Override
    public void run() {
        while (true) {
            try {
                sendOneHeartbeat();
                Thread.sleep(intervalMs);
            } catch (InterruptedException ie) {
                logger.log("Heartbeat thread interrupted, stopping.");
                return;
            } catch (Exception e) {
                // Keep running even if a heartbeat fails
                logger.log("Heartbeat failed: " + e.getMessage());
            }
        }
    }

    private void sendOneHeartbeat() throws Exception {
        try (Socket socket = new Socket(host, monitorPort);
             Scanner input = new Scanner(socket.getInputStream());
             PrintStream output = new PrintStream(socket.getOutputStream())) {

            int id = state.getServerId();
            int sum = state.getSum();

            output.println("SERVER_HEARTBEAT " + id + " " + sum);

            String response = input.nextLine();
            String[] parts = response.split(" ");

            if (parts.length != 2) {
                logger.log("Unknown response to heartbeat: " + response);
                return; // IMPORTANT: stop processing this response
            }

            String role = parts[0];
            int restoredSum;
            try {
                restoredSum = Integer.parseInt(parts[1]);
            } catch (NumberFormatException nfe) {
                logger.log("Invalid sum in heartbeat response: " + response);
                return;
            }

            // Update sum (secondary must keep up-to-date)
            state.restoreSumIfHigher(restoredSum, logger);

            // Split-brain prevention: ALWAYS obey monitor role
            if ("PRIMARY".equals(role)) {
                if (!state.isPrimary()) {
                    state.promote();
                    logger.log("Server promoted to PRIMARY by monitor");
                } else {
                    // optional: keep minimal
                    // logger.log("Heartbeat acknowledged as PRIMARY");
                }
            } else if ("SECONDARY".equals(role)) {
                if (state.isPrimary()) {
                    state.demote();
                    logger.log("Server demoted to SECONDARY by monitor (split-brain prevention)");
                } else {
                    // logger.log("Heartbeat acknowledged as SECONDARY");
                }
            } else {
                logger.log("Unknown role in heartbeat response: " + response);
            }
        }
    }
}
