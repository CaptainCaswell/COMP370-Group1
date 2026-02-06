import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

public class MonitorUI extends JFrame {
    private Monitor monitor;

    // Top bar components
    private JLabel primaryLabel;
    private JLabel sumLabel;
    private JButton killPrimaryButton;

    // Client list
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;
    private JButton startClientButton;
    private JButton killClientButton;

    // Secondary server list
    private DefaultListModel<String> secondaryListModel;
    private JList<String> secondaryList;
    private JButton startServerButton;
    private JButton killSecondaryButton;

    // Track running processes for starting/killing
    private ArrayList<Process> clientProcesses;
    private ArrayList<Process> serverProcesses;

    // Log panel
    private JTextArea logArea;
    private JScrollPane logScrollPane;

    // Auto-increment port for new nodes
    private int nextServerID = 2000;
    private int nextClientID = 1;

    public MonitorUI(Monitor monitor) {
        this.monitor = monitor;
        this.clientProcesses = new ArrayList<>();
        this.serverProcesses = new ArrayList<>();

        initializeUI();
        startUpdateThread();
    }

    private void initializeUI() {
        setTitle("Server Redundancy Management System - Monitor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout(10, 10));

        // Top panel
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Main panel with two columns
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        mainPanel.add(createClientPanel());
        mainPanel.add(createSecondaryPanel());
        add(mainPanel, BorderLayout.CENTER);

        // Log panel at bottom
        JPanel logPanel = createLogPanel();
        add(logPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null); // Center on screen
        log("Monitor UI started");
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        JLabel primaryTitle = new JLabel("Primary Server:");
        primaryTitle.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(primaryTitle);

        primaryLabel = new JLabel("None");
        primaryLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        primaryLabel.setForeground(Color.BLUE);
        panel.add(primaryLabel);

        JLabel sumTitle = new JLabel("Current Sum:");
        sumTitle.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(sumTitle);

        sumLabel = new JLabel("0");
        sumLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        sumLabel.setForeground(Color.BLUE);
        panel.add(sumLabel);

        killPrimaryButton = new JButton("Kill Primary");
        killPrimaryButton.setBackground(new Color(220, 50, 50));
        killPrimaryButton.setForeground(Color.WHITE);
        killPrimaryButton.setFocusPainted(false);
        killPrimaryButton.addActionListener(e -> killPrimary());
        panel.add(killPrimaryButton);

        return panel;
    }

    private JPanel createClientPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("Clients"));

        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(clientList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        startClientButton = new JButton("Start Client");
        startClientButton.setBackground(new Color(50, 150, 50));
        startClientButton.setForeground(Color.WHITE);
        startClientButton.setFocusPainted(false);
        startClientButton.addActionListener(e -> startClient());
        buttons.add(startClientButton);

        killClientButton = new JButton("Kill Selected");
        killClientButton.setBackground(new Color(220, 50, 50));
        killClientButton.setForeground(Color.WHITE);
        killClientButton.setFocusPainted(false);
        killClientButton.addActionListener(e -> killSelectedClient());
        buttons.add(killClientButton);

        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSecondaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("Secondary Servers"));

        secondaryListModel = new DefaultListModel<>();
        secondaryList = new JList<>(secondaryListModel);
        secondaryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(secondaryList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        startServerButton = new JButton("Start Server");
        startServerButton.setBackground(new Color(50, 150, 50));
        startServerButton.setForeground(Color.WHITE);
        startServerButton.setFocusPainted(false);
        startServerButton.addActionListener(e -> startSecondaryServer());
        buttons.add(startServerButton);

        killSecondaryButton = new JButton("Kill Selected");
        killSecondaryButton.setBackground(new Color(220, 50, 50));
        killSecondaryButton.setForeground(Color.WHITE);
        killSecondaryButton.setFocusPainted(false);
        killSecondaryButton.addActionListener(e -> killSelectedSecondary());
        buttons.add(killSecondaryButton);

        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Logs"));
        panel.setPreferredSize(new Dimension(0, 150));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        panel.add(logScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void startUpdateThread() {
        Thread updater = new Thread(() -> {
            while (true) {
                try {
                    SwingUtilities.invokeLater(this::updateDisplay);
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updater.setDaemon(true);
        updater.start();
    }

    private void updateDisplay() {
        int primaryID = monitor.getPrimaryID();
        primaryLabel.setText(primaryID != 0 ? "Server " + primaryID : "None");
        sumLabel.setText(String.valueOf(monitor.getPrimarySum()));
        killPrimaryButton.setEnabled(primaryID != 0);

        // Save current selections
        int selectedSecondaryIndex = secondaryList.getSelectedIndex();
        int selectedClientIndex = clientList.getSelectedIndex();

        // Update secondary servers
        secondaryListModel.clear();
        for (NodeInfo server : monitor.getServers().values()) {
            if (server.nodeID != primaryID) {
                String status = server.isAlive() ? "ALIVE" : "TIMEOUT";
                secondaryListModel.addElement("Server " + server.nodeID + " - " + status);
            }
        }

        // Update clients
        clientListModel.clear();
        for (NodeInfo client : monitor.getClients().values()) {
            String status = client.isAlive() ? "ALIVE" : "TIMEOUT";
            clientListModel.addElement("Client " + client.nodeID + " - " + status);
        }

        // Restore selections if still valid
        if (selectedSecondaryIndex >= 0 && selectedSecondaryIndex < secondaryListModel.size()) {
            secondaryList.setSelectedIndex(selectedSecondaryIndex);
        }

        if (selectedClientIndex >= 0 && selectedClientIndex < clientListModel.size()) {
            clientList.setSelectedIndex(selectedClientIndex);
        }

        // Optional: update logs from monitor if you keep a recent log list
    }

    private void killPrimary() {
        int primaryID = monitor.getPrimaryID();
        if (primaryID == 0) {
            log("No primary server to kill");
            return;
        }
        for (Process p : serverProcesses) {
            if (p.isAlive()) {
                p.destroy();
                log("Primary server killed. Failover should occur.");
                return;
            }
        }
        log("Could not find primary process");
    }

    private void startClient() {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", "bin", "Client", String.valueOf( nextClientID ) );
            nextClientID++;
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            clientProcesses.add(p);
            log("Client started (Client " + clientProcesses.size() + ")");
        } catch (Exception e) {
            log("Error starting client: " + e.getMessage());
        }
    }

    private void killSelectedClient() {
        int index = clientList.getSelectedIndex();
        if (index >= 0 && index < clientProcesses.size()) {
            Process p = clientProcesses.get(index);
            if (p.isAlive()) p.destroy();
            log("Client " + (index + 1) + " killed");
        }
    }

    private void startSecondaryServer() {
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", "bin", "Server", String.valueOf(nextServerID));
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            serverProcesses.add(p);
            log("Secondary server started on port " + nextServerID);
            nextServerID++;
        } catch (Exception e) {
            log("Error starting server: " + e.getMessage());
        }
    }

    private void killSelectedSecondary() {
        int index = secondaryList.getSelectedIndex();
        if (index >= 0 && index < serverProcesses.size()) {
            Process p = serverProcesses.get(index);
            if (p.isAlive()) p.destroy();
            log("Secondary server killed");
        }
    }

    private void log(String message) {
        // String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        // logArea.append("[" + timestamp + "] " + message + "\n");
        // logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void addLogMessage( String message ) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
