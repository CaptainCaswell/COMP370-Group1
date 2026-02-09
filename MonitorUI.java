import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
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
    private JButton startSlowClientButton;
    private JButton killClientButton;

    // Secondary server list
    private DefaultListModel<String> secondaryListModel;
    private JList<String> secondaryList;
    private JButton startServerButton;
    private JButton startSlowServerButton;
    private JButton killSecondaryButton;

    // Track running processes by their ID
    private Map<Integer, Process> clientProcesses;
    private Map<Integer, Process> serverProcesses;

    // Log panel
    private JTextArea logArea;
    private JScrollPane logScrollPane;

    // Auto-increment port for new nodes
    private int nextServerID = 2000;
    private int nextClientID = 1;

    public MonitorUI(Monitor monitor) {
        this.monitor = monitor;
        this.clientProcesses = new HashMap<>();
        this.serverProcesses = new HashMap<>();

        initializeUI();
        startUpdateThread();
    }

    private void initializeUI() {
        setTitle("Server Redundancy Management System - Monitor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
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

        // Bottom panel with scenarios and logs
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(createScenarioPanel(), BorderLayout.NORTH);
        bottomPanel.add(createLogPanel(), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null); // Center on screen
        log("Monitor UI started");
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(5, 10, 5, 10)
        ));

        JLabel primaryTitle = new JLabel("Primary Server:");
        primaryTitle.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(primaryTitle);

        primaryLabel = new JLabel("None");
        primaryLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(primaryLabel);

        panel.add(Box.createHorizontalStrut(20));

        JLabel sumTitle = new JLabel("Current Sum:");
        sumTitle.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(sumTitle);

        sumLabel = new JLabel("0");
        sumLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(sumLabel);

        panel.add(Box.createHorizontalStrut(20));

        killPrimaryButton = new JButton("Kill Primary");
        killPrimaryButton.setBackground(new Color(220, 53, 69));
        killPrimaryButton.setForeground(Color.WHITE);
        killPrimaryButton.setFocusPainted(false);
        killPrimaryButton.addActionListener(e -> killPrimary());
        panel.add(killPrimaryButton);

        return panel;
    }

    private JPanel createClientPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        TitledBorder border = new TitledBorder("Clients");
        border.setTitleFont(new Font("Arial", Font.BOLD, 13));
        panel.setBorder(border);

        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(clientList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        
        startClientButton = new JButton("Start Client");
        styleButton(startClientButton, new Color(40, 167, 69));
        startClientButton.addActionListener(e -> startClient(0));
        buttons.add(startClientButton);

        startSlowClientButton = new JButton("Start Slow Client");
        styleButton(startSlowClientButton, new Color(255, 193, 7));
        startSlowClientButton.setForeground(Color.BLACK);
        startSlowClientButton.addActionListener(e -> startClient(500));
        buttons.add(startSlowClientButton);

        killClientButton = new JButton("Kill Selected");
        styleButton(killClientButton, new Color(220, 53, 69));
        killClientButton.addActionListener(e -> killSelectedClient());
        buttons.add(killClientButton);

        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSecondaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        TitledBorder border = new TitledBorder("Secondary Servers");
        border.setTitleFont(new Font("Arial", Font.BOLD, 13));
        panel.setBorder(border);

        secondaryListModel = new DefaultListModel<>();
        secondaryList = new JList<>(secondaryListModel);
        secondaryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        secondaryList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(secondaryList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());
        
        startServerButton = new JButton("Start Server");
        styleButton(startServerButton, new Color(40, 167, 69));
        startServerButton.addActionListener(e -> startSecondaryServer(0));
        buttons.add(startServerButton);

        startSlowServerButton = new JButton("Start Slow Server");
        styleButton(startSlowServerButton, new Color(255, 193, 7));
        startSlowServerButton.setForeground(Color.BLACK);
        startSlowServerButton.addActionListener(e -> startSecondaryServer(500));
        buttons.add(startSlowServerButton);

        killSecondaryButton = new JButton("Kill Selected");
        styleButton(killSecondaryButton, new Color(220, 53, 69));
        killSecondaryButton.addActionListener(e -> killSelectedSecondary());
        buttons.add(killSecondaryButton);

        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createScenarioPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        TitledBorder border = new TitledBorder("Test Scenarios");
        border.setTitleFont(new Font("Arial", Font.BOLD, 13));
        panel.setBorder(border);
        panel.setBackground(new Color(248, 249, 250));

        // Placeholder buttons for 6 scenarios
        for (int i = 1; i <= 6; i++) {
            JButton scenarioButton = new JButton("Scenario " + i);
            styleButton(scenarioButton, new Color(0, 123, 255));
            final int scenarioNum = i;
            scenarioButton.addActionListener(e -> runScenario(scenarioNum));
            panel.add(scenarioButton);
        }

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        TitledBorder border = new TitledBorder("Logs");
        border.setTitleFont(new Font("Arial", Font.BOLD, 13));
        panel.setBorder(border);
        panel.setPreferredSize(new Dimension(0, 200));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBackground(new Color(250, 250, 250));
        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        panel.add(logScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.PLAIN, 12));
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
        String selectedSecondary = secondaryList.getSelectedValue();
        String selectedClient = clientList.getSelectedValue();

        // Update secondary servers
        secondaryListModel.clear();
        for (NodeInfo server : monitor.getServers().values()) {
            if (server.nodeID != primaryID) {
                String status = server.isAlive() ? "ALIVE" : "TIMEOUT";
                String item = "Server " + server.nodeID + " - " + status;
                secondaryListModel.addElement(item);
            }
        }

        // Update clients
        clientListModel.clear();
        for (NodeInfo client : monitor.getClients().values()) {
            String status = client.isAlive() ? "ALIVE" : "TIMEOUT";
            String item = "Client " + client.nodeID + " - " + status;
            clientListModel.addElement(item);
        }

        // Restore selections if still valid
        if (selectedSecondary != null) {
            for (int i = 0; i < secondaryListModel.size(); i++) {
                if (secondaryListModel.get(i).equals(selectedSecondary)) {
                    secondaryList.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (selectedClient != null) {
            for (int i = 0; i < clientListModel.size(); i++) {
                if (clientListModel.get(i).equals(selectedClient)) {
                    clientList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void killPrimary() {
        int primaryID = monitor.getPrimaryID();
        if (primaryID == 0) {
            log("No primary server to kill");
            return;
        }
        
        Process p = serverProcesses.get(primaryID);
        if (p != null && p.isAlive()) {
            p.destroy();
            log("Primary server " + primaryID + " killed. Failover should occur.");
        } else {
            log("Could not find primary process for server " + primaryID);
        }
    }

    private void startClient(long delay) {
        try {
            int clientID = nextClientID++;
            ProcessBuilder pb;
            
            if (delay > 0) {
                pb = new ProcessBuilder("java", "-cp", "bin", "Client", 
                    String.valueOf(clientID), String.valueOf(delay));
            } else {
                pb = new ProcessBuilder("java", "-cp", "bin", "Client", 
                    String.valueOf(clientID));
            }
            
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            clientProcesses.put(clientID, p);
            
            String delayInfo = delay > 0 ? " (slow, delay=" + delay + "ms)" : "";
            log("Client " + clientID + " started" + delayInfo);
        } catch (Exception e) {
            log("Error starting client: " + e.getMessage());
        }
    }

    private void killSelectedClient() {
        String selected = clientList.getSelectedValue();
        if (selected == null) {
            log("No client selected");
            return;
        }
        
        // Parse client ID from "Client X - STATUS" format
        String[] parts = selected.split(" ");
        if (parts.length >= 2) {
            try {
                int clientID = Integer.parseInt(parts[1]);
                Process p = clientProcesses.get(clientID);
                if (p != null && p.isAlive()) {
                    p.destroy();
                    log("Client " + clientID + " killed");
                } else {
                    log("Client " + clientID + " process not found or already terminated");
                }
            } catch (NumberFormatException e) {
                log("Error parsing client ID from selection");
            }
        }
    }

    private void startSecondaryServer(long delay) {
        try {
            int serverID = nextServerID++;
            ProcessBuilder pb;
            
            if (delay > 0) {
                pb = new ProcessBuilder("java", "-cp", "bin", "Server", 
                    String.valueOf(serverID), String.valueOf(delay));
            } else {
                pb = new ProcessBuilder("java", "-cp", "bin", "Server", 
                    String.valueOf(serverID));
            }
            
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            serverProcesses.put(serverID, p);
            
            String delayInfo = delay > 0 ? " (slow, delay=" + delay + "ms)" : "";
            log("Server " + serverID + " started" + delayInfo);
        } catch (Exception e) {
            log("Error starting server: " + e.getMessage());
        }
    }

    private void killSelectedSecondary() {
        String selected = secondaryList.getSelectedValue();
        if (selected == null) {
            log("No server selected");
            return;
        }
        
        // Parse server ID from "Server X - STATUS" format
        String[] parts = selected.split(" ");
        if (parts.length >= 2) {
            try {
                int serverID = Integer.parseInt(parts[1]);
                Process p = serverProcesses.get(serverID);
                if (p != null && p.isAlive()) {
                    p.destroy();
                    log("Server " + serverID + " killed");
                } else {
                    log("Server " + serverID + " process not found or already terminated");
                }
            } catch (NumberFormatException e) {
                log("Error parsing server ID from selection");
            }
        }
    }

    private void runScenario(int scenarioNum) {
        // Placeholder for scenario execution
        log("Running Scenario " + scenarioNum + " - Not yet implemented");
        
        // You'll implement these scenarios later
        // Example structure:
        // switch (scenarioNum) {
        //     case 1:
        //         // Scenario 1 logic
        //         break;
        //     case 2:
        //         // Scenario 2 logic
        //         break;
        //     // etc.
        // }
    }

    private void log(String message) {
        // This method is for internal UI logs
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void addLogMessage(String message) {
        // This method is called by the Logger class for monitor events
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}