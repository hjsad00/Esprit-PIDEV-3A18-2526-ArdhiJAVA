package tn.neuron.ardhi.utils.UserAndDiag;

import java.security.SecureRandom;

/**
 * Password generation and strength evaluation utilities.
 */
public class PasswordUtils {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SYMBOLS;
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum PasswordStrength {
        WEAK("Faible", "#e74c3c", 0.25),
        FAIR("Moyen", "#e67e22", 0.50),
        GOOD("Bon", "#f1c40f", 0.75),
        STRONG("Très Fort", "#27ae60", 1.0);

        private final String label;
        private final String color;
        private final double progress;

        PasswordStrength(String label, String color, double progress) {
            this.label = label;
            this.color = color;
            this.progress = progress;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }

        public double getProgress() {
            return progress;
        }
    }

    private PasswordUtils() {
    }

    /**
     * Generates a random strong password of the given length.
     * Guarantees at least one uppercase, one lowercase, one digit, and one symbol.
     */
    public static String generatePassword(int length) {
        if (length < 8)
            length = 12;

        StringBuilder sb = new StringBuilder(length);
        // Ensure at least one of each type
        sb.append(UPPERCASE.charAt(RANDOM.nextInt(UPPERCASE.length())));
        sb.append(LOWERCASE.charAt(RANDOM.nextInt(LOWERCASE.length())));
        sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        sb.append(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));

        // Fill the rest randomly
        for (int i = 4; i < length; i++) {
            sb.append(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }

        // Shuffle the characters
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return new String(arr);
    }

    /**
     * Evaluates password strength based on length and character diversity.
     */
    public static PasswordStrength evaluateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.WEAK;
        }

        int score = 0;

        // Length scoring
        if (password.length() >= 6)
            score++;
        if (password.length() >= 8)
            score++;
        if (password.length() >= 12)
            score++;
        if (password.length() >= 16)
            score++;

        // Character diversity
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSymbol = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c))
                hasUpper = true;
            else if (Character.isLowerCase(c))
                hasLower = true;
            else if (Character.isDigit(c))
                hasDigit = true;
            else
                hasSymbol = true;
        }
        if (hasUpper)
            score++;
        if (hasLower)
            score++;
        if (hasDigit)
            score++;
        if (hasSymbol)
            score++;

        // Map score to strength
        if (score <= 2)
            return PasswordStrength.WEAK;
        if (score <= 4)
            return PasswordStrength.FAIR;
        if (score <= 6)
            return PasswordStrength.GOOD;
        return PasswordStrength.STRONG;
    }
}
