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
            String response = input.nextLine();

            switch ( response ) {
                case "ACK":
                    logger.log( "Heartbeat acknowledged." );
                    break;
                case "NONE":
                    logger.log( "No primary server available." );
                    break;
                default:
                    if (response.startsWith( "CURRENT_PRIMARY" )) {
                        client.setPrimary( Integer.parseInt( response.split( " " )[1] ) );
                    } else {
                        logger.log( "Unknown response: " + response );
                    }
                    break;
            }
        });
    }
}
