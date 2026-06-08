package tn.neuron.ardhi.services.marketplace;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Service de reconnaissance vocale utilisant Vosk (offline).
 * Utilisation :
 *   SpeechToTextService stt = new SpeechToTextService();
 *   stt.startListening(text -> System.out.println("Reconnu : " + text));
 *   // ... plus tard :
 *   stt.stopListening();
 */
public class SpeechToTextService {

    // 🔧 Chemin vers le modèle Vosk français (dans resources)
    private static final String MODEL_PATH = "src/main/resources/models/vosk-fr";

    private Model model;
    private Recognizer recognizer;
    private TargetDataLine microphone;
    private Thread listeningThread;
    private volatile boolean isListening = false;

    /**
     * Charge le modèle Vosk en mémoire.
     * À appeler une seule fois au démarrage (ex: dans le contrôleur).
     */
    public void loadModel() throws IOException {
        System.out.println("[STT] Chargement du modèle Vosk français...");
        this.model = new Model(MODEL_PATH);
        System.out.println("[STT] Modèle chargé avec succès ✅");
    }

    /**
     * Démarre l'écoute du microphone en temps réel.
     * @param onTextRecognized Callback appelé à chaque phrase reconnue
     */
    public void startListening(Consumer<String> onTextRecognized) {
        if (isListening) {
            System.out.println("[STT] Déjà en cours d'écoute.");
            return;
        }

        listeningThread = new Thread(() -> {
            try {
                // Format audio : 16kHz, 16-bit, mono (requis par Vosk)
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("[STT] ❌ Microphone non supporté sur ce système.");
                    return;
                }

                recognizer = new Recognizer(model, 16000);
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                isListening = true;
                System.out.println("[STT] 🎤 Écoute démarrée...");

                byte[] buffer = new byte[4096];

                while (isListening) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);

                    if (bytesRead > 0) {
                        // Résultat partiel (temps réel)
                        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                            // Résultat final (fin de phrase)
                            String result = recognizer.getResult();
                            String text = extractText(result);
                            if (!text.isEmpty()) {
                                System.out.println("[STT] ✅ Reconnu : " + text);
                                onTextRecognized.accept(text);
                            }
                        }
                        // Tu peux aussi utiliser recognizer.getPartialResult()
                        // pour afficher le texte en temps réel (optionnel)
                    }
                }

                // Flush du dernier résultat
                String finalResult = recognizer.getFinalResult();
                String finalText = extractText(finalResult);
                if (!finalText.isEmpty()) {
                    onTextRecognized.accept(finalText);
                }

            } catch (LineUnavailableException e) {
                System.err.println("[STT] ❌ Microphone indisponible : " + e.getMessage());
            } catch (IOException e) {
                System.err.println("[STT] ❌ Erreur Vosk : " + e.getMessage());
            } finally {
                cleanup();
            }
        });

        listeningThread.setDaemon(true);
        listeningThread.start();
    }

    /**
     * Arrête l'écoute proprement.
     */
    public void stopListening() {
        System.out.println("[STT] 🛑 Arrêt de l'écoute...");
        isListening = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }

    /**
     * Indique si l'écoute est en cours.
     */
    public boolean isListening() {
        return isListening;
    }

    /**
     * Libère les ressources (à appeler à la fermeture de l'app).
     */
    public void cleanup() {
        isListening = false;
        if (microphone != null && microphone.isOpen()) {
            microphone.stop();
            microphone.close();
        }
        if (recognizer != null) {
            recognizer.close();
        }
        // Note: model.close() est optionnel si l'app se ferme
    }

    // -------------------------------------------------------
    // Méthode privée : extrait le texte du JSON Vosk
    // Vosk retourne : {"text": "bonjour comment allez vous"}
    // -------------------------------------------------------
    private String extractText(String jsonResult) {
        try {
            JSONObject json = new JSONObject(jsonResult);
            return json.optString("text", "").trim();
        } catch (Exception e) {
            return "";
        }
    }
}
