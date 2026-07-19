package server;

import common.ChatLogger;
import common.Message;
import common.Message.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

/**
 * ClientHandler
 * -------------
 * Runs on its own thread — one instance per connected socket.
 * Responsible for:
 *   1. Reading the username the client sends when it connects.
 *   2. Reading further lines from that client and turning them into
 *      broadcast / private / command actions.
 *   3. Cleaning up (removing itself from the shared client set,
 *      closing the socket, notifying everyone else) when the client
 *      disconnects or an I/O error occurs.
 *
 * Supported client commands (typed by the user in the client console):
 *   /users            -> list currently connected usernames
 *   /pm <user> <msg>  -> send a private message to <user>
 *   /quit             -> disconnect gracefully
 *
 * Concurrency:
 *   The set of active handlers (`clients`) is a thread-safe collection
 *   (Collections.synchronizedSet / ConcurrentHashMap-backed set) owned
 *   by ChatServer and shared by every ClientHandler thread, which is
 *   why all iteration over it is done inside a `synchronized` block.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Set<ClientHandler> clients;
    private final ChatLogger logger;

    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, Set<ClientHandler> clients, ChatLogger logger) {
        this.socket = socket;
        this.clients = clients;
        this.logger = logger;
    }

    public String getUsername() {
        return username;
    }

    /** Sends a raw line to just this client. Used for broadcasts and system replies. */
    public void send(String line) {
        if (out != null) {
            out.println(line);
        }
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // --- Step 1: read and validate the username ---
            out.println("SUBMITNAME");
            username = in.readLine();
            if (username == null || username.trim().isEmpty()) {
                socket.close();
                return;
            }
            username = username.trim();

            synchronized (clients) {
                boolean taken = clients.stream()
                        .anyMatch(c -> c.username != null && c.username.equalsIgnoreCase(username));
                if (taken) {
                    out.println("NAMEINUSE");
                    socket.close();
                    return;
                }
                clients.add(this);
            }

            out.println("NAMEACCEPTED " + username);

            // --- Step 2: announce the join to everyone ---
            Message joinMsg = new Message(username, "", MessageType.JOIN);
            broadcast(joinMsg.toString());
            logger.log(joinMsg.toString());

            // --- Step 3: main read loop ---
            String line;
            while (running && (line = in.readLine()) != null) {
                handleClientLine(line);
            }

        } catch (IOException e) {
            System.out.println("Connection lost for " + (username != null ? username : "unknown client")
                    + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /** Parses one line of input from the client and routes it appropriately. */
    private void handleClientLine(String line) {
        if (line.isEmpty()) {
            return;
        }

        if (line.equalsIgnoreCase("/quit")) {
            running = false;
            return;
        }

        if (line.equalsIgnoreCase("/users")) {
            StringBuilder sb = new StringBuilder("Connected users: ");
            synchronized (clients) {
                clients.forEach(c -> sb.append(c.username).append(", "));
            }
            String reply = new Message("SERVER", sb.toString(), MessageType.SYSTEM).toString();
            send(reply);
            return;
        }

        if (line.startsWith("/pm ")) {
            // Format: /pm targetUser message text here
            String rest = line.substring(4).trim();
            int spaceIdx = rest.indexOf(' ');
            if (spaceIdx == -1) {
                send(new Message("SERVER", "Usage: /pm <username> <message>", MessageType.SYSTEM).toString());
                return;
            }
            String targetName = rest.substring(0, spaceIdx);
            String content = rest.substring(spaceIdx + 1);
            sendPrivateMessage(targetName, content);
            return;
        }

        // Default: normal broadcast chat message
        Message chatMsg = new Message(username, line, MessageType.CHAT);
        broadcast(chatMsg.toString());
        logger.log(chatMsg.toString());
    }

    private void sendPrivateMessage(String targetName, String content) {
        ClientHandler target = null;
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if (c.username != null && c.username.equalsIgnoreCase(targetName)) {
                    target = c;
                    break;
                }
            }
        }
        Message pm = new Message(username, content, MessageType.PRIVATE);
        if (target == null) {
            send(new Message("SERVER", "User '" + targetName + "' not found.", MessageType.SYSTEM).toString());
            return;
        }
        target.send(pm.toString());
        send(pm.toString()); // echo back to sender so they see what was sent
        logger.log("(private -> " + targetName + ") " + pm);
    }

    /** Sends a line to every currently connected client. */
    private void broadcast(String line) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.send(line);
            }
        }
    }

    /** Cleans up state when a client disconnects (normally or due to an error). */
    private void disconnect() {
        synchronized (clients) {
            clients.remove(this);
        }
        if (username != null) {
            Message leaveMsg = new Message(username, "", MessageType.LEAVE);
            broadcast(leaveMsg.toString());
            logger.log(leaveMsg.toString());
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // Socket already closed; nothing further to do.
        }
    }
}
