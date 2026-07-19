package common;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Message
 * -------
 * Plain data object representing a single chat message.
 * Used only to keep formatting consistent between the server and any
 * client (console or GUI) that wants to render a message.
 *
 * The wire protocol itself is plain-text lines (see ClientHandler),
 * this class is a convenience wrapper for building/parsing those lines
 * in a single place instead of scattering String concatenation everywhere.
 */
public class Message implements Serializable {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String sender;
    private final String content;
    private final String timestamp;
    private final MessageType type;

    public enum MessageType {
        CHAT,       // normal broadcast message
        PRIVATE,    // private/whisper message
        JOIN,       // user joined notification
        LEAVE,      // user left notification
        SYSTEM      // server/system notice (errors, user list, etc.)
    }

    public Message(String sender, String content, MessageType type) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.timestamp = LocalTime.now().format(TIME_FORMAT);
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public MessageType getType() {
        return type;
    }

    /**
     * Formats the message for display on a console or GUI text area.
     * Example: "[14:02:31] Alice: hey everyone"
     */
    @Override
    public String toString() {
        switch (type) {
            case JOIN:
                return "[" + timestamp + "] *** " + sender + " joined the chat ***";
            case LEAVE:
                return "[" + timestamp + "] *** " + sender + " left the chat ***";
            case PRIVATE:
                return "[" + timestamp + "] (private) " + sender + ": " + content;
            case SYSTEM:
                return "[" + timestamp + "] [SERVER] " + content;
            case CHAT:
            default:
                return "[" + timestamp + "] " + sender + ": " + content;
        }
    }
}
