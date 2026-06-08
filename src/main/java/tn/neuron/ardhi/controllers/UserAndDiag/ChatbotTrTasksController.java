package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import tn.neuron.ardhi.services.UserAndDiag.LangChainChatbotService;
import java.util.UUID;

public class ChatbotTrTasksController {

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField inputField;

    private LangChainChatbotService langChainService = new LangChainChatbotService();
    private String sessionId;
    private String systemContext;

    public void setContext(String systemContext) {
        this.sessionId = UUID.randomUUID().toString();
        this.systemContext = systemContext;

        // Add the initial friendly greeting to the UI only
        chatArea.appendText(
                "Assistant: Bonjour ! Je suis votre expert agronome. Comment puis-je vous aider avec cette étape ?\n\n");
    }

    @FXML
    void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty())
            return;

        chatArea.appendText("Moi: " + message + "\n");
        inputField.clear();
        inputField.setDisable(true);

        // 2. Append to History manually isn't needed anymore

        new Thread(() -> {
            try {
                // 3. Send via LangChain Service which handles the memory
                String response = langChainService.chat(sessionId, systemContext, message);

                Platform.runLater(() -> {
                    // 4. Update UI
                    chatArea.appendText("Assistant: " + response + "\n\n");

                    inputField.setDisable(false);
                    inputField.requestFocus();

                    // Auto-scroll to bottom
                    chatArea.setScrollTop(Double.MAX_VALUE);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    chatArea.appendText("[Erreur: Impossible de contacter l'assistant]\n");
                    inputField.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }
}
