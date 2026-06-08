package tn.neuron.ardhi.utils.UserAndDiag;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized application configuration loader.
 * Reads all secrets and settings from {@code config.properties} on the classpath.
 *
 * <p>Usage:
 * <pre>
 *   String key = AppConfig.get("groq.api.key");
 *   String db  = AppConfig.get("db.url", "jdbc:mysql://localhost:3306/ardhi");
 * </pre>
 *
 * <p>To set up locally, copy {@code config.properties.example} to
 * {@code config.properties} and fill in your real values.
 */
public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream is = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                props.load(is);
                LogUtils.info(AppConfig.class, "✅ config.properties loaded (" + props.size() + " keys)");
            } else {
                System.err.println("⚠️  config.properties not found on classpath!");
                System.err.println("    → Copy src/main/resources/config.properties.example");
                System.err.println("      to  src/main/resources/config.properties");
                System.err.println("      and fill in your real values.");
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to load config.properties: " + e.getMessage());
        }
    }

    /**
     * Returns the value for the given key, or an empty string if not found.
     */
    public static String get(String key) {
        return props.getProperty(key, "");
    }

    /**
     * Returns the value for the given key, or {@code defaultValue} if not found.
     */
    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Returns the integer value for the given key, or {@code defaultValue} if not found or unparseable.
     */
    public static int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null || val.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Prevent instantiation
    private AppConfig() {}
}
