package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.net.URL;
import java.util.ResourceBundle;

public class ChatbotIAController implements Initializable {

    @FXML private ScrollPane scrollChatbot;
    @FXML private VBox       vboxConversation;
    @FXML private TextField  txtChatInput;
    @FXML private Button     btnEnvoyer;

    // Nouveaux composants v2
    @FXML private HBox  hboxSuggestions;   // zone suggestions contextuelles (peut être null si absent du FXML)
    @FXML private Label lblMsgCount;       // compteur de messages (peut être null)

    private ChatbotController chatbot;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        int userId = UserSession.getInstance().getUser().getId();

        chatbot = new ChatbotController();
        chatbot.initialiser(scrollChatbot, vboxConversation, txtChatInput, btnEnvoyer, userId);

        // Injection des composants optionnels v2
        if (hboxSuggestions != null) chatbot.setHboxSuggestions(hboxSuggestions);
        if (lblMsgCount     != null) chatbot.setLblMsgCount(lblMsgCount);

        chatbot.setOnPlanifierMaintenance(() -> {
            try {
                Stage stage = (Stage) txtChatInput.getScene().getWindow();
                stage.close();
                Parent root = FXMLLoader.load(
                        getClass().getResource("/fxml/MaterielEtMaintenance/Materiels.fxml"));
                Stage newStage = new Stage();
                newStage.setScene(new Scene(root, 1300, 800));
                newStage.setTitle("Mes Matériels — ARDHI");
                newStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ── Questions rapides ────────────────────────────────────────
    @FXML private void handleQuestionBruit()      { chatbot.poserQuestionRapide("Mon tracteur fait un bruit bizarre au démarrage"); }
    @FXML private void handleQuestionDemarrage()  { chatbot.poserQuestionRapide("Mon matériel ne démarre plus, rien ne se passe"); }
    @FXML private void handleQuestionSurchauffe() { chatbot.poserQuestionRapide("Mon moteur surchauffe, la température monte trop haut"); }
    @FXML private void handleQuestionFuite()      { chatbot.poserQuestionRapide("Il y a une fuite de liquide sous mon matériel"); }
    @FXML private void handleQuestionVibration()  { chatbot.poserQuestionRapide("Mon matériel vibre de manière inhabituelle en roulant"); }
    @FXML private void handleQuestionPuissance()  { chatbot.poserQuestionRapide("Mon tracteur perd de la puissance avec de la fumée noire"); }
    @FXML private void handleQuestionCout()       { chatbot.poserQuestionRapide("Quel est le budget maintenance annuel pour un tracteur ?"); }
    @FXML private void handleQuestionSaison()     { chatbot.poserQuestionRapide("Quels entretiens faire avant la saison des récoltes en été ?"); }
    @FXML private void handleQuestionEntretien()  { chatbot.poserQuestionRapide("Quelles sont les bonnes pratiques d'entretien préventif ?"); }
    @FXML private void handleQuestionMateriel()   { chatbot.poserQuestionRapide("Quel matériel agricole dois-je entretenir en priorité ?"); }

    @FXML private void handleEnvoyer()            { chatbot.envoyerMessage(); }
    @FXML private void handleFermer()             { ((Stage) txtChatInput.getScene().getWindow()).close(); }

    /** Réinitialise la conversation */
    @FXML private void handleNouvelleConversation() {
        chatbot.reinitialiser();
    }

    // ── Ouverture statique ───────────────────────────────────────
    public static void ouvrir() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ChatbotIAController.class.getResource(
                            "/fxml/MaterielEtMaintenance/chatbot-ia.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1050, 780));
            stage.setTitle("🌿 ARDHI Assistant IA — Expert maintenance agricole");
            stage.setMinWidth(750);
            stage.setMinHeight(580);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
