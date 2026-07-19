package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static support.SimpleAssert.assertEquals;
import static support.SimpleAssert.assertTrue;

/**
 * ChatLoggerTest
 * --------------
 * Verifies that ChatLogger writes lines to disk and, importantly, that
 * concurrent writes from multiple threads never interleave or corrupt a
 * line — this is the exact scenario that happens for real when several
 * ClientHandler threads log messages at the same time.
 */
public class ChatLoggerTest {

    public void testSingleLineIsWritten() throws IOException {
        String path = "test/tmp/single_line_test.log";
        new File(path).getParentFile().mkdirs();
        new File(path).delete();

        ChatLogger logger = new ChatLogger(path);
        logger.log("Hello from test");

        List<String> lines = readLines(path);
        assertEquals("exactly one line should be written", 1, lines.size());
        assertTrue("the written line should contain our message", lines.get(0).contains("Hello from test"));

        new File(path).delete();
    }

    public void testConcurrentWritesAreNotCorrupted() throws Exception {
        String path = "test/tmp/concurrent_test.log";
        new File(path).getParentFile().mkdirs();
        new File(path).delete();

        ChatLogger logger = new ChatLogger(path);

        int threadCount = 20;
        int messagesPerThread = 25;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < messagesPerThread; i++) {
                        logger.log("thread-" + threadId + " message-" + i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(30, TimeUnit.SECONDS);
        pool.shutdown();
        assertTrue("all logging threads should finish within timeout", finished);

        List<String> lines = readLines(path);
        assertEquals("total line count should equal threads * messages (no lost/merged lines)",
                threadCount * messagesPerThread, lines.size());

        // Every line must be well-formed (starts with a bracketed timestamp) —
        // if writes interleaved, some lines would be malformed/merged.
        for (String line : lines) {
            assertTrue("every logged line should start with a timestamp bracket: " + line, line.startsWith("["));
        }

        new File(path).delete();
    }

    private List<String> readLines(String path) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
