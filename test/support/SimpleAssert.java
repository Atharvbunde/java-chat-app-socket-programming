package support;

/**
 * SimpleAssert
 * ------------
 * Tiny, dependency-free assertion helpers so the test suite can run with
 * nothing but the JDK — no JUnit jar to download, which matters because
 * this project (and its CI) intentionally has zero external dependencies.
 *
 * Each method throws AssertionError on failure, exactly like JUnit's
 * Assertions class, so TestRunner can catch AssertionError uniformly.
 */
public final class SimpleAssert {

    private SimpleAssert() {
    }

    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertEquals(String message, Object expected, Object actual) {
        boolean equal = (expected == null) ? actual == null : expected.equals(actual);
        if (!equal) {
            throw new AssertionError(message + " -- expected: <" + expected + "> but was: <" + actual + ">");
        }
    }

    public static void assertNotNull(String message, Object obj) {
        if (obj == null) {
            throw new AssertionError(message);
        }
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }
}
