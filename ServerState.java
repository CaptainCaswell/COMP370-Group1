public class ServerState {
    private final int serverId;
    private int sum = 0;
    private boolean isPrimary = false;

    public ServerState(int serverId) {
        this.serverId = serverId;
    }

    public int getServerId() {
        return serverId;
    }

    public synchronized boolean isPrimary() {
        return isPrimary;
    }

    public synchronized void promote() {
        isPrimary = true;
    }

    public synchronized void demote() {
        isPrimary = false;
    }

    public synchronized int getSum() {
        return sum;
    }

    public synchronized int addToSum(int n) {
        sum += n;
        return sum;
    }

    // Restore from monitor if higher
    public synchronized void restoreSumIfHigher(int newSum, Logger logger) {
        if (newSum >= 0 && newSum > sum) {
            sum = newSum;
            logger.log("Sum restored to " + newSum);
        }
    }
}
