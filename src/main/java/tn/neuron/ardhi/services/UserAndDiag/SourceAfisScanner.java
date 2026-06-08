package tn.neuron.ardhi.services.UserAndDiag;

import com.machinezoo.sourceafis.FingerprintImage;
import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class SourceAfisScanner implements BiometricScanner {

    @Override
    public CompletableFuture<String> capture() {
        // Create the future that will be returned
        CompletableFuture<String> resultFuture = new CompletableFuture<>();

        // Run UI operations on JavaFX Thread
        Platform.runLater(() -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Scanner d'empreinte (Sélectionner une image)");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Images d'empreintes", "*.png", "*.jpg", "*.jpeg", "*.bmp",
                                "*.tiff"));

                // Show dialog
                File selectedFile = fileChooser.showOpenDialog(null);

                if (selectedFile != null) {
                    // Process file in a background thread to avoid freezing UI during IO/Processing
                    CompletableFuture.runAsync(() -> {
                        try {
                            byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());

                            // Create Template from Image (SourceAFIS 3.x)
                            FingerprintTemplate template = new FingerprintTemplate(
                                    new FingerprintImage(imageBytes));

                            // Serialize
                            String templateStr = Base64.getEncoder().encodeToString(template.toByteArray());

                            // Complete successfully
                            resultFuture.complete(templateStr);
                        } catch (Exception e) {
                            e.printStackTrace();
                            resultFuture.completeExceptionally(e);
                        }
                    });
                } else {
                    // User cancelled
                    resultFuture.complete(null);
                }
            } catch (Exception e) {
                resultFuture.completeExceptionally(e);
            }
        });

        return resultFuture;
    }

    @Override
    public boolean verify(String capturedTemplateStr, String storedTemplateStr) {
        if (capturedTemplateStr == null || storedTemplateStr == null)
            return false;

        try {
            byte[] capturedBytes = Base64.getDecoder().decode(capturedTemplateStr);
            byte[] storedBytes = Base64.getDecoder().decode(storedTemplateStr);

            FingerprintTemplate probe = new FingerprintTemplate(capturedBytes);
            FingerprintTemplate candidate = new FingerprintTemplate(storedBytes);

            // Matcher
            FingerprintMatcher matcher = new FingerprintMatcher(probe);

            double score = matcher.match(candidate);
            return score >= 40.0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        // Always available as it uses File System
        return true;
    }
}
