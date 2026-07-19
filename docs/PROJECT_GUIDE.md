# Project Guide — Testing, GitHub Strategy, Interview Prep

Companion document to the main `README.md`. This covers everything you need to test the project thoroughly, publish it as convincing proof-of-work on GitHub, and talk about it confidently in interviews.

---

## 1. Testing Strategy

### Manual test matrix

| # | Scenario | Steps | Expected result |
|---|---|---|---|
| 1 | Single client connects | Start server, start 1 client, enter username | Client sees "Connected as X"; server logs the connection |
| 2 | Multiple clients | Start server, start 3 clients with different usernames | All 3 can message each other; each sees the other two's join notifications |
| 3 | Duplicate username | Connect Client A as "Sam", then Client B as "Sam" | Client B receives `NAMEINUSE` and is disconnected with a clear message |
| 4 | Empty message | Client presses Enter with no text typed | Server ignores it (no broadcast, no log entry) — verified in `ClientHandler.handleClientLine` |
| 5 | Sudden client disconnect | Force-close a client's terminal (Ctrl+C) mid-session | Server detects the broken socket, removes the client, broadcasts a "left the chat" notice |
| 6 | Server shutdown while clients connected | Stop the server process | Clients' listener threads catch `IOException` and print "Disconnected from server" |
| 7 | Invalid private-message target | Send `/pm Ghost hello` where "Ghost" isn't connected | Sender receives `User 'Ghost' not found.` |
| 8 | Long message | Send a very long single-line message (500+ chars) | Delivered and logged in full — no manual line-length limit is enforced |
| 9 | Rapid messaging | Send many messages back-to-back quickly from one client | All are broadcast in order without loss (single-threaded read loop per client guarantees per-client ordering) |
| 10 | Port already in use | Start two server instances on the same port | Second instance prints a clear `BindException` message with a suggested fix |

### Automated verification performed during development

An end-to-end simulation was scripted and actually executed (server + 2 concurrent clients, piping timed input) to confirm the real behavior, not just the code reading correctly:

- Both clients connected successfully.
- Broadcast messages appeared on both sides.
- `/users` correctly listed connected usernames.
- `/pm` correctly delivered a private message and echoed it back to the sender.
- Join/leave notifications fired for both clients.
- `logs/chat.log` was created and contained a complete, correctly timestamped transcript of the session.

### Optional JUnit test ideas (for extending the project)

```java
// Example test skeletons — add a test/ folder + JUnit 5 dependency to implement these.

class MessageTest {
    // toString() for CHAT type produces "[HH:mm:ss] sender: content"
    // toString() for JOIN type produces "*** sender joined the chat ***"
}

class ChatLoggerTest {
    // log() appends a line to the target file
    // concurrent calls from multiple threads never interleave/corrupt a line
}

// For ClientHandler/ChatServer, prefer a lightweight integration test:
// - start ChatServer on an ephemeral port in a background thread
// - open 2 real Socket connections from the test
// - assert broadcast messages are received by both sockets
```

---

## 2. GitHub Strategy

### Repository setup
- **Repository name:** `java-chat-app-socket-programming`
- **Description:** "Real-time multi-client chat application in core Java using TCP sockets and multithreading — no frameworks, runs on localhost."
- **Suggested tags/topics:** `java`, `socket-programming`, `multithreading`, `tcp`, `client-server`, `networking`, `swing`, `chat-application`, `concurrency`, `java-project`

### What to commit
- All of `src/`, `README.md`, `docs/`, `.gitignore`
- Curated screenshots in `screenshots/`
- A sample `logs/chat.log` from a demo run (optional, but good proof-of-work) — otherwise keep logs out of git via `.gitignore` and only commit a redacted sample

### What NOT to commit
- Compiled `.class` files / `bin/` or `out/` directories (already excluded via `.gitignore`)
- IDE-specific project files (`.idea/`, `.classpath`, `.project`)
- Any future database credentials, API keys, or `.env` files — if you add DB-backed login later, use environment variables and never commit real passwords

### Commit message style
Use clear, imperative-mood messages tied to real milestones, e.g.:
- `feat: implement ServerSocket and basic connection accept loop`
- `feat: add ClientHandler thread with broadcast support`
- `feat: add username validation and duplicate-name handling`
- `feat: implement private messaging (/pm) and /users command`
- `feat: add persistent chat logging with ChatLogger`
- `feat: add optional Swing GUI client`
- `docs: add README with architecture and run instructions`
- `test: document manual test matrix and simulation results`

---

## 3. Day-Wise Proof-of-Work Plan

| Day | Focus | Files to commit | Suggested commit message | Screenshot/proof to capture |
|---|---|---|---|---|
| 1 | Project setup + basic server | `ChatServer.java` (accept loop only) | `feat: initial ServerSocket setup` | Server console showing "listening" message |
| 2 | Client connection | `ChatClient.java` (connect only) | `feat: basic client socket connection` | Client console showing "Connected" |
| 3 | Message exchange | Add read/write loop | `feat: send and receive text messages` | Two terminal windows exchanging a message |
| 4 | Multithreading + multiple clients | `ClientHandler.java`, thread pool in `ChatServer` | `feat: multithreaded client handling for concurrent clients` | Server log showing 3 simultaneous connections |
| 5 | Usernames + broadcasting | Username handshake, `broadcast()` | `feat: username handling and message broadcasting` | 3-client chat screenshot |
| 6 | Private messaging + logging | `/pm`, `/users`, `ChatLogger.java` | `feat: private messaging and persistent chat logs` | `/pm` output + `logs/chat.log` contents |
| 7 | Testing + docs + GUI | `ChatClientGUI.java`, `README.md`, `docs/PROJECT_GUIDE.md` | `docs: finalize README, test matrix, and GUI client` | GUI screenshot, full README preview |

---

## 4. Screenshots / Proof Checklist

Capture and place these in `/screenshots` before your final push:

- [ ] Project folder structure (IDE sidebar or `tree` command output)
- [ ] Server terminal showing "listening" + at least one accepted connection
- [ ] Client 1 connected and chatting
- [ ] Client 2 connected and chatting
- [ ] Three-client simultaneous chat session
- [ ] A broadcast message visible on all client terminals
- [ ] A `/pm` private message exchange
- [ ] A join notification and a leave notification
- [ ] Contents of `logs/chat.log` after a session
- [ ] The Swing GUI client window (if you run the optional GUI)
- [ ] GitHub repository main page (showing folder structure + README rendering)
- [ ] Rendered README preview on GitHub

---

## 5. Interview Preparation

**Q1: Explain your project.**
> I built a real-time, multi-client chat application in core Java using TCP sockets and multithreading, with no external frameworks. A `ChatServer` listens on a `ServerSocket` and, for every client that connects, spins up a dedicated thread (`ClientHandler`) so multiple users can chat concurrently. Threads share a thread-safe collection of connected clients to broadcast messages, and I also implemented private messaging, join/leave notifications, and persistent chat logging. There's a console client and an optional Swing GUI client using the identical protocol.

**Q2: Why did you use a new thread per client instead of handling clients sequentially?**
> A single-threaded server would block on one client's `read()` call while others wait, making real-time multi-user chat impossible. Handing each accepted socket to its own thread (via a cached thread pool) lets the server service many clients concurrently — while one thread is blocked waiting for that client's next message, other threads keep serving their own clients independently.

**Q3: How do you prevent race conditions when broadcasting messages?**
> The set of connected `ClientHandler`s is wrapped with `Collections.synchronizedSet`, and every iteration over it (during broadcast, `/users`, or disconnect cleanup) happens inside a `synchronized(clients)` block. That guarantees only one thread mutates or iterates the shared collection at a time, preventing `ConcurrentModificationException` and lost updates.

**Q4: What's the difference between `Thread` and `Runnable` in your design, and why did you choose one?**
> `ClientHandler implements Runnable` rather than extending `Thread`, which keeps the class focused on "what to do" rather than "how to run it," and lets me submit it to an `ExecutorService` thread pool instead of manually managing raw `Thread` objects — better resource control and reuse.

**Q5: How does your client receive messages while also allowing the user to type?**
> Each client runs two threads: the main thread blocks on reading user keyboard input (`Scanner`) and sends it to the server, while a background daemon thread simultaneously blocks on reading incoming lines from the server socket and prints them. This way incoming messages appear immediately, without waiting for the user to press Enter.

**Q6: How do you handle a client disconnecting unexpectedly (e.g., closing the terminal)?**
> The server's `readLine()` call returns `null` (or throws `IOException`) when the socket breaks. That's caught in `ClientHandler.run()`'s try/catch, which then calls a `disconnect()` cleanup method — removing the handler from the shared client set, broadcasting a "left the chat" notice, and closing the socket, all wrapped so a bad client can't crash the server.

**Q7: What happens if two clients try to use the same username?**
> When a client sends its username, the server checks the shared client set (inside a synchronized block) for a case-insensitive match. If found, it sends back a `NAMEINUSE` response and closes that connection before adding it to the set — so duplicate usernames are rejected atomically.

**Q8: How would you scale this beyond localhost to a real deployed server?**
> Deploy the `ChatServer` on a machine with a public IP or behind a reverse proxy, open the chosen port, and point clients at that host. For real scale I'd also move to NIO/`Selector`-based non-blocking I/O or a framework like Netty to avoid one-thread-per-connection overhead at high connection counts, add TLS via `SSLSocket`, and back the client list with a horizontally-scalable pub/sub layer (e.g., Redis) if running multiple server instances.

**Q9: Why use plain-text line-based messages instead of Java serialization or JSON?**
> A simple newline-delimited text protocol is easy to read, debug (you can literally `telnet` into the server), and it keeps `BufferedReader`/`PrintWriter` usage straightforward for a learning-focused project. Serialization introduces versioning and security concerns (deserialization vulnerabilities); JSON would be my choice if I extended this into a "real" product needing structured payloads.

**Q10: What was the hardest part of building this, and what would you improve next?**
> Correctly handling concurrent access to shared state (the client list) without deadlocking or corrupting broadcasts was the trickiest part — solved with a synchronized collection and disciplined locking around every access. Next I'd add multiple chat rooms, a database-backed login system, and TLS encryption, and move the server to NIO for better scalability.

---

## 6. Resume / LinkedIn Description

**Resume bullet:**
> Built a multithreaded, real-time chat application in Java using TCP sockets, implementing a thread-per-client server architecture, thread-safe message broadcasting, private messaging, and persistent chat logging; includes a console client and an optional Swing GUI client.

**LinkedIn project description:**
> I built a real-time multi-client chat application from scratch in core Java — no frameworks — to deepen my understanding of socket programming and concurrency. The server accepts connections via `ServerSocket` and spins up a dedicated thread per client to broadcast messages concurrently, with thread-safe shared state, private messaging, join/leave notifications, and persistent chat logs. It includes both a console client and an optional Java Swing GUI client. Full source + docs on GitHub: [link]
