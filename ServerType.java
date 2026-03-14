public class ServerType {
    private final String host;
    private final int port;
    private final int monitorPort;

    public ServerType(String host, int port, int monitorPort) {
        this.host = host;
        this.port = port;
        this.monitorPort = monitorPort;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public int getMonitorPort() { return monitorPort; }
}