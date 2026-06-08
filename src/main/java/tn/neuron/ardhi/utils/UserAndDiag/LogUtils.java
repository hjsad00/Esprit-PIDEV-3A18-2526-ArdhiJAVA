package tn.neuron.ardhi.utils.UserAndDiag;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogUtils {

    static {
        // Optional: Configure custom handlers or formatters here if needed
        // For now, default console handler is sufficient
    }

    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }

    public static void info(Class<?> clazz, String message) {
        getLogger(clazz).info(message);
    }

    public static void warn(Class<?> clazz, String message) {
        getLogger(clazz).warning(message);
    }

    public static void error(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).log(Level.SEVERE, message, t);
    }

    public static void error(Class<?> clazz, String message) {
        getLogger(clazz).severe(message);
    }
}