package common;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ChatLogger
 * ----------
 * Appends every broadcast/private/system message to a log file on disk
 * (logs/chat.log by default) so the conversation history survives a
 * server restart and can be uploaded to GitHub as proof of work.
 *
 * Thread-safety:
 *   Multiple ClientHandler threads call log() concurrently, so the
 *   method is synchronized to prevent interleaved / corrupted lines.
 */
public class ChatLogger {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String logFilePath;

    public ChatLogger(String logFilePath) {
        this.logFilePath = logFilePath;
        // Make sure the parent "logs" directory exists.
        java.io.File file = new java.io.File(logFilePath);
        java.io.File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Appends a single line to the log file. Synchronized so concurrent
     * client-handler threads never interleave their writes.
     */
    public synchronized void log(String line) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            String stamped = "[" + LocalDateTime.now().format(DATE_TIME_FORMAT) + "] " + line;
            writer.println(stamped);
        } catch (IOException e) {
            // Logging failures should never crash the chat server.
            System.err.println("ChatLogger: could not write to " + logFilePath + " -> " + e.getMessage());
        }
    }
}
