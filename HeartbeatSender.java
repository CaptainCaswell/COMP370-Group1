import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

// Abstract heartbeat sender
public abstract class HeartbeatSender implements Runnable {
    protected static final String host = "localhost";
    protected static final int monitorPort = 9000;
    protected static final long HEARTBEAT_INTERVAL = 500;

    protected final Logger logger;
    protected volatile boolean running = true;

    public HeartbeatSender(Logger logger) {
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

    // Each subclass must implement its own heartbeat logic
    protected abstract void sendHeartbeat();

    public void stop() {
        running = false;
    }

    // Helper method for socket setup and teardown
    protected void withSocket( SocketAction action ) {
        try (Socket socket = new Socket(host, monitorPort);
             Scanner input = new Scanner(socket.getInputStream());
             PrintStream output = new PrintStream(socket.getOutputStream())) {

            action.perform(input, output);

        } catch (Exception e) {
            logger.log("Heartbeat failed: " + e.getMessage());
        }
    }

    // Functional interface for passing heartbeat actions
    @FunctionalInterface
    protected interface SocketAction {
        void perform( Scanner input, PrintStream output ) throws Exception;
    }
}