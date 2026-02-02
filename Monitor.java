import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;

public class Monitor {
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    protected ServerSocket serverSocket; // Socket for this program
    protected int port; // Port for this program
    protected boolean running = true; // Flag to keep while loop going
    protected Logger logger;

    protected int primaryServerID = 0; // Port for primary server
    protected int lastPrimarySum = 0; // Stores sum received from primary server

    protected Map<Integer, ServerInfo> servers = new HashMap<>(); // Map of servers, ServerID is key
    protected Map<Integer, ClientInfo> clients = new HashMap<>(); // Map of servers, ServerID is key

    protected static final long INTERVAL = 500; // Time between connection checks in milliseconds
    protected static final long TIMEOUT = 1500; // Time to consider connection lost in milliseconds

    // Inner class to organize connection information
    protected static class ServerInfo {
        int serverID;
        int sum;
        long lastHeartbeat;

        ServerInfo( int serverID, int sum ) {
            this.serverID = serverID;
            this.sum = sum;
            this.lastHeartbeat = System.currentTimeMillis();
        }

        void updateHeartbeat( int sum ) {
            this.sum = sum;
            this.lastHeartbeat = System.currentTimeMillis();
        }

        boolean isAlive() {
            return (System.currentTimeMillis() - lastHeartbeat) < TIMEOUT;
        }

    }

     protected static class ClientInfo {
        int clientID;
        int sum;
        long lastHeartbeat;

        ClientInfo( int clientID ) {
            this.clientID = clientID;
            this.lastHeartbeat = System.currentTimeMillis();
        }

        void updateHeartbeat( int sum ) {
            this.lastHeartbeat = System.currentTimeMillis();
        }

        boolean isAlive() {
            return (System.currentTimeMillis() - lastHeartbeat) < TIMEOUT;
        }

    }

    // Constructor
    public Monitor() {
        port = 9000;
        this.logger = new Logger( "monitor.log" );
        failureDetector();
    }
    
    public static void main( String[] args ) throws IOException {
        // Start server
        Monitor monitor = new Monitor();
        monitor.start();
    }

    // Start Basic Server
    public void start() throws IOException {
        // Open socket
        serverSocket = new ServerSocket( port );

        

        // Announce system running
        logger.log( "Monitor started on port " + port );        
        
        while ( running ) {
            
            // Waits for connection, returns socket when connected
            Socket incomingSocket = serverSocket.accept();

            threadPool.submit( () -> threadHeartbeat( incomingSocket ) );
        }

        serverSocket.close();
    }

    protected void threadHeartbeat( Socket incomingSocket ) {
        try {
            // Set variables for socket input and output
            Scanner input = new Scanner( incomingSocket.getInputStream() );
            PrintStream output = new PrintStream( incomingSocket.getOutputStream() );

            // Get input from client
            String message = input.nextLine();
            
            // Process and reply
            String outgoingMessage = processRequest( message );
            output.println( outgoingMessage );

            // Close things
            input.close();
            incomingSocket.close();

        } catch (IOException e ) {
            logger.log( "Attempted heartbeat connection failed" );
        }
    }

    protected boolean checkPrimary() {
        return primaryServerID != 0;
    }

    protected synchronized String processRequest( String message ) {

        // Server
        if ( message.startsWith( "SERVER_HEARTBEAT") ) {
            return serverHeartbeat( message );
        }

        // Client
        if ( message.startsWith( "CLIENT_HEARTBEAT") ) {
            return clientHeartbeat( message );
        }

        return "ERROR";
    }

    // Accepts server heartbeat, returns server type
    protected String serverHeartbeat( String message ) {
        boolean isNew = false;
        // Message should be: SERVER_HEARTBEAT serverID port sum
        String[] splitMessage = message.split( " " );

        if ( splitMessage.length != 3) {
            return "HEARTBEAT SYNTAX ERROR";
        }

        int serverID = Integer.parseInt( splitMessage[1] );
        int sum = Integer.parseInt( splitMessage[2] );

        // Add server to map if not already added (first heartbeat)
        if ( !servers.containsKey( serverID ) ) {
            // Add server to map
            ServerInfo newServer = new ServerInfo( serverID, sum );
            servers.put( serverID, newServer );
            isNew = true;
        }
        
        String type;
        
        // If no current primary, make primary
        if ( primaryServerID == 0 ) setPrimaryID( serverID );

        // Get server object
        ServerInfo server = servers.get( serverID );
        server.updateHeartbeat( sum );

        // If server is primary
        if ( primaryServerID == serverID ) {
            // Update latest sum
            if (sum >= lastPrimarySum) {
                lastPrimarySum = sum;
            }
            type = "PRIMARY";
        }

        // If server is secondary
        else {
            type = "SECONDARY";
        }

        // If server new logging
        if ( isNew ) {
            logger.log( "Server " + serverID + " added. Currently " + type + ".");
        }

        // If server not new logging
        else {
            logger.log( type + " Server " + serverID + " heartbeat recieved" );
        }

        if ( type.equals( "PRIMARY" ) ) type += " " + lastPrimarySum;

        return type;
    }

    protected String clientHeartbeat( String message ) {
        // Message should be: CLIENT_HEARTBEAT clientID
        String[] splitMessage = message.split( " " );

        if ( splitMessage.length != 2) {
            return "HEARTBEAT SYNTAX ERROR";
        }

        int clientID = Integer.parseInt( splitMessage[1] );
        
        // Add server to map if not already added (first heartbeat)
        if ( !clients.containsKey( clientID ) ) {
            // Add server to map
            ClientInfo newClient = new ClientInfo( clientID );
            clients.put( clientID, newClient );
        }

        return getPrimary();
    }

    protected synchronized String getPrimary() {
        if (primaryServerID == 0 ) {
            return "NONE";
        } else {
            ServerInfo primary = servers.get( primaryServerID );
            return "CURRENT_PRIMARY " + primary.serverID;
        }
    }

    protected synchronized void setPrimaryID( int newID ) {
        primaryServerID = newID;
    }

    protected synchronized int getPrimaryID() {
        return primaryServerID;
    }

    protected void failureDetector() {
        Thread detector = new Thread( () -> {
            while ( running ) {
                try {
                    Thread.sleep( INTERVAL );
                    if ( primaryServerID != 0 ) checkFailures();
                } catch ( InterruptedException e ) {
                    break;
                }
            }
        });

        detector.setDaemon( true );
        detector.start();
    }

    protected synchronized void checkFailures() {
        ServerInfo primary = servers.get( primaryServerID );

        if ( !primary.isAlive() ) {
            logger.log( "### PRIMARY FAILURE ###");
            
            servers.remove( primaryServerID );

            serverFailover( primary.sum );
        }

        servers.entrySet().removeIf( entry -> {
            ServerInfo server = entry.getValue();
            // Only trim secondary dead servers
            if ( server.serverID != primaryServerID && !server.isAlive() ) {
                logger.log( "Secondary server " + server.serverID + " not responding. Removed from list." );
                return true;
            }

            return false;
        });
    }

    protected void serverFailover( int oldSum ) {
        Integer candidateID = null;

        // Find the lowest ID server (highest uptime)
        for ( Map.Entry<Integer, ServerInfo> entry : servers.entrySet() ) {
            ServerInfo server = entry.getValue();
            
            // Check server is alive
            if ( server.isAlive() ) {
                // If no current candidate or current candidate is higher
                if ( candidateID == null || server.serverID < candidateID ) {
                    candidateID = server.serverID;
                }
            }
        }

        // If candidate found
        if ( candidateID != null ) {
            primaryServerID = candidateID;
            lastPrimarySum = oldSum;
            logger.log( "FAILOVER to Server " + primaryServerID + " with restored sum " + lastPrimarySum + " on next heartbeat." );
            
        }
        
        // If no candidates
        else {
            primaryServerID = 0;
            logger.log( "FAILOVER not possible. No servers available." );
        }
    }

}