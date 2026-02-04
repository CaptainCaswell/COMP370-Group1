import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ServerState state;
    private final Logger logger;

    public ClientHandler(Socket clientSocket, ServerState state, Logger logger) {
        this.clientSocket = clientSocket;
        this.state = state;
        this.logger = logger;
    }

    @Override
    public void run() {
        try (Socket socket = clientSocket) {
            if (!state.isPrimary()) {
                try (PrintStream output = new PrintStream(socket.getOutputStream())) {
                    output.println("NOT_PRIMARY");
                }
                logger.log("Rejected client connection as SECONDARY");
                return;
            }

            try (Scanner input = new Scanner(socket.getInputStream());
                 PrintStream output = new PrintStream(socket.getOutputStream())) {

                String clientMessage = input.nextLine();
                String outgoingMessage = processMessage(clientMessage);
                output.println(outgoingMessage);
            }

        } catch (IOException e) {
            logger.log("Error handling client: " + e.getMessage());
        }
    }

    private String processMessage(String clientMessage) {
        try {
            int number = Integer.parseInt(clientMessage.trim());
            int newSum = state.addToSum(number);

            logger.log("Received from client: " + clientMessage);
            logger.log("Sum updated to " + newSum);

            return "SUCCESS " + newSum;
        } catch (NumberFormatException e) {
            logger.log("Invalid input from client: " + clientMessage);
            return "FAILED " + state.getSum();
        }
    }
}
