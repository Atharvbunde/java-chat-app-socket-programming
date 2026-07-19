package common;

import static support.SimpleAssert.assertEquals;
import static support.SimpleAssert.assertTrue;

/**
 * MessageTest
 * -----------
 * Unit tests for Message.toString() formatting, run via support.TestRunner.
 */
public class MessageTest {

    public void testChatMessageFormat() {
        Message msg = new Message("Alice", "hello there", Message.MessageType.CHAT);
        String formatted = msg.toString();
        assertTrue("chat message should contain the sender name", formatted.contains("Alice:"));
        assertTrue("chat message should contain the content", formatted.contains("hello there"));
        assertTrue("chat message should start with a bracketed timestamp", formatted.startsWith("["));
    }

    public void testJoinMessageFormat() {
        Message msg = new Message("Bob", "", Message.MessageType.JOIN);
        String formatted = msg.toString();
        assertTrue("join message should mention 'joined the chat'", formatted.contains("joined the chat"));
        assertTrue("join message should mention the username", formatted.contains("Bob"));
    }

    public void testLeaveMessageFormat() {
        Message msg = new Message("Bob", "", Message.MessageType.LEAVE);
        String formatted = msg.toString();
        assertTrue("leave message should mention 'left the chat'", formatted.contains("left the chat"));
    }

    public void testPrivateMessageFormat() {
        Message msg = new Message("Alice", "secret", Message.MessageType.PRIVATE);
        String formatted = msg.toString();
        assertTrue("private message should be marked (private)", formatted.contains("(private)"));
        assertTrue("private message should contain content", formatted.contains("secret"));
    }

    public void testSystemMessageFormat() {
        Message msg = new Message("SERVER", "user list here", Message.MessageType.SYSTEM);
        String formatted = msg.toString();
        assertEquals("system messages should be tagged as [SERVER]", true, formatted.contains("[SERVER]"));
    }

    public void testGettersReturnConstructorValues() {
        Message msg = new Message("Carol", "hi", Message.MessageType.CHAT);
        assertEquals("getSender should return constructor value", "Carol", msg.getSender());
        assertEquals("getContent should return constructor value", "hi", msg.getContent());
        assertEquals("getType should return constructor value", Message.MessageType.CHAT, msg.getType());
        assertTrue("getTimestamp should not be null/empty", msg.getTimestamp() != null && !msg.getTimestamp().isEmpty());
    }
}
