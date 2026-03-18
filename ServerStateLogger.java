public class ServerStateLogger implements Observer {
    private Logger logger;

    public ServerStateLogger() {
        this.logger = LoggerFactory.getInstance().getLogger("observer.log");
    }

    @Override
    public void update(String event, int nodeID) {
        if (event.equals("SERVER_FAILURE")) {
            logger.log("Observer noticed: Server " + nodeID + " failed", 3);
        } 
        else if (event.equals("PRIMARY_CHANGED")) {
            logger.log("Observer noticed: Server " + nodeID + " is now PRIMARY", 3);
        }
    }
}