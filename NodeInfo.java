public class NodeInfo {
    public int nodeID;
    public long lastHeartbeat;
    protected boolean shutdown = false;

    public static final long TIMEOUT = 1500;

    // Constructor
    public NodeInfo(int nodeID) {
        this.nodeID = nodeID;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    // Update heartbeat
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    // Check if node is alive
    public boolean isAlive() {
        return (System.currentTimeMillis() - lastHeartbeat) < TIMEOUT;
    }

    public void setShutdown() {
        this.shutdown = true;
    }

    public boolean checkShutdown() {
        return shutdown;
    }
}
