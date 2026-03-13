import java.util.HashMap;
import java.util.Map;

public class LoggerFactory {
    // Singleton instance
    private static LoggerFactory instance = null;
    
    // Map to store Logger instances by filename
    private Map<String, Logger> loggers = new HashMap<>();
    
    // MonitorUI reference
    private MonitorUI monitorUI = null;

    // Private constructor
    private LoggerFactory() {

    }

    public static synchronized LoggerFactory getInstance() {
        // Check if instance exists
        if ( instance == null ) {
            instance = new LoggerFactory();
        }

        return instance;
    }

    // getLogger(String filename)
    public synchronized Logger getLogger( String name ) {
        // Check if logger already made for that name
        if ( loggers.containsKey( name ) ) {
            return loggers.get( name );
        }

        // Construct new logger
        Logger logger = new Logger( name );
        
        if ( monitorUI != null ) logger.setMonitorUI( monitorUI );

        // Put in map
        loggers.put( name, logger );

        return logger;
    }

    public void setMonitorUI() {
        monitorUI = MonitorUI.getInstance();

        // Update all loggers
        for ( Logger logger : loggers.values() ) {
            logger.setMonitorUI( monitorUI );
        }
    }
}