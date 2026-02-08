import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

    protected Map<Integer, ServerInfo> servers = new HashMap<>(); // Map of servers, serverID is key
    protected Map<Integer, NodeInfo> clients = new HashMap<>(); // Map of clients, clientID is key

    protected static final long INTERVAL = 500; // Time between connection checks in milliseconds

    // Constructor
    public Monitor() {
        port = 9000;
        this.logger = new Logger( "monitor.log" );
        failureDetector();
    }
    
    public static void main( String[] args ) throws IOException {
        // Create monitor instance
        Monitor monitor = new Monitor();

        // Check for arg and if it is UI
        if ( args.length > 0 && args[0].equals( "ui" ) ) {
            // Launch UI
            javax.swing.SwingUtilities.invokeLater( () -> {
                MonitorUI ui = new MonitorUI( monitor );
                Logger.setMonitorUI( ui );
                ui.setVisible( true );
            });
        }

        // Start monitor
        monitor.start();
    }

    // Start Basic Server
    public void start() throws IOException {
        // Open socket
        serverSocket = new ServerSocket( port );

        // Announce system running
        logger.log( "Monitor started on port " + port, 3 );        
        
        while ( running ) {
            // Waits for connection, returns socket when connected
            Socket incomingSocket = serverSocket.accept();

            threadPool.submit( new HeartbeatProcess( incomingSocket, this, logger ) );
        }

        serverSocket.close();
    }

    protected boolean checkPrimary() {
        return primaryServerID != 0;
    }

    // Accepts server heartbeat, returns server type
    protected String processHeartbeat( String message ) {
        boolean isNew = false;
        int expectedArgs;
        String returnMessage = null;

        // Server message should be: SERVER_HEARTBEAT serverID port sum
        // Client message should be: CLIENT_HEARTBEAT clientID

        String[] splitMessage = message.split( " " );

        // Get number of expected arguments
        if (message.startsWith( "SERVER_HEARTBEAT" )) {
            expectedArgs = 3;
        } else if ( message.startsWith( "CLIENT_HEARTBEAT" )) {
            expectedArgs = 2;
        } else {
            logger.log( "Unknown heartbeat preamble: " + message, 2 );
            return "HEARTBEAT SYNTAX ERROR";
        }

        // Make sure arguments are correct
        if ( splitMessage.length != expectedArgs ) {
            logger.log( "Incorrect number of heartbeat arguments: " + message, 2 );
            return "HEARTBEAT SYNTAX ERROR";
        }

        // Set ID
        int id = Integer.parseInt( splitMessage[1] );

        // Server only
        if ( expectedArgs == 3 ) {
            // Set sum from heartbeat
            int sum = Integer.parseInt( splitMessage[2] );

            // Add server to map if not already added (first heartbeat)
            if ( !servers.containsKey( id ) ) {
                // Add server to map
                ServerInfo newServer = new ServerInfo( id );
                servers.put( id, newServer );
                isNew = true;
                logger.log( "Server " + id + " added", 3 );
            }
            
            String type;
            
            // If no current primary, make primary
            if ( primaryServerID == 0 ) setPrimaryID( id );

            // Get server object and update it
            ServerInfo server = servers.get( id );
            server.updateHeartbeat( sum );

            // If server is primary
            if ( primaryServerID == id ) {
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
                logger.log( "Server " + id + " added, currently " + type + "", 3);
            }

            // If server not new logging
            else {
                logger.log( type + " Server " + id + " heartbeat recieved", 2 );
            }

            returnMessage = type + " " + lastPrimarySum;
        } else {
            // Add client to map if not already added (first heartbeat)
            if ( !clients.containsKey( id ) ) {
                // Add server to map
                NodeInfo newClient = new NodeInfo( id );
                clients.put( id, newClient );
                logger.log( "Client " + id + " added", 3 );
            }

            // Get client object and update it
            NodeInfo client = clients.get( id );
            client.updateHeartbeat();

            returnMessage = getPrimary();
        }

        logger.log( ( (expectedArgs == 2) ? "Client " : "Server " ) + id + " heartbeat recieved", 2 );

        if ( returnMessage != null) {
            return returnMessage;
        } else {
            logger.log( "No heartbeat return from input: " + message, 2 );
            return "HEARTBEAT SYNTAX ERROR";
        }
    }

    protected synchronized String getPrimary() {
        if (primaryServerID == 0 ) {
            return "NONE";
        } else {
            NodeInfo primary = servers.get( primaryServerID );
            return "CURRENT_PRIMARY " + primary.nodeID;
        }
    }

    protected synchronized void setPrimaryID( int newID ) {
        primaryServerID = newID;
    }

    protected synchronized int getPrimaryID() {
        return primaryServerID;
    }

    protected synchronized int getPrimarySum() {
        return lastPrimarySum;
    }

    protected synchronized Map<Integer,ServerInfo> getServers() {
        return servers;
    }

    protected synchronized Map<Integer,NodeInfo> getClients() {
        return clients;
    }

    protected void failureDetector() {
        Thread detector = new Thread( () -> {
            while ( running ) {
                try {
                    Thread.sleep( INTERVAL );
                    checkFailures();
                } catch ( InterruptedException e ) {
                    break;
                }
            }
        });

        detector.setDaemon( true );
        detector.start();
    }

    protected synchronized void checkFailures() {
        
        // Check primary failures, if there is a primary
        if ( primaryServerID != 0 ) {
            NodeInfo primary = servers.get( primaryServerID );

            if ( primary != null && !primary.isAlive() ) {
                logger.log( "Primary server " + primaryServerID + " not responding, Removing and triggering failover", 3 );
                
                servers.remove( primaryServerID );

                serverFailover();
            }
        }   

        // Check secondary failures
        servers.entrySet().removeIf( entry -> {
            NodeInfo server = entry.getValue();

            // Only trim secondary dead servers
            if ( server.nodeID != primaryServerID && !server.isAlive() ) {
                logger.log( "Secondary server " + server.nodeID + " not responding, removed from list", 3 );
                return true;
            }

            return false;
        });

        // Check client failures
        clients.entrySet().removeIf( entry -> {
            NodeInfo client = entry.getValue();

            if ( !client.isAlive() ) {
                logger.log( "Client " + client.nodeID + " not responding, removed from list", 3 );
                
                // Trim
                return true;
            }

            // Don't trim
            return false;
        });
    }

    protected void serverFailover( ) {
        Integer candidateID = null;

        // Find the lowest ID server (highest uptime)
        for ( Map.Entry<Integer, ServerInfo> entry : servers.entrySet() ) {
            NodeInfo server = entry.getValue();
            
            // Check server is alive
            if ( server.isAlive() ) {
                // If no current candidate or current candidate is higher
                if ( candidateID == null || server.nodeID < candidateID ) {
                    candidateID = server.nodeID;
                }
            }
        }

        // If candidate found
        if ( candidateID != null ) {
            primaryServerID = candidateID;
            logger.log( "FAILOVER to Server " + primaryServerID + " with restored sum " + lastPrimarySum + " on next heartbeat", 3 );
            
        }
        
        // If no candidates
        else {
            primaryServerID = 0;
            logger.log( "FAILOVER not possible - No servers available", 3 );
        }
    }
}