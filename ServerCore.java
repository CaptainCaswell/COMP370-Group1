import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerCore {
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private final ServerState state;
    private final Logger logger;

    private static final String HOST = "localhost";
    private static final int MONITOR_PORT = 9000;
    private static final long HEARTBEAT_INTERVAL_MS = 500;

    public ServerCore(int port) {
        this.state = new ServerState(port); // serverId = port (your current convention)
        this.logger = new Logger("server" + port + ".log");
    }

    public void start() {
        // Heartbeat thread
        Thread hb = new Thread(new HeartbeatSender(state, logger, HOST, MONITOR_PORT, HEARTBEAT_INTERVAL_MS));
        hb.setDaemon(true);
        hb.start();

        // Accept clients
        try {
            serverSocket = new ServerSocket(state.getServerId());
            logger.log("Server " + state.getServerId() + " started");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new ClientHandler(clientSocket, state, logger));
            }
        } catch (IOException e) {
            logger.log("Server start/accept loop error: " + e.getMessage());
        } finally {
            safeClose();
        }
    }

    public void stop() {
        running = false;
        safeClose();
        threadPool.shutdownNow();
    }

    private void safeClose() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }
}
