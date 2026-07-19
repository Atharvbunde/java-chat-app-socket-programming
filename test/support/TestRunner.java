package support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * TestRunner
 * ----------
 * Minimal reflection-based test runner: for each given class, invokes
 * every public no-arg method whose name starts with "test", reports
 * PASS/FAIL per method, and exits with a non-zero status code if any
 * test failed — so it can gate a CI pipeline exactly like `mvn test`
 * would, without needing Maven or a JUnit jar.
 *
 * Usage: java -cp bin:testbin support.TestRunner
 * (class list is hard-coded in main() below — add new test classes there)
 */
public class TestRunner {

    public static void main(String[] args) throws Exception {
        List<Class<?>> testClasses = new ArrayList<>();
        testClasses.add(Class.forName("common.MessageTest"));
        testClasses.add(Class.forName("common.ChatLoggerTest"));
        testClasses.add(Class.forName("integration.ChatServerIntegrationTest"));

        int total = 0;
        int failed = 0;

        for (Class<?> clazz : testClasses) {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            for (Method method : clazz.getMethods()) {
                if (method.getName().startsWith("test") && method.getParameterCount() == 0) {
                    total++;
                    try {
                        method.invoke(instance);
                        System.out.println("PASS  " + clazz.getSimpleName() + "." + method.getName());
                    } catch (Exception e) {
                        failed++;
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        System.out.println("FAIL  " + clazz.getSimpleName() + "." + method.getName()
                                + " -> " + cause.getMessage());
                    }
                }
            }
        }

        System.out.println("----------------------------------------");
        System.out.println("Total: " + total + "  Failed: " + failed + "  Passed: " + (total - failed));

        if (failed > 0) {
            System.exit(1);
        }
    }
}
