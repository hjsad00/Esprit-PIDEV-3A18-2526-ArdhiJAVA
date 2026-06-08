package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.services.UserAndDiag.LangChainChatbotService;
import tn.neuron.ardhi.services.UserAndDiag.SpeechToTextService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import java.util.UUID;

public class ChatbotDiagController {

    @FXML
    private VBox messageContainer;
    @FXML
    private TextField txtInput;
    @FXML
    private Button btnMic;

    private LangChainChatbotService langChainService = new LangChainChatbotService();
    private SpeechToTextService sttService = new SpeechToTextService();
    private String sessionId;
    private String systemContext;

    public void setContext(String context) {
        // Create a unique session ID for this specific chat window instance
        this.sessionId = UUID.randomUUID().toString();
        this.systemContext = context;

        // This addMessage is just for the UI
        addMessage("IA", "Bonjour ! Je suis prêt à répondre à vos questions sur ce diagnostic.", false);
    }

    @FXML
    void toggleVoiceRecording(ActionEvent event) {
        if (sttService.isRecording()) {
            // Stop recording and transcribe
            btnMic.setText("⏳");
            btnMic.setDisable(true);
            sttService.stopAndTranscribe(
                    text -> {
                        txtInput.setText(text);
                        btnMic.setText("🎙️");
                        btnMic.setStyle(
                                "-fx-background-color: #e67e22; -fx-background-radius: 25; -fx-cursor: hand; -fx-min-width: 45; -fx-min-height: 45;");
                        btnMic.setDisable(false);
                        // Auto-send
                        sendMessage(null);
                    },
                    error -> {
                        btnMic.setText("🎙️");
                        btnMic.setStyle(
                                "-fx-background-color: #e67e22; -fx-background-radius: 25; -fx-cursor: hand; -fx-min-width: 45; -fx-min-height: 45;");
                        btnMic.setDisable(false);
                    });
        } else {
            // Start recording
            sttService.startRecording();
            btnMic.setText("⏹️");
            btnMic.setStyle(
                    "-fx-background-color: #e74c3c; -fx-background-radius: 25; -fx-cursor: hand; -fx-min-width: 45; -fx-min-height: 45;");
        }
    }

    @FXML
    void sendMessage(ActionEvent event) {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty())
            return;

        // 1. Update UI
        addMessage("Vous", msg, true);
        txtInput.clear();
        txtInput.setDisable(true);

        // 2. Add User message to History isn't needed manually anymore,
        // LangChain handles it in memory.

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                // Send session ID, context, and the new message to LangChain
                return langChainService.chat(sessionId, systemContext, msg);
            }
        };

        task.setOnSucceeded(e -> {
            String aiResponse = task.getValue();

            // 3. Update UI
            addMessage("IA", aiResponse, false);

            txtInput.setDisable(false);
            txtInput.requestFocus();
        });

        task.setOnFailed(e -> {
            addMessage("IA", "Erreur de connexion.", false);
            txtInput.setDisable(false);
            e.getSource().getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void addMessage(String sender, String text, boolean isUser) {
        Label lblText = new Label(text);
        lblText.setWrapText(true);
        lblText.setMaxWidth(280);
        lblText.setStyle("-fx-text-fill: " + (isUser ? "white" : "#333") + ";");

        HBox bubble = new HBox(lblText);
        bubble.setPadding(new javafx.geometry.Insets(10));
        bubble.setStyle("-fx-background-color: " + (isUser ? "#6B7F3F" : "#E0E0E0") + ";"
                + "-fx-background-radius: " + (isUser ? "15 15 0 15" : "15 15 15 0") + ";");

        HBox container = new HBox(bubble);
        container.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Platform.runLater(() -> {
            messageContainer.getChildren().add(container);
        });
    }

    @FXML
    void goBack(ActionEvent event) {
        WindowUtils.goBack(event);
    }
}
