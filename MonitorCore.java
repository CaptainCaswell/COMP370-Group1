import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonitorCore {
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private final int port;
    private final Logger logger;

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    private int primaryServerID = 0;
    private int lastPrimarySum = 0;

    private final Map<Integer, ServerInfo> servers = new HashMap<>();
    private final Map<Integer, NodeInfo> clients = new HashMap<>();

    private static final long FAILURE_CHECK_INTERVAL_MS = 500;

    public MonitorCore(int port) {
        this.port = port;
        this.logger = new Logger("monitor.log");

        Thread detector = new Thread(new FailureDetector(this, logger, FAILURE_CHECK_INTERVAL_MS));
        detector.setDaemon(true);
        detector.start();
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            logger.log("Monitor started on port " + port);

            while (running) {
                Socket incomingSocket = serverSocket.accept();
                threadPool.submit(new MonitorConnectionHandler(incomingSocket, this, logger));
            }
        } catch (IOException e) {
            logger.log("Monitor accept loop error: " + e.getMessage());
        } finally {
            safeClose();
        }
    }

    public synchronized String processRequest(String message) {
        if (message.startsWith("SERVER_HEARTBEAT")) {
            return serverHeartbeat(message);
        }
        if (message.startsWith("CLIENT_HEARTBEAT")) {
            return clientHeartbeat(message);
        }
        return "ERROR";
    }

    private synchronized String serverHeartbeat(String message) {
        boolean isNew = false;

        String[] split = message.split(" ");
        if (split.length != 3) return "HEARTBEAT SYNTAX ERROR";

        int serverID = Integer.parseInt(split[1]);
        int sum = Integer.parseInt(split[2]);

        if (!servers.containsKey(serverID)) {
            servers.put(serverID, new ServerInfo(serverID));
            isNew = true;
        }

        if (primaryServerID == 0) primaryServerID = serverID;

        ServerInfo server = servers.get(serverID);
        server.updateHeartbeat(sum);

        String type;
        if (primaryServerID == serverID) {
            if (sum >= lastPrimarySum) lastPrimarySum = sum;
            type = "PRIMARY";
        } else {
            type = "SECONDARY";
        }

        if (isNew) {
            logger.log("Server " + serverID + " added. Currently " + type);
        } else {
            // keep minimal output if you want
            // logger.log(type + " Server " + serverID + " heartbeat received");
        }

        return type + " " + lastPrimarySum;
    }

    private synchronized String clientHeartbeat(String message) {
        String[] split = message.split(" ");
        if (split.length != 2) return "HEARTBEAT SYNTAX ERROR";

        int clientID = Integer.parseInt(split[1]);

        if (!clients.containsKey(clientID)) {
            clients.put(clientID, new NodeInfo(clientID));
        }

        return getPrimary();
    }

    private synchronized String getPrimary() {
        if (primaryServerID == 0) return "NONE";
        NodeInfo primary = servers.get(primaryServerID);
        if (primary == null) return "NONE";
        return "CURRENT_PRIMARY " + primary.nodeID;
    }

    // called by FailureDetector
    public synchronized void checkFailures() {
        if (primaryServerID == 0) return;

        NodeInfo primary = servers.get(primaryServerID);
        if (primary == null) {
            primaryServerID = 0;
            return;
        }

        if (!primary.isAlive()) {
            logger.log("### PRIMARY FAILURE ###");
            servers.remove(primaryServerID);
            serverFailover();
        }

        // remove dead secondaries
        servers.entrySet().removeIf(entry -> {
            NodeInfo server = entry.getValue();
            if (server.nodeID != primaryServerID && !server.isAlive()) {
                logger.log("Secondary server " + server.nodeID + " not responding. Removed from list");
                return true;
            }
            return false;
        });
    }

    private synchronized void serverFailover() {
        Integer candidateID = null;

        for (Map.Entry<Integer, ServerInfo> entry : servers.entrySet()) {
            NodeInfo server = entry.getValue();
            if (server.isAlive()) {
                if (candidateID == null || server.nodeID < candidateID) {
                    candidateID = server.nodeID;
                }
            }
        }

        if (candidateID != null) {
            primaryServerID = candidateID;
            logger.log("FAILOVER to Server " + primaryServerID +
                    " with restored sum " + lastPrimarySum + " on next heartbeat");
        } else {
            primaryServerID = 0;
            logger.log("FAILOVER not possible - No servers available");
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

    public synchronized int getPrimarySum() {
        return lastPrimarySum;
    }

    public synchronized Map<Integer,ServerInfo> getServers() {
        return servers;
    }

    public synchronized Map<Integer,NodeInfo> getClients() {
        return clients;
    }

    protected synchronized int getPrimaryID() {
        return primaryServerID;
    }
}
