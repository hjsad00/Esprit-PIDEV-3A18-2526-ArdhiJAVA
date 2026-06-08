package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Materiel;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Notification;
import tn.neuron.ardhi.services.MaterielEtMaintenance.MaterielService;
import tn.neuron.ardhi.services.MaterielEtMaintenance.NotificationService;
import tn.neuron.ardhi.utils.MaterielEtMaintenance.MaintenanceAlertUtils;
import tn.neuron.ardhi.utils.MaterielEtMaintenance.NotificationScheduler;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MaterielsController implements Initializable {

    @FXML private TableView<Materiel>            tableMateriels;
    @FXML private TableColumn<Materiel, Integer> colId;
    @FXML private TableColumn<Materiel, String>  colNom;
    @FXML private TableColumn<Materiel, String>  colType;
    @FXML private TableColumn<Materiel, String>  colEtat;
    @FXML private TableColumn<Materiel, LocalDate> colDateAchat;
    @FXML private TableColumn<Materiel, String>  colMaintenance;
    @FXML private TableColumn<Materiel, String>  colActions;

    @FXML private ComboBox<String> cmbFilterType;
    @FXML private ComboBox<String> cmbFilterEtat;
    @FXML private Label            lblCount;
    @FXML private TextField        txtRecherche;

    @FXML private VBox  notificationBox;
    @FXML private Label lblNotifications;
    @FXML private Label lblBadgeNotif;
    @FXML private VBox  panelNotifications;
    @FXML private VBox  listeNotifications;
    @FXML private VBox  vboxAucune;

    // ── IA ────────────────────────────────────────────────────
    @FXML private VBox  vboxIAPredictions;
    @FXML private VBox  colonneIA;           // La VBox colonne droite entière
    @FXML private Button btnToggleIA;        // Bouton toggle panel IA
    private PanelIAController panelIA;
    private boolean panelIAVisible = true;

    private MaterielService     materielService     = new MaterielService();
    private NotificationService notificationService = new NotificationService();

    private ObservableList<Materiel> tousLesMateriels = FXCollections.observableArrayList();
    private DateTimeFormatter        dateFormatter     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private boolean                  panelNotifVisible = false;
    private ScheduledExecutorService pollingScheduler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id_materiel"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colEtat.setCellValueFactory(new PropertyValueFactory<>("etat"));
        colDateAchat.setCellValueFactory(new PropertyValueFactory<>("date_achat"));

        colDateAchat.setCellFactory(column -> new TableCell<Materiel, LocalDate>() {
            @Override protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText((empty || date == null) ? null : dateFormatter.format(date));
            }
        });

        colMaintenance.setCellFactory(param -> new TableCell<Materiel, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setStyle(""); return; }
                Materiel m = getTableView().getItems().get(getIndex());
                setText(MaintenanceAlertUtils.getMaintenanceMessage(m));
                switch (MaintenanceAlertUtils.getMaintenanceUrgencyLevel(m)) {
                    case "URGENT":        setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold; -fx-background-color: #ffcdd2;"); break;
                    case "CETTE_SEMAINE": setStyle("-fx-text-fill: #f57f17; -fx-font-weight: bold; -fx-background-color: #fff9c4;"); break;
                    case "CE_MOIS":       setStyle("-fx-text-fill: #e65100; -fx-background-color: #fff3e0;"); break;
                    case "BIENTOT":       setStyle("-fx-text-fill: #0277bd; -fx-background-color: #e1f5fe;"); break;
                    default:              setStyle("-fx-text-fill: #2e7d32; -fx-background-color: #e8f5e9;"); break;
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<Materiel, String>() {
            private final Button btn = new Button("Détails");
            { btn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 11px;");
                btn.setOnAction(e -> ouvrirDetailsMateriel(getTableView().getItems().get(getIndex()))); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty); setGraphic(empty ? null : btn);
            }
        });

        initialiserFiltres();

        int userId = UserSession.getInstance().getUser().getId();

        // Initialiser panneau IA avant chargement
        if (vboxIAPredictions != null) {
            panelIA = new PanelIAController();
            panelIA.initialiser(userId, vboxIAPredictions);
        }

        chargerMesMateriels();
        verifierNotificationsMaintenance();
        NotificationScheduler.getInstance().demarrer(userId);
        demarrerPollingBadge();

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            rafraichirBadge();
        }).start();
    }

    // ════════════════════════════════════════════════════════
    //  TOGGLE PANEL IA (NOUVEAU)
    // ════════════════════════════════════════════════════════

    @FXML
    private void handleToggleIA() {
        panelIAVisible = !panelIAVisible;
        if (colonneIA != null) {
            colonneIA.setVisible(panelIAVisible);
            colonneIA.setManaged(panelIAVisible);
        }
        if (btnToggleIA != null) {
            btnToggleIA.setText(panelIAVisible ? "🤖 Masquer IA" : "🤖 Afficher IA");
        }
    }

    // ════════════════════════════════════════════════════════
    //  ACTIONS IA
    // ════════════════════════════════════════════════════════

    @FXML
    private void handleActualiserIA() {
        if (panelIA != null)
            panelIA.analyserTous(new ArrayList<>(tousLesMateriels));
    }

    // ── FIX : affiche uniquement les matériels en retard/urgents ──
    @FXML
    private void handleTopRisque() {
        if (panelIA != null) {
            // S'assurer que le panel est visible
            if (!panelIAVisible) handleToggleIA();
            panelIA.afficherTopRisque();
        }
    }

    // ════════════════════════════════════════════════════════
    //  CHARGEMENT MATÉRIELS
    // ════════════════════════════════════════════════════════

    public void chargerMesMateriels() {
        int userId = UserSession.getInstance().getUser().getId();
        tousLesMateriels = FXCollections.observableArrayList(
                materielService.getMaterielsByUserId(userId));
        appliquerFiltres();

        if (tousLesMateriels.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Information"); a.setHeaderText(null);
            a.setContentText("Vous n'avez aucun matériel.\nCliquez sur 'Ajouter' pour en créer un.");
            a.show();
        }

        // Analyser après chargement réel
        if (panelIA != null) {
            Platform.runLater(() -> panelIA.analyserTous(new ArrayList<>(tousLesMateriels)));
        }
    }

    private void appliquerFiltres() {
        List<Materiel> filtered = new ArrayList<>(tousLesMateriels);
        String t = cmbFilterType.getValue();
        if (t != null && !t.equals("Tous les types"))
            filtered = filtered.stream().filter(m -> m.getType().equals(t)).collect(Collectors.toList());
        String e = cmbFilterEtat.getValue();
        if (e != null && !e.equals("Tous les états"))
            filtered = filtered.stream().filter(m -> m.getEtat().equals(e)).collect(Collectors.toList());
        if (txtRecherche != null && !txtRecherche.getText().trim().isEmpty()) {
            String r = txtRecherche.getText().trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(m -> m.getNom().toLowerCase().contains(r)
                            || m.getType().toLowerCase().contains(r)
                            || m.getEtat().toLowerCase().contains(r))
                    .collect(Collectors.toList());
        }
        tableMateriels.setItems(FXCollections.observableArrayList(filtered));
        lblCount.setText(filtered.size() + " Matériel" + (filtered.size() > 1 ? "s" : ""));
    }

    // ════════════════════════════════════════════════════════
    //  PANEL NOTIFICATIONS
    // ════════════════════════════════════════════════════════

    @FXML private void togglePanelNotifications() {
        panelNotifVisible = !panelNotifVisible;
        panelNotifications.setVisible(panelNotifVisible);
        panelNotifications.setManaged(panelNotifVisible);
        if (panelNotifVisible) chargerNotifications();
    }

    @FXML private void fermerPanelNotifications() {
        panelNotifVisible = false;
        panelNotifications.setVisible(false);
        panelNotifications.setManaged(false);
    }

    @FXML private void toutMarquerLu() {
        int userId = UserSession.getInstance().getUser().getId();
        notificationService.marquerToutesCommeLues(userId);
        chargerNotifications(); rafraichirBadge();
    }

    private void chargerNotifications() {
        listeNotifications.getChildren().clear();
        int userId = UserSession.getInstance().getUser().getId();
        List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
        if (notifications.isEmpty()) {
            vboxAucune.setVisible(true); vboxAucune.setManaged(true); return;
        }
        vboxAucune.setVisible(false); vboxAucune.setManaged(false);
        for (Notification notif : notifications)
            listeNotifications.getChildren().add(creerItemNotification(notif));
        rafraichirBadge();
    }

    private void rafraichirBadge() {
        int userId = UserSession.getInstance().getUser().getId();
        int count = notificationService.getNombreNonLues(userId);
        Platform.runLater(() -> {
            if (count > 0) {
                lblBadgeNotif.setVisible(true); lblBadgeNotif.setManaged(true);
                lblBadgeNotif.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                lblBadgeNotif.setVisible(false); lblBadgeNotif.setManaged(false);
            }
        });
    }

    private HBox creerItemNotification(Notification notif) {
        HBox item = new HBox(12);
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle(getStyleNotif(notif));

        String couleur = "#718096";
        if (notif.getNiveauUrgence() != null) switch (notif.getNiveauUrgence()) {
            case URGENT:        couleur = "#e53e3e"; break;
            case CETTE_SEMAINE: couleur = "#dd6b20"; break;
            case CE_MOIS:       couleur = "#3182ce"; break;
            case BIENTOT:       couleur = "#805ad5"; break;
        }

        Label icone = new Label(notif.getEmoji());
        icone.setAlignment(Pos.CENTER);
        icone.setStyle("-fx-font-size: 14px; -fx-text-fill: " + couleur + ";" +
                "-fx-background-color: " + couleur + "22; -fx-border-color: " + couleur + "66;" +
                "-fx-border-width: 1.5; -fx-border-radius: 18; -fx-background-radius: 18;" +
                "-fx-min-width: 36; -fx-min-height: 36; -fx-max-width: 36; -fx-max-height: 36; -fx-alignment: center;");

        VBox contenu = new VBox(3); HBox.setHgrow(contenu, Priority.ALWAYS);
        Label titre = new Label(notif.getTitre());
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3748;");
        titre.setWrapText(true); titre.setMaxWidth(270);
        Label message = new Label(notif.getMessage());
        message.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a5568;");
        message.setWrapText(true); message.setMaxWidth(270);
        Label date = new Label(notif.getCreatedAt() != null
                ? notif.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "");
        date.setStyle("-fx-font-size: 10px; -fx-text-fill: #a0aec0;");
        contenu.getChildren().addAll(titre, message, date);

        Label pointNonLu = new Label("●");
        pointNonLu.setStyle("-fx-text-fill: #3182ce; -fx-font-size: 9px;");
        pointNonLu.setVisible(!notif.isLu()); pointNonLu.setManaged(!notif.isLu());

        item.getChildren().addAll(icone, contenu, pointNonLu);
        item.setOnMouseClicked(e -> {
            if (!notif.isLu()) {
                notificationService.marquerCommeLue(notif.getId()); notif.setLu(true);
                item.setStyle(getStyleNotif(notif));
                pointNonLu.setVisible(false); pointNonLu.setManaged(false);
                rafraichirBadge();
            }
        });
        item.setOnMouseEntered(e -> item.setStyle(getStyleNotif(notif) + "-fx-background-color: #f0f4f8;"));
        item.setOnMouseExited(e -> item.setStyle(getStyleNotif(notif)));
        return item;
    }

    private String getStyleNotif(Notification notif) {
        String bordure = "#718096";
        if (notif.getNiveauUrgence() != null) switch (notif.getNiveauUrgence()) {
            case URGENT:        bordure = "#e53e3e"; break;
            case CETTE_SEMAINE: bordure = "#dd6b20"; break;
            case CE_MOIS:       bordure = "#3182ce"; break;
            case BIENTOT:       bordure = "#805ad5"; break;
        }
        return "-fx-background-color: " + (notif.isLu() ? "#ffffff" : "#ebf8ff") + ";" +
                "-fx-border-color: " + bordure + " transparent transparent transparent;" +
                "-fx-border-width: 0 0 1 4; -fx-cursor: hand;";
    }

    private void demarrerPollingBadge() {
        pollingScheduler = Executors.newSingleThreadScheduledExecutor();
        pollingScheduler.scheduleAtFixedRate(this::rafraichirBadge, 10, 30, TimeUnit.SECONDS);
    }

    // ════════════════════════════════════════════════════════
    //  MÉTHODES FXML INCHANGÉES
    // ════════════════════════════════════════════════════════

    private void initialiserFiltres() {
        ObservableList<String> types = FXCollections.observableArrayList(
                "Tous les types","Tracteur","Moissonneuse","Semoir","Pulvérisateur","Charrue","Herse","Autre");
        cmbFilterType.setItems(types); cmbFilterType.setValue("Tous les types");
        cmbFilterType.setOnAction(e -> appliquerFiltres());
        ObservableList<String> etats = FXCollections.observableArrayList(
                "Tous les états","Neuf","Bon","Moyen","En panne","En maintenance");
        cmbFilterEtat.setItems(etats); cmbFilterEtat.setValue("Tous les états");
        cmbFilterEtat.setOnAction(e -> appliquerFiltres());
    }

    @FXML private void handleRechercher(ActionEvent event) { appliquerFiltres(); }

    @FXML private void handleResetFilters(ActionEvent event) {
        cmbFilterType.setValue("Tous les types"); cmbFilterEtat.setValue("Tous les états");
        if (txtRecherche != null) txtRecherche.clear(); appliquerFiltres();
    }

    @FXML private void handleAjouterMateriel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MaterielEtMaintenance/form-materiel.fxml"));
            Parent root = loader.load(); FormMaterielController c = loader.getController(); c.setParentController(this);
            Stage s = new Stage(); s.setScene(new Scene(root)); s.setTitle("Ajouter un Matériel"); s.show();
        } catch (IOException ex) { showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage()); }
    }

    @FXML private void handleModifierMateriel(ActionEvent event) {
        Materiel sel = tableMateriels.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Sélectionnez un matériel."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MaterielEtMaintenance/form-materiel.fxml"));
            Parent root = loader.load(); FormMaterielController c = loader.getController(); c.setMateriel(sel, this);
            Stage s = new Stage(); s.setScene(new Scene(root)); s.setTitle("Modifier - " + sel.getNom()); s.show();
        } catch (IOException ex) { showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage()); }
    }

    @FXML private void handleSupprimerMateriel(ActionEvent event) {
        Materiel sel = tableMateriels.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Sélectionnez un matériel."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation"); confirm.setContentText("Supprimer '" + sel.getNom() + "' ? Action irréversible.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (materielService.supprimerMateriel(sel.getId_materiel()))
            { showAlert(Alert.AlertType.INFORMATION, "Succès", "Matériel supprimé."); chargerMesMateriels(); }
            else showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer.");
        }
    }

    @FXML private void handleVoirDetails(ActionEvent event) {
        Materiel sel = tableMateriels.getSelectionModel().getSelectedItem();
        if (sel != null) ouvrirDetailsMateriel(sel);
        else showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Sélectionnez un matériel.");
    }

    private void ouvrirDetailsMateriel(Materiel materiel) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MaterielEtMaintenance/detail-materiel.fxml"));
            Parent root = loader.load(); DetailMaterielController c = loader.getController(); c.setMateriel(materiel);
            Stage s = new Stage(); s.setScene(new Scene(root, 700, 600)); s.setTitle("Détails - " + materiel.getNom()); s.show();
        } catch (IOException ex) { showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage()); }
    }

    private void ouvrirCalendrierMaintenance(Materiel materiel) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MaterielEtMaintenance/calendrier-maintenance.fxml"));
            Parent root = loader.load(); CalendrierMaintenanceController c = loader.getController(); c.setMateriel(materiel);
            Stage s = new Stage(); s.setScene(new Scene(root)); s.setTitle("Planifier - " + materiel.getNom());
            s.initModality(Modality.APPLICATION_MODAL); s.showAndWait();
            chargerMesMateriels(); verifierNotificationsMaintenance();
            int userId = UserSession.getInstance().getUser().getId();
            NotificationScheduler.getInstance().scannerMaintenant(userId);
            new Thread(() -> { try { Thread.sleep(2000); } catch (InterruptedException ignored) {} rafraichirBadge(); }).start();
        } catch (IOException ex) { showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage()); }
    }

    @FXML private void handlePlanifierMaintenance(ActionEvent event) {
        Materiel sel = tableMateriels.getSelectionModel().getSelectedItem();
        if (sel != null) ouvrirCalendrierMaintenance(sel);
        else showAlert(Alert.AlertType.WARNING, "Aucune sélection", "Sélectionnez un matériel.");
    }

    public void verifierNotificationsMaintenance() {
        int userId = UserSession.getInstance().getUser().getId();
        List<Materiel> urgents = MaintenanceAlertUtils.filterMaterielsNeedingMaintenance(
                materielService.getMaterielsByUserId(userId));
        if (!urgents.isEmpty()) {
            notificationBox.setVisible(true); notificationBox.setManaged(true);
            lblNotifications.setText("🔔 " + urgents.size() + " maintenance" + (urgents.size() > 1 ? "s urgentes" : " urgente"));
        } else {
            notificationBox.setVisible(false); notificationBox.setManaged(false);
        }
    }

    @FXML private void handleRafraichir(ActionEvent event) {
        chargerMesMateriels(); verifierNotificationsMaintenance();
        int userId = UserSession.getInstance().getUser().getId();
        NotificationScheduler.getInstance().scannerMaintenant(userId);
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            rafraichirBadge();
            if (panelNotifVisible) Platform.runLater(this::chargerNotifications);
        }).start();
        showAlert(Alert.AlertType.INFORMATION, "Rafraîchissement", "Données actualisées.");
    }

    @FXML private void handleRetourDashboard(ActionEvent event) {
        try {
            if (pollingScheduler != null) pollingScheduler.shutdown();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/MaterielEtMaintenance/MaterielsEtMaintenance.fxml"));
            Stage stage = (Stage) tableMateriels.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
