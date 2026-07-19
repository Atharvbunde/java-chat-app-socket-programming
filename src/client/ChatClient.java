package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * ChatClient
 * ----------
 * Console client for the chat application.
 *
 * Uses two threads:
 *   - The main thread reads from System.in and sends lines to the server.
 *   - A background "listener" thread continuously reads incoming lines
 *     from the server socket and prints them, so the user can receive
 *     messages from other clients at any time without blocking on their
 *     own input.
 *
 * Run with: java client.ChatClient [host] [port]
 * Defaults: host=localhost, port=5000
 */
public class ChatClient {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;

        try (Socket socket = new Socket(host, port)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner console = new Scanner(System.in);

            // --- Handshake: server asks for a username ---
            String serverPrompt = in.readLine(); // expects "SUBMITNAME"
            if (!"SUBMITNAME".equals(serverPrompt)) {
                System.out.println("Unexpected server response, disconnecting.");
                return;
            }

            System.out.print("Enter your username: ");
            String username = console.nextLine().trim();
            out.println(username);

            String response = in.readLine();
            if (response == null) {
                System.out.println("Server closed the connection.");
                return;
            }
            if (response.equals("NAMEINUSE")) {
                System.out.println("That username is already taken. Please restart the client and try another.");
                return;
            }
            System.out.println("Connected as " + username + ". Type /quit to exit, /users to list connected users,");
            System.out.println("or /pm <username> <message> to send a private message.");
            System.out.println("----------------------------------------------------------");

            // --- Background thread: continuously listen for server messages ---
            Thread listener = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            listener.setDaemon(true);
            listener.start();

            // --- Main thread: read user input and send to server ---
            String userLine;
            while (console.hasNextLine()) {
                userLine = console.nextLine();
                out.println(userLine);
                if (userLine.equalsIgnoreCase("/quit")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Could not connect to server at " + host + ":" + port + " -> " + e.getMessage());
            System.err.println("Make sure ChatServer is running first.");
        }

        System.out.println("Client closed.");
    }
}
