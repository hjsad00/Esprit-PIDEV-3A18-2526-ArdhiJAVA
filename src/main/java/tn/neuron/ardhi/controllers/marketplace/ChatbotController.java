package tn.neuron.ardhi.controllers.marketplace;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.marketplace.ChatIntent;
import tn.neuron.ardhi.services.marketplace.ChatbotService;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur JavaFX du chatbot marketplace.
 * Gère l'interface chat : affichage des bulles, envoi des messages,
 * appel asynchrone au ChatbotService.
 */
public class ChatbotController implements Initializable {

    @FXML private VBox messagesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextArea inputField;
    @FXML private Button sendButton;
    @FXML private HBox loadingBox;
    @FXML private Label statusLabel;

    private ChatbotService chatbotService;
    private int userId;
    private CatalogueProduitController catalogueController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        chatbotService = new ChatbotService();

        // Ctrl+Entrée pour envoyer
        inputField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                envoyerMessage();
                event.consume();
            }
        });

        // Auto-scroll vers le bas quand de nouveaux messages arrivent
        messagesBox.heightProperty().addListener((obs, oldVal, newVal) -> scrollPane.setVvalue(1.0));

        // Message de bienvenue
        afficherMessageBot(
                "👋 Bonjour ! Je suis votre assistant du **Marketplace Agricole**.\n\n" +
                        "Je peux vous aider à :\n" +
                        "• 🛒 Acheter des produits (ex: \"Je veux 2 kg de tomates\")\n" +
                        "• 📦 Vérifier la disponibilité (ex: \"Avez-vous des olives ?\")\n" +
                        "• 🔍 Filtrer le catalogue (ex: \"Montre moi les fruits\")\n" +
                        "• 🗑️ Supprimer un article du panier\n\n" +
                        "Que puis-je faire pour vous ?");
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setCatalogueController(CatalogueProduitController controller) {
        this.catalogueController = controller;
    }

    // -------------------------------------------------------------------------
    // Envoi du message
    // -------------------------------------------------------------------------

    @FXML
    private void envoyerMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        afficherMessageUtilisateur(message);
        inputField.clear();

        setInputEnabled(false);
        setLoading(true);

        Thread thread = new Thread(() -> {
            // ── CORRECTION : traiterMessage retourne ChatIntent, pas String ──
            ChatIntent intent = chatbotService.traiterMessage(message, userId);

            Platform.runLater(() -> {
                setLoading(false);
                setInputEnabled(true);

                // Lire le texte depuis intent.getTexteReponse()
                afficherMessageBot(intent.getTexteReponse());

                inputField.requestFocus();

                // Rafraîchir le panier dans le catalogue
                if (catalogueController != null) {
                    catalogueController.rafraichirAffichagePanier();

                    // Si intention "filtrer" → appliquer les filtres visuels
                    if ("filtrer".equals(intent.getIntention())) {
                        catalogueController.appliquerFiltresDuChatbot(intent);
                    }
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) messagesBox.getScene().getWindow();
        stage.close();
    }

    // -------------------------------------------------------------------------
    // Affichage des bulles de messages
    // -------------------------------------------------------------------------

    private void afficherMessageUtilisateur(String texte) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        Label bubble = new Label(texte);
        bubble.setWrapText(true);
        bubble.setMaxWidth(340);
        bubble.setTextAlignment(TextAlignment.LEFT);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white;" +
                        "-fx-background-radius: 18 18 4 18; -fx-font-size: 13px;");
        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
    }

    private void afficherMessageBot(String texte) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 20px;");
        avatar.setMinWidth(32);
        Label bubble = new Label(texte);
        bubble.setWrapText(true);
        bubble.setMaxWidth(360);
        bubble.setTextAlignment(TextAlignment.LEFT);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setStyle(
                "-fx-background-color: white; -fx-text-fill: #2c3e50;" +
                        "-fx-background-radius: 18 18 18 4; -fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);");
        row.getChildren().addAll(avatar, bubble);
        messagesBox.getChildren().add(row);
    }

    // -------------------------------------------------------------------------
    // États UI
    // -------------------------------------------------------------------------

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setDisable(!enabled);
        sendButton.setDisable(!enabled);
        sendButton.setStyle(enabled
                ? "-fx-background-color: #27ae60; -fx-text-fill: white;" +
                "-fx-font-size: 18px; -fx-background-radius: 24; -fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(39,174,96,0.4), 6, 0, 0, 2);"
                : "-fx-background-color: #95a5a6; -fx-text-fill: white;" +
                "-fx-font-size: 18px; -fx-background-radius: 24;");
    }
}