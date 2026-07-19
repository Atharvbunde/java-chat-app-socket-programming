package integration;

import server.ChatServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static support.SimpleAssert.assertEquals;
import static support.SimpleAssert.assertTrue;

/**
 * ChatServerIntegrationTest
 * -------------------------
 * Full end-to-end test: starts a REAL ChatServer on an ephemeral port in
 * a background thread, opens REAL Socket connections to it (just like
 * ChatClient does), and verifies actual broadcast / private-message /
 * duplicate-username behavior over the network stack — not mocks.
 */
public class ChatServerIntegrationTest {

    /** Finds a free TCP port so tests never collide with a real running server. */
    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public void testBroadcastBetweenTwoRealClients() throws Exception {
        int port = findFreePort();
        Thread serverThread = new Thread(() -> new ChatServer().start(port));
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(500); // give the server a moment to bind and start listening

        try (Socket alice = new Socket("localhost", port);
             Socket bob = new Socket("localhost", port)) {

            BufferedReader aliceIn = new BufferedReader(new InputStreamReader(alice.getInputStream()));
            PrintWriter aliceOut = new PrintWriter(alice.getOutputStream(), true);
            BufferedReader bobIn = new BufferedReader(new InputStreamReader(bob.getInputStream()));
            PrintWriter bobOut = new PrintWriter(bob.getOutputStream(), true);

            // Handshake: both clients submit usernames
            assertEquals("server should prompt for a username", "SUBMITNAME", aliceIn.readLine());
            aliceOut.println("Alice");
            assertEquals("server should accept Alice's username", "NAMEACCEPTED Alice", aliceIn.readLine());
            assertTrue("Alice should see her own join notice", aliceIn.readLine().contains("Alice joined"));

            assertEquals("server should prompt Bob for a username", "SUBMITNAME", bobIn.readLine());
            bobOut.println("Bob");
            assertEquals("server should accept Bob's username", "NAMEACCEPTED Bob", bobIn.readLine());
            assertTrue("Bob should see his own join notice", bobIn.readLine().contains("Bob joined"));
            assertTrue("Alice should also see Bob's join notice", aliceIn.readLine().contains("Bob joined"));

            // Alice sends a broadcast message; Bob should receive it
            aliceOut.println("hello from alice");
            String aliceEcho = aliceIn.readLine();
            String bobReceived = bobIn.readLine();
            assertTrue("Alice's own console should show her message", aliceEcho.contains("hello from alice"));
            assertTrue("Bob should receive Alice's broadcast message", bobReceived.contains("hello from alice"));
            assertTrue("broadcast message should be attributed to Alice", bobReceived.contains("Alice:"));

        } finally {
            serverThread.interrupt();
        }
    }

    public void testDuplicateUsernameIsRejected() throws Exception {
        int port = findFreePort();
        Thread serverThread = new Thread(() -> new ChatServer().start(port));
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(500);

        try (Socket first = new Socket("localhost", port);
             Socket second = new Socket("localhost", port)) {

            BufferedReader firstIn = new BufferedReader(new InputStreamReader(first.getInputStream()));
            PrintWriter firstOut = new PrintWriter(first.getOutputStream(), true);
            BufferedReader secondIn = new BufferedReader(new InputStreamReader(second.getInputStream()));
            PrintWriter secondOut = new PrintWriter(second.getOutputStream(), true);

            firstIn.readLine(); // SUBMITNAME
            firstOut.println("Sam");
            assertEquals("first client should be accepted as Sam", "NAMEACCEPTED Sam", firstIn.readLine());

            secondIn.readLine(); // SUBMITNAME
            secondOut.println("Sam");
            assertEquals("second client using the same name should be rejected", "NAMEINUSE", secondIn.readLine());

        } finally {
            serverThread.interrupt();
        }
    }

    public void testPrivateMessageIsDeliveredOnlyToTarget() throws Exception {
        int port = findFreePort();
        Thread serverThread = new Thread(() -> new ChatServer().start(port));
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(500);

        try (Socket alice = new Socket("localhost", port);
             Socket bob = new Socket("localhost", port)) {

            BufferedReader aliceIn = new BufferedReader(new InputStreamReader(alice.getInputStream()));
            PrintWriter aliceOut = new PrintWriter(alice.getOutputStream(), true);
            BufferedReader bobIn = new BufferedReader(new InputStreamReader(bob.getInputStream()));
            PrintWriter bobOut = new PrintWriter(bob.getOutputStream(), true);

            aliceIn.readLine();
            aliceOut.println("Alice");
            aliceIn.readLine(); // NAMEACCEPTED
            aliceIn.readLine(); // Alice's own join notice

            bobIn.readLine();
            bobOut.println("Bob");
            bobIn.readLine(); // NAMEACCEPTED
            bobIn.readLine(); // Bob's own join notice
            aliceIn.readLine(); // Alice sees Bob's join notice

            aliceOut.println("/pm Bob this is a secret");
            String bobPrivate = bobIn.readLine();
            String aliceEcho = aliceIn.readLine();

            assertTrue("Bob should receive the private message", bobPrivate.contains("this is a secret"));
            assertTrue("private message should be marked as private", bobPrivate.contains("(private)"));
            assertTrue("sender should get an echo of the private message", aliceEcho.contains("this is a secret"));

        } finally {
            serverThread.interrupt();
        }
    }
}
