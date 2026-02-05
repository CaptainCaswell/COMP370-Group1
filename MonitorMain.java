public class MonitorMain {
    public static void main(String[] args) {
        MonitorCore monitor = new MonitorCore(9000);

        // Launch UI
        javax.swing.SwingUtilities.invokeLater(() -> {
            MonitorUI ui = new MonitorUI(monitor);
            ui.setVisible(true);
        });

        monitor.start();
    }
}
