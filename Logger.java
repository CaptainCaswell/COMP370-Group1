import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static SimpleDateFormat dateFormat = new SimpleDateFormat( "yyy-MM-dd HH:mm:ss.SSS" );
    private PrintWriter fileWriter;
    private String logFileName;

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

    // Create timestamp
    public static String getTimeStamp() {
        return "[" + dateFormat.format( new Date() ) + "] ";
    }

    // Log message
    public void log( String message ) {
        String timestampedMessage = getTimeStamp() + message;
        
        // Output to console
        System.out.println( timestampedMessage );

        // Output to file if fileWriter working
        if (fileWriter != null ) {
            fileWriter.println( timestampedMessage );
        }
    }

    public void close() {
        if ( fileWriter != null ) {
            fileWriter.close();
        }
    }


}
