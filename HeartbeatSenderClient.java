public class HeartbeatSenderClient extends HeartbeatSender {
    private final int clientID;
    private final Client client;

    public HeartbeatSenderClient( int clientID, Client client, Logger logger ) {
        super( logger );
        this.clientID = clientID;
        this.client = client;
    }

    @Override
    protected void sendHeartbeat() {
        withSocket( ( input, output ) -> {
            output.println( "CLIENT_HEARTBEAT " + clientID );

            logger.log( "Heartbeat sent" );

            String response = input.nextLine();

            // Check for shutdown command
            if ( response.equals( "SHUTDOWN" ) ) {
                logger.log( "Shutdown command recieved, closing..." );
                client.shutdown();
            }

            // If monitor reports no primary
            if ( response.equals( "NONE" ) ) {
                logger.log( "No primary server available." );
            }
            
            // Check other monitor responses
            else {
                if (response.startsWith( "CURRENT_PRIMARY" )) {
                    // Check if primary is new
                    if ( client.setPrimary( Integer.parseInt( response.split( " " )[1] ) ) ) {
                        logger.log( "New primary found, switching" );
                    } else {
                        logger.log( "Heartbeat acknowledged" );
                    }
                    
                } else {
                    logger.log( "Unknown response: " + response );
                }
            }
        });
    }
}
