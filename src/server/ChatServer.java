package server;

import common.ChatLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ChatServer
 * ----------
 * Entry point for the server side of the application.
 *
 * Responsibilities:
 *   1. Open a ServerSocket on the configured port.
 *   2. Loop forever, accepting incoming client connections.
 *   3. For every accepted connection, hand it off to a ClientHandler
 *      running on its own thread (via a cached thread pool) so the
 *      server can serve many clients at once without blocking.
 *
 * Run with:  java server.ChatServer [port]
 * Default port: 5000
 */
public class ChatServer {

    public static final int DEFAULT_PORT = 5000;

    // Thread-safe set shared by every ClientHandler for broadcasting.
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private final ChatLogger logger = new ChatLogger("logs/chat.log");
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port argument, using default port " + DEFAULT_PORT);
            }
        }
        new ChatServer().start(port);
    }

    public void start(int port) {
        System.out.println("==============================================");
        System.out.println(" Java Chat Server starting on port " + port);
        System.out.println("==============================================");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening. Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket, clients, logger);
                pool.execute(handler);
            }

        } catch (IOException e) {
            if (e instanceof java.net.BindException) {
                System.err.println("ERROR: Port " + port + " is already in use.");
                System.err.println("Fix: stop the process using it, or run with a different port:");
                System.err.println("     java server.ChatServer 5001");
            } else {
                System.err.println("Server error: " + e.getMessage());
            }
        } finally {
            pool.shutdown();
        }
    }
}
