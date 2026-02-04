public class ServerMain {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect Syntax. Enter \"java ServerMain <port>\".");
            return;
        }

        int port = Integer.parseInt(args[0]);

        ServerCore server = new ServerCore(port);
        server.start();
    }
}
