package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.UserAndDiag.Abonnement;
import tn.neuron.ardhi.models.UserAndDiag.Offre;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.UserAndDiag.AbonnementService;
import tn.neuron.ardhi.services.UserAndDiag.OffreService;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.PDFGenerator;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

import java.util.ResourceBundle;

/**
 * Contrôleur pour la page "S'abonner" - affiche les offres dynamiquement depuis
 * la BDD.
 */
public class SabonnerController implements Initializable {

    @FXML
    private Label lblStatut;
    @FXML
    private Button btnFacture;
    @FXML
    private Button btnAnnuler;
    @FXML
    private HBox offresContainer;
    @FXML
    private Label lblEmptyState;

    private User currentUser;
    private AbonnementService as = new AbonnementService();
    private OffreService os = new OffreService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = UserSession.getInstance().getUser();

        verifierAbonnement();
        chargerOffres();
    }

    /**
     * Charge les offres actives depuis la BDD et génère les cartes dynamiquement.
     */
    private void chargerOffres() {
        try {
            List<Offre> offres = os.recupererActives();

            if (offres.isEmpty()) {
                lblEmptyState.setText("Aucune offre disponible pour le moment.");
                lblEmptyState.setVisible(true);
            } else {
                lblEmptyState.setVisible(false);
                offresContainer.getChildren().clear();

                for (Offre offre : offres) {
                    VBox card = creerCarteOffre(offre);
                    offresContainer.getChildren().add(card);
                }
            }
        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error loading offers", e);
            lblEmptyState.setText("Erreur lors du chargement des offres.");
            lblEmptyState.setVisible(true);
        }
    }

    /**
     * Crée une carte d'offre visuellement attractive.
     */
    private VBox creerCarteOffre(Offre offre) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(320);
        card.setPrefHeight(400);
        card.setPadding(new Insets(offre.isEstRecommandee() ? 15 : 30, 25, 30, 25));

        // Style de la carte selon si c'est recommandée ou non
        if (offre.isEstRecommandee()) {
            card.setStyle("-fx-background-color: linear-gradient(to bottom right, #FFF8F0, #FFF5E8); " +
                    "-fx-background-radius: 20; -fx-border-color: " + offre.getCouleurPrimaire() + "; " +
                    "-fx-border-width: 2; -fx-border-radius: 20;");
        } else {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        }

        // Effet d'ombre
        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setColor(offre.isEstRecommandee()
                ? Color.web(offre.getCouleurPrimaire(), 0.3)
                : Color.web("#000000", 0.12));
        card.setEffect(shadow);

        // Badge "Meilleure offre" si recommandée
        if (offre.isEstRecommandee()) {
            Label badge = new Label("⭐ MEILLEURE OFFRE");
            badge.setTextFill(Color.WHITE);
            badge.setFont(Font.font("System", FontWeight.BOLD, 10));
            badge.setStyle("-fx-background-color: linear-gradient(to right, " +
                    offre.getCouleurPrimaire() + ", " + offre.getCouleurSecondaire() + "); " +
                    "-fx-background-radius: 5; -fx-padding: 4 12;");
            card.getChildren().add(badge);
        }

        // Nom de l'offre
        Label lblNom = new Label(offre.getNom());
        lblNom.setTextFill(Color.web(offre.getCouleurPrimaire()));
        lblNom.setFont(Font.font("System", FontWeight.BOLD, 22));
        card.getChildren().add(lblNom);

        // Séparateur
        Separator separator = new Separator();
        separator.setPrefWidth(250);
        card.getChildren().add(separator);

        // Prix
        Label lblPrix = new Label(String.format("%.2f DT", offre.getPrixMensuel()));
        lblPrix.setTextFill(Color.web("#333333"));
        lblPrix.setFont(Font.font("System", FontWeight.BOLD, 42));
        card.getChildren().add(lblPrix);

        Label lblMois = new Label("/ mois");
        lblMois.setTextFill(Color.web("#999999"));
        lblMois.setFont(Font.font("System", 14));
        card.getChildren().add(lblMois);

        // Avantages (Scrollable)
        VBox avantagesBox = new VBox(10);
        avantagesBox.setAlignment(Pos.CENTER_LEFT);
        avantagesBox.setPadding(new Insets(5, 5, 5, 0)); // Reduced padding for scrollpane content

        for (String avantage : offre.getAvantagesAsList()) {
            Label lblAvantage = new Label("✓ " + avantage);
            lblAvantage.setTextFill(Color.web("#555555"));
            lblAvantage.setFont(Font.font("System", 13));
            lblAvantage.setWrapText(true); // Allow text wrapping
            avantagesBox.getChildren().add(lblAvantage);
        }

        // Wrap listing in ScrollPane
        ScrollPane scrollPane = new ScrollPane(avantagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        scrollPane.setPrefHeight(150); // Set a preferred height for the scrollable area

        card.getChildren().add(scrollPane);

        // Spacer to push button to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        card.getChildren().add(spacer);

        // Bouton de souscription
        Button btnSouscrire = new Button("Choisir ce pack");
        btnSouscrire.setPrefWidth(240);
        btnSouscrire.setTextFill(Color.WHITE);
        btnSouscrire.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnSouscrire.setStyle("-fx-background-color: linear-gradient(to right, " +
                offre.getCouleurPrimaire() + ", " + offre.getCouleurSecondaire() + "); " +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btnSouscrire.setPadding(new Insets(12, 0, 12, 0));

        // Action du bouton - passer l'offre à la méthode de paiement
        btnSouscrire.setOnAction(event -> ouvrirPaiement(offre));

        card.getChildren().add(btnSouscrire);

        return card;
    }

    private Abonnement trouverAbonnementActif() {
        try {
            List<Abonnement> mesAbonnements = as.recupererParUser(currentUser.getId());
            for (Abonnement a : mesAbonnements) {
                if ("ACTIF".equals(a.getStatut())) {
                    return a;
                }
            }
        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error verifying subscription", e);
        }
        return null;
    }

    public void verifierAbonnement() {
        Abonnement abo = trouverAbonnementActif();

        if (abo != null) {
            lblStatut.setText(abo.getType() + " (Actif)");
            lblStatut.setStyle("-fx-text-fill: #2c8f3e;");
            btnFacture.setVisible(true);
            btnAnnuler.setVisible(true);
        } else {
            lblStatut.setText("GRATUIT");
            lblStatut.setStyle("-fx-text-fill: #777777;");
            btnFacture.setVisible(false);
            btnAnnuler.setVisible(false);
        }
    }

    @FXML
    void telechargerFacture(ActionEvent event) {
        try {
            Abonnement abo = trouverAbonnementActif();

            if (abo == null) {
                WindowUtils.showAlert("Erreur", "Aucun abonnement actif trouvé.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer la facture");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

            String defaultName = "Facture_" + abo.getType().replace(" ", "_") + ".pdf";
            fileChooser.setInitialFileName(defaultName);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                PDFGenerator pdfGen = new PDFGenerator();
                pdfGen.genererFacturePDF(file.getAbsolutePath(), currentUser, abo);

                WindowUtils.showAlert("Succès", "Votre facture a été téléchargée avec succès !");
            }
        } catch (Exception e) {
            LogUtils.error(this.getClass(), "Error generating PDF", e);
            WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de générer le PDF : " + e.getMessage());
        }
    }

    @FXML
    void annulerAbonnement(ActionEvent event) {
        if (WindowUtils.showConfirmation("Annulation",
                "Annuler votre abonnement ? Vous perdrez vos avantages immédiatement.")) {
            try {
                as.annulerAbonnement(currentUser.getId());
                WindowUtils.showAlert("Succès", "Votre abonnement a été annulé.");
                verifierAbonnement();
            } catch (Exception e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de l'annulation : " + e.getMessage());
            }
        }
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.goBack(event);
    }

    /**
     * Ouvre le popup de paiement avec les informations de l'offre.
     */
    private void ouvrirPaiement(Offre offre) {
        try {
            if (as.aAbonnementActif(currentUser.getId())) {
                boolean confirm = WindowUtils.showConfirmation("Abonnement existant",
                        "Vous avez déjà un abonnement actif. Votre abonnement actuel sera automatiquement annulé si vous souscrivez à ce nouveau pack. Continuer ?");

                if (!confirm) {
                    return;
                }
            }

            WindowUtils.loadPopup("/fxml/UserAndDiag/PaiementAbonnement.fxml", "Paiement Sécurisé - " + offre.getNom(),
                    (PaiementAbonnementController pc) -> pc.setDonneesOffre(offre, 1, this));

        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error checking subscription", e);
            WindowUtils.showAlert("Erreur", "Erreur lors de la vérification de l'abonnement : " + e.getMessage());
        }
    }

}