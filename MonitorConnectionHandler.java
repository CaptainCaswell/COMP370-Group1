import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class MonitorConnectionHandler implements Runnable {
    private final Socket socket;
    private final MonitorCore core;
    private final Logger logger;

    public MonitorConnectionHandler(Socket socket, MonitorCore core, Logger logger) {
        this.socket = socket;
        this.core = core;
        this.logger = logger;
    }

    @Override
    public void run() {
        try (Socket s = socket;
             Scanner input = new Scanner(s.getInputStream());
             PrintStream output = new PrintStream(s.getOutputStream())) {

            String message = input.nextLine();
            String reply = core.processRequest(message);
            output.println(reply);

        } catch (IOException e) {
            logger.log("Monitor connection handler failed: " + e.getMessage());
        }
    }
}
