public class ServerInfo extends NodeInfo {
    private int sum;

    public ServerInfo( int nodeID ) {
        super( nodeID );
        this.sum = 0;
    }

    // Update heartbeat
    public void updateHeartbeat( int sum ) {
        this.sum = sum;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public int getSum() {
        return sum;
    }
    
}
