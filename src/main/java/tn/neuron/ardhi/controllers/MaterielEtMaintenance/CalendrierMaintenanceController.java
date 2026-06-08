package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.MaterielEtMaintenance.EmailService;
import tn.neuron.ardhi.services.MaterielEtMaintenance.GoogleCalendarService;
import tn.neuron.ardhi.services.MaterielEtMaintenance.MaintenancePdfService;
import tn.neuron.ardhi.services.MaterielEtMaintenance.MaterielService;
import tn.neuron.ardhi.services.MaterielEtMaintenance.WhatsAppService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class CalendrierMaintenanceController implements Initializable {

    @FXML private Label            lblTitre;
    @FXML private Label            lblMaterielInfo;
    @FXML private DatePicker       dpDate;
    @FXML private ComboBox<String> cmbHeure;
    @FXML private TextArea         txtDescription;
    @FXML private ComboBox<String> cmbTypeMaintenance;
    @FXML private Button           btnPlanifier;

    private Materiel              materiel;
    private GoogleCalendarService googleCalendarService;
    private MaterielService       materielService;
    private EmailService          emailService;
    private MaintenancePdfService pdfService;
    private WhatsAppService       whatsAppService;   // ✅ NOUVEAU

    private User   utilisateurConnecte;
    private String dernierGoogleEventId;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            utilisateurConnecte = UserSession.getInstance().getUser();
        } catch (Exception e) {
            utilisateurConnecte = null;
        }

        if (utilisateurConnecte == null) {
            showError("Erreur de session", "Aucun utilisateur connecté.");
            return;
        }

        // Services
        String userEmail = utilisateurConnecte.getEmail();
        googleCalendarService = new GoogleCalendarService(userEmail);
        materielService       = new MaterielService();
        emailService          = new EmailService();
        pdfService            = new MaintenancePdfService();
        whatsAppService       = new WhatsAppService();  // ✅ NOUVEAU

        // Types de maintenance
        cmbTypeMaintenance.getItems().addAll(
                "Préventive", "Corrective", "Urgente", "Révision complète");
        cmbTypeMaintenance.setValue("Préventive");

        // Heures 08h-18h
        for (int i = 8; i <= 18; i++) {
            cmbHeure.getItems().add(String.format("%02d:00", i));
            if (i != 18) cmbHeure.getItems().add(String.format("%02d:30", i));
        }
        cmbHeure.setValue("09:00");

        dpDate.setValue(LocalDate.now().plusMonths(1));
        txtDescription.setText("Maintenance préventive du matériel.\n" +
                "Vérification générale, graissage, contrôle des niveaux.");
    }

    public void setMateriel(Materiel materiel) {
        this.materiel = materiel;
        if (materiel != null) {
            lblTitre.setText("Planifier Maintenance");
            lblMaterielInfo.setText("Matériel: " + materiel.getNom()
                    + " | Type: " + materiel.getType()
                    + " | État: " + materiel.getEtat());
            if (materiel.getDate_prochaine_maintenance() != null)
                dpDate.setValue(materiel.getDate_prochaine_maintenance());
        }
    }

    // ════════════════════════════════════════════════════════
    //  PLANIFIER — bouton principal
    // ════════════════════════════════════════════════════════

    @FXML
    private void handlePlanifier() {
        if (!validerFormulaire()) return;

        btnPlanifier.setDisable(true);
        btnPlanifier.setText("Traitement en cours...");

        LocalDate     date    = dpDate.getValue();
        String[]      hParts  = cmbHeure.getValue().split(":");
        LocalTime     time    = LocalTime.of(Integer.parseInt(hParts[0]),
                hParts.length > 1 ? Integer.parseInt(hParts[1]) : 0);
        LocalDateTime dateTime = LocalDateTime.of(date, time);

        String typeMaintenance  = cmbTypeMaintenance.getValue();
        String description      = txtDescription.getText().trim();
        String nomAgriculteur   = utilisateurConnecte.getNom() != null
                ? utilisateurConnecte.getNom() : utilisateurConnecte.getEmail();
        String emailAgriculteur = utilisateurConnecte.getEmail();

        // ✅ Numéro de téléphone depuis le modèle User
        String telephone = utilisateurConnecte.getPhone() != null
                ? utilisateurConnecte.getPhone() : "";

        String descriptionComplete = buildDescriptionComplete(
                typeMaintenance, description, nomAgriculteur);

        Task<ResultatPlanification> task = new Task<>() {
            @Override
            protected ResultatPlanification call() {
                ResultatPlanification res = new ResultatPlanification();

                // 1. Google Calendar
                updateMessage("Ajout dans Google Calendar...");
                res.googleEventId = googleCalendarService.planifierMaintenance(
                        materiel, dateTime, descriptionComplete);

                // 2. Base de données
                updateMessage("Mise à jour de la base de données...");
                res.materielUpdated = materielService.planifierMaintenance(
                        materiel.getId_materiel(), date, res.googleEventId);

                res.maintenanceCreated = sauvegarderDansHistorique(
                        materiel.getId_materiel(), date, description, typeMaintenance);

                // 3. PDF
                updateMessage("Génération du PDF...");
                File pdfFile = pdfService.genererPdfConfirmation(
                        nomAgriculteur, emailAgriculteur,
                        materiel.getNom(), materiel.getType(), materiel.getEtat(),
                        typeMaintenance, dateTime, description, res.googleEventId);

                // 4. Email
                updateMessage("Envoi de l'email...");
                res.emailEnvoye = emailService.envoyerConfirmationMaintenance(
                        emailAgriculteur, nomAgriculteur,
                        materiel.getNom(), materiel.getType(), materiel.getEtat(),
                        typeMaintenance, dateTime, description, pdfFile);

                // 5. ✅ WhatsApp — confirmation immédiate
                updateMessage("Envoi du message WhatsApp...");
                if (telephone != null && !telephone.isEmpty()) {
                    res.whatsAppEnvoye = whatsAppService.envoyerConfirmation(
                            telephone,
                            nomAgriculteur,
                            materiel.getNom(),
                            typeMaintenance,
                            dateTime,
                            description
                    );
                } else {
                    System.out.println("Pas de numéro de téléphone - WhatsApp ignoré");
                    res.whatsAppEnvoye = false;
                }

                return res;
            }
        };

        task.setOnSucceeded(e -> {
            ResultatPlanification res = task.getValue();
            dernierGoogleEventId = res.googleEventId;

            btnPlanifier.setDisable(false);
            btnPlanifier.setText("Planifier sur Google Calendar");

            if (res.materielUpdated && res.maintenanceCreated) {
                StringBuilder msg = new StringBuilder();
                msg.append("MAINTENANCE PLANIFIEE AVEC SUCCES !\n\n");
                msg.append("Materiel  : ").append(materiel.getNom()).append("\n");
                msg.append("Date      : ").append(dateTime.format(FORMATTER)).append("\n");
                msg.append("Type      : ").append(typeMaintenance).append("\n\n");
                msg.append("Google Calendar : ")
                        .append(res.googleEventId != null
                                && !res.googleEventId.startsWith("SIM_")
                                ? "OK" : "Simulation").append("\n");
                msg.append("Email           : ")
                        .append(res.emailEnvoye
                                ? "OK -> " + emailAgriculteur : "Erreur").append("\n");
                // ✅ Statut WhatsApp dans le message de succès
                msg.append("WhatsApp        : ")
                        .append(res.whatsAppEnvoye
                                ? "OK -> " + telephone
                                : telephone.isEmpty()
                                ? "Pas de numero renseigne"
                                : "Erreur");

                showSuccess("Maintenance Planifiée", msg.toString());
                fermerFenetre();
            } else {
                showWarning("Attention",
                        "Maintenance enregistrée dans Google Calendar mais une erreur " +
                                "est survenue lors de la sauvegarde locale.");
            }
        });

        task.setOnFailed(e -> {
            btnPlanifier.setDisable(false);
            btnPlanifier.setText("Planifier sur Google Calendar");
            Throwable ex = task.getException();
            showError("Erreur", ex != null ? ex.getMessage() : "Erreur inconnue");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ════════════════════════════════════════════════════════
    //  DESCRIPTION COMPLÈTE
    // ════════════════════════════════════════════════════════

    private String buildDescriptionComplete(String typeMaintenance,
                                            String description,
                                            String nomAgriculteur) {
        return "Type de maintenance: " + typeMaintenance + "\n"
                + "Agriculteur: " + nomAgriculteur + "\n\n"
                + "Description des travaux:\n" + description;
    }

    // ════════════════════════════════════════════════════════
    //  SAUVEGARDE HISTORIQUE
    // ════════════════════════════════════════════════════════

    private boolean sauvegarderDansHistorique(int materielId, LocalDate date,
                                              String description,
                                              String typeMaintenance) {
        String sql = "INSERT INTO maintenance " +
                "(materiel_id, description, date_maintenance, cout) VALUES (?, ?, ?, ?)";
        try {
            Connection conn = MyDatabase.getInstance().getCnx();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, materielId);
            stmt.setString(2, "[" + typeMaintenance + "] " + description);
            stmt.setDate(3, java.sql.Date.valueOf(date));
            stmt.setDouble(4, 0.0);
            int rows = stmt.executeUpdate();
            stmt.close();
            return rows > 0;
        } catch (Exception e) {
            System.err.println("Erreur sauvegarde: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════
    //  ACTIONS FXML
    // ════════════════════════════════════════════════════════

    @FXML private void handleAnnuler() { fermerFenetre(); }

    // ════════════════════════════════════════════════════════
    //  VALIDATION
    // ════════════════════════════════════════════════════════

    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();
        if (dpDate.getValue() == null)
            erreurs.append("• Veuillez sélectionner une date\n");
        else if (dpDate.getValue().isBefore(LocalDate.now()))
            erreurs.append("• La date ne peut pas être dans le passé\n");
        if (cmbHeure.getValue() == null || cmbHeure.getValue().isEmpty())
            erreurs.append("• Veuillez sélectionner une heure\n");
        if (txtDescription.getText() == null
                || txtDescription.getText().trim().length() < 10)
            erreurs.append("• La description doit contenir au moins 10 caractères\n");
        if (cmbTypeMaintenance.getValue() == null)
            erreurs.append("• Veuillez sélectionner un type de maintenance\n");

        if (erreurs.length() > 0) {
            showWarning("Validation", erreurs.toString());
            return false;
        }
        return true;
    }

    // ════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════

    private void showSuccess(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
    private void showWarning(String t, String m) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
    private void showInfo(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
    private void fermerFenetre() {
        ((Stage) lblTitre.getScene().getWindow()).close();
    }

    // ════════════════════════════════════════════════════════
    //  CLASSE INTERNE RÉSULTAT
    // ════════════════════════════════════════════════════════

    private static class ResultatPlanification {
        String  googleEventId;
        boolean materielUpdated;
        boolean maintenanceCreated;
        boolean emailEnvoye;
        boolean whatsAppEnvoye;   // ✅ NOUVEAU
    }
}