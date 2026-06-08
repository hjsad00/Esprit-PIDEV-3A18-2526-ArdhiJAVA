package tn.neuron.ardhi.services.UserAndDiag;

import javafx.scene.image.Image;

public interface FaceRecognitionService {
    /**
     * Extracts a face from the given JavaFX image, and returns a Base64 encoded
     * string
     * representing the face signature (the cropped face image or embeddings).
     * 
     * @param image The full camera snapshot
     * @return Base64 string of the face signature, or null if no face found.
     */
    String extractFaceSignature(Image image);

    /**
     * Verifies if the captured face image matches the stored signature.
     * 
     * @param capturedImage   The new camera snapshot
     * @param storedSignature The stored Base64 string
     * @return true if they match, false otherwise.
     */
    boolean verifyFace(Image capturedImage, String storedSignature);
}
