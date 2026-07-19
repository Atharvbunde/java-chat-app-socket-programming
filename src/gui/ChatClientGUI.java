package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * ChatClientGUI
 * -------------
 * Optional Java Swing front-end for the chat client. Talks to the same
 * ChatServer / ClientHandler protocol as the console client — only the
 * presentation layer differs.
 *
 * Layout:
 *   - Top: connection status label
 *   - Center: scrollable, read-only text area showing the conversation
 *   - Bottom: text field + Send button for composing messages
 *
 * Threading:
 *   Swing components must only be touched on the Event Dispatch Thread
 *   (EDT). Incoming socket messages arrive on a background listener
 *   thread, so every UI update from that thread is wrapped in
 *   SwingUtilities.invokeLater(...).
 *
 * Run with: java gui.ChatClientGUI [host] [port]
 */
public class ChatClientGUI extends JFrame {

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");
    private final JLabel statusLabel = new JLabel("Not connected");

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ChatClientGUI(String host, int port) {
        super("Java Chat App - Socket Programming");
        buildUI();
        connect(host, port);
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(560, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        JPanel bottomPanel = new JPanel(new BorderLayout(6, 6));
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(statusLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(this::onSend);
        inputField.addActionListener(this::onSend); // Enter key also sends
    }

    private void connect(String host, int port) {
        username = JOptionPane.showInputDialog(this, "Enter your username:", "Login", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            System.exit(0);
        }
        username = username.trim();

        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String prompt = in.readLine(); // "SUBMITNAME"
            if (!"SUBMITNAME".equals(prompt)) {
                showErrorAndExit("Unexpected handshake response from server.");
                return;
            }
            out.println(username);

            String response = in.readLine();
            if (response == null || response.equals("NAMEINUSE")) {
                showErrorAndExit("Username '" + username + "' is already taken. Restart and pick another.");
                return;
            }

            statusLabel.setText("Connected as " + username + "  |  Server: " + host + ":" + port);
            appendLine("Connected. Type a message and press Enter or click Send.");
            appendLine("Commands: /users to list users, /pm <user> <message> for a private message, /quit to exit.");

            startListenerThread();

        } catch (IOException e) {
            showErrorAndExit("Could not connect to " + host + ":" + port + " -> " + e.getMessage());
        }
    }

    /** Background thread reading incoming messages from the server socket. */
    private void startListenerThread() {
        Thread listener = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String finalLine = line;
                    SwingUtilities.invokeLater(() -> appendLine(finalLine));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> appendLine("Disconnected from server."));
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    private void onSend(ActionEvent e) {
        String text = inputField.getText().trim();
        if (text.isEmpty() || out == null) {
            return;
        }
        out.println(text);
        inputField.setText("");
        if (text.equalsIgnoreCase("/quit")) {
            closeConnection();
        }
    }

    private void appendLine(String line) {
        chatArea.append(line + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    private void showErrorAndExit(String message) {
        JOptionPane.showMessageDialog(this, message, "Connection Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        SwingUtilities.invokeLater(() -> {
            ChatClientGUI gui = new ChatClientGUI(host, port);
            gui.setVisible(true);
        });
    }
}
