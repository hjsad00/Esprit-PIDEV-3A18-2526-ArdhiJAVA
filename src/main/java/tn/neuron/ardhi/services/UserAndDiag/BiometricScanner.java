package tn.neuron.ardhi.services.UserAndDiag;

public interface BiometricScanner {
    /**
     * Captures a fingerprint template from the scanner asynchronously.
     * 
     * @return A CompletableFuture containing the captured template as a String
     *         (Base64), or null if failed/cancelled.
     */
    java.util.concurrent.CompletableFuture<String> capture();

    /**
     * Verifies if a captured template matches a stored template.
     * 
     * @param capturedTemplate The template just captured.
     * @param storedTemplate   The template stored in the database.
     * @return true if they match, false otherwise.
     */
    boolean verify(String capturedTemplate, String storedTemplate);

    /**
     * Checks if the scanner hardware is available.
     * 
     * @return true if available.
     */
    boolean isAvailable();
}
