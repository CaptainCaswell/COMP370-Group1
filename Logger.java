import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" );
    private PrintWriter fileWriter;
    private String logFileName;
    private static MonitorUI monitorUI= null;

    // Constructor
    public Logger( String logFileName ) {
        // Make log folder if needed
        File logsDir = new File( "logs" );
        if ( !logsDir.exists() ) logsDir.mkdir();

        this.logFileName = "logs/" + logFileName;

        try {
            fileWriter = new PrintWriter( new FileWriter( this.logFileName, true ), true );
        } catch ( IOException e ) {
            System.err.println( "Failed to create log: " + this.logFileName );
        }
    }

    // Attach logger to UI
    public static void setMonitorUI( MonitorUI ui ) {
        monitorUI = ui;
    }

    // Create timestamp
    public static String getTimeStamp() {
        return "[" + dateFormat.format( new Date() ) + "] ";
    }

    // Log message without level
    public void log( String message ) {
        // Default to logging to console and logfile
        log( message, 2 );
    }

    // Log message with level
    public void log( String message, int logLevel ) {
        String timestampedMessage = getTimeStamp() + message;
        
        switch ( logLevel ) {
            // MonitorUI
            case 3:
                if ( monitorUI != null ) monitorUI.addLogMessage ( timestampedMessage );
            
            // File
            case 2:
                if (fileWriter != null ) fileWriter.println( timestampedMessage );
            
            // Console
            case 1:
                System.out.println( timestampedMessage );

            // No log
            case 0:
                break;
        }
    }


    public void close() {
        if ( fileWriter != null ) {
            fileWriter.close();
        }
    }


}
