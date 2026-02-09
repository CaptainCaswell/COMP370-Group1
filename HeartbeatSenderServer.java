public class HeartbeatSenderServer extends HeartbeatSender {
    private final int serverID;
    private final Server server;

    public HeartbeatSenderServer( int serverID, Server server, Logger logger ) {
        super( logger );
        this.serverID = serverID;
        this.server = server;
    }

    @Override
    protected void sendHeartbeat() {
        withSocket( ( input, output ) -> {
            output.println( "SERVER_HEARTBEAT " + serverID + " " + server.getSum() );

            logger.log( "Heartbeat sent" );
            
            String response = input.nextLine();

            // Check for shutdown command
            if ( response.equals( "SHUTDOWN" ) ) {
                logger.log( "Shutdown command recieved, closing..." );
                server.shutdown();
            }

            String[] parts = response.split( " " );

            // Check if correct number of parts
            if (parts.length != 2) {
                logger.log( "Unknown response: " + response );
                return;
            }

            String serverType = parts[0];
            int updateSum = Integer.parseInt( parts[1] );
            server.setSum( updateSum );

            switch ( serverType ) {
                case "PRIMARY":
                    // If reply is PRIMARY, but server thinks it's a secondary
                    if ( !server.getIsPrimary() ) {
                        server.promote();
                        logger.log( "Server promoted to PRIMARY" );

                    // If reply is PRIMARY, and server knows it's primary
                    } else {
                        logger.log( "Heartbeat acknowledged" );
                    }
                    break;
                case "SECONDARY":
                    // Check if server thinks it is primary
                    if ( server.getIsPrimary() ) {
                        server.demote();
                    }

                    // Check if sum is valid
                    if (updateSum >= 0) {
                        server.setSum( updateSum );
                        logger.log("Heartbeat acknowledged.");
                    } else {
                        logger.log("Heartbeat acknowledged but sum invalid.");
                    }
                    break;
                default:
                    logger.log("Unknown response: " + response);
            }
        });
    }
}
