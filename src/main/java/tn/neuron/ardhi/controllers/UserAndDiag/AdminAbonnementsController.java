package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import tn.neuron.ardhi.models.UserAndDiag.Abonnement;
import tn.neuron.ardhi.models.UserAndDiag.Offre;
import tn.neuron.ardhi.services.UserAndDiag.AbonnementService;
import tn.neuron.ardhi.services.UserAndDiag.OffreService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.Date;
import java.util.ResourceBundle;

public class AdminAbonnementsController implements Initializable {

    @FXML
    private TableView<Abonnement> tableAbonnements;
    @FXML
    private TableColumn<Abonnement, Integer> colId;
    @FXML
    private TableColumn<Abonnement, Integer> colUserId;
    @FXML
    private TableColumn<Abonnement, String> colType;
    @FXML
    private TableColumn<Abonnement, Float> colPrix;
    @FXML
    private TableColumn<Abonnement, Date> colDebut;
    @FXML
    private TableColumn<Abonnement, Date> colFin;
    @FXML
    private TableColumn<Abonnement, String> colStatut;

    @FXML
    private TextField tfUserId;
    @FXML
    private ComboBox<Offre> cbType;
    @FXML
    private TextField tfPrix;
    @FXML
    private DatePicker dpDebut;
    @FXML
    private DatePicker dpFin;
    @FXML
    private ComboBox<String> cbStatut;

    @FXML
    private VBox mainContainer;
    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private DatePicker dpFiltreDebut;
    @FXML
    private DatePicker dpFiltreFin;
    @FXML
    private TextField tfFiltreUserId;
    @FXML
    private TextField tfFiltreId;
    @FXML
    private ComboBox<String> cbFiltreStatut;
    @FXML
    private javafx.scene.control.Slider sliderPrix;
    @FXML
    private Label lblPrixMax;
    @FXML
    private Label lblInfo;

    private AbonnementService as = new AbonnementService();
    private OffreService os = new OffreService();
    private ObservableList<Abonnement> listeAbonnements = FXCollections.observableArrayList();
    private ObservableList<Abonnement> listeFiltree = FXCollections.observableArrayList();
    private ObservableList<Offre> listeOffres = FXCollections.observableArrayList();
    private ObservableList<Offre> toutesLesOffres = FXCollections.observableArrayList(); // All offers including
                                                                                         // inactive

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Setup ComboBox - ACTIF, ANNULE, EXPIRE
        cbStatut.setItems(FXCollections.observableArrayList("ACTIF", "ANNULE", "EXPIRE"));

        // Setup Type ComboBox - charger dynamiquement depuis la BDD
        chargerOffres();

        // Load data
        chargerDonnees();

        // Selection listener - MODE SWITCHING
        tableAbonnements.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // --- MODE MODIFICATION ---
                lblTitreFormulaire.setText("Modification de l'abonnement #" + newVal.getId());
                remplirFormulaire(newVal);

                btnAjouter.setVisible(false);
                btnModifier.setVisible(true);
                btnSupprimer.setVisible(true);
            } else {
                // --- MODE AJOUT ---
                lblTitreFormulaire.setText("Ajouter un nouvel abonnement");
                viderFormulaire();

                btnAjouter.setVisible(true);
                btnModifier.setVisible(false);
                btnSupprimer.setVisible(false);
            }
        });

        // Initial state: Add mode
        tableAbonnements.getSelectionModel().clearSelection();

        // Initial state: Add mode - hide modification buttons
        btnAjouter.setVisible(true);
        btnModifier.setVisible(false);
        btnSupprimer.setVisible(false);

        // Setup slider listener
        sliderPrix.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblPrixMax.setText(String.format("%.0f DT", newVal.doubleValue()));
            appliquerFiltreTempsReel();
        });

        // Real-time filter listeners
        tfFiltreUserId.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltreTempsReel());
        dpFiltreDebut.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltreTempsReel());
        dpFiltreFin.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltreTempsReel());
        tfFiltreId.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltreTempsReel());

        cbFiltreStatut.setItems(FXCollections.observableArrayList("Tout", "ACTIF", "ANNULE", "EXPIRE"));
        cbFiltreStatut.getSelectionModel().selectFirst();
        cbFiltreStatut.valueProperty().addListener((obs, oldVal, newVal) -> appliquerFiltreTempsReel());

        // Setup table deselection logic
        WindowUtils.setupTableDeselection(tableAbonnements, mainContainer);
    }

    /**
     * Filtre en temps réel sans afficher d'alerte.
     */
    private void appliquerFiltreTempsReel() {
        String userIdText = tfFiltreUserId.getText().trim();
        String subIdText = tfFiltreId.getText().trim();
        String statutFilter = cbFiltreStatut.getValue() != null ? cbFiltreStatut.getValue() : "Tout";

        listeFiltree.clear();
        for (Abonnement a : listeAbonnements) {
            boolean inclure = true;

            // Filter by Subscription ID
            if (!subIdText.isEmpty()) {
                if (!String.valueOf(a.getId()).contains(subIdText)) {
                    inclure = false;
                }
            }

            // Filter by Status
            if (!statutFilter.equals("Tout")) {
                if (a.getStatut() == null || !a.getStatut().equalsIgnoreCase(statutFilter)) {
                    inclure = false;
                }
            }

            // Filter by User ID
            if (!userIdText.isEmpty()) {
                try {
                    int filtreUserId = Integer.parseInt(userIdText);
                    if (a.getUserId() != filtreUserId) {
                        inclure = false;
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid input for real-time filtering
                    inclure = false;
                }
            }

            // Filter by date range
            if (dpFiltreDebut.getValue() != null && a.getDateDebut() != null) {
                if (a.getDateDebut().toLocalDate().isBefore(dpFiltreDebut.getValue())) {
                    inclure = false;
                }
            }
            if (dpFiltreFin.getValue() != null && a.getDateFin() != null) {
                if (a.getDateFin().toLocalDate().isAfter(dpFiltreFin.getValue())) {
                    inclure = false;
                }
            }

            // Filter by price (slider)
            if (a.getPrix() > sliderPrix.getValue()) {
                inclure = false;
            }

            if (inclure) {
                listeFiltree.add(a);
            }
        }

        tableAbonnements.setItems(listeFiltree);
        mettreAJourInfo();
    }

    private void chargerDonnees() {
        try {
            listeAbonnements.clear();
            listeAbonnements.addAll(as.recuperer());
            tableAbonnements.setItems(listeAbonnements);
            mettreAJourInfo();
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les abonnements: " + e.getMessage());
        }
    }

    private void chargerOffres() {
        try {
            listeOffres.clear();
            toutesLesOffres.clear();
            toutesLesOffres.addAll(os.recuperer()); // All offers for lookup and ComboBox
            listeOffres.addAll(toutesLesOffres); // Show ALL offers in ComboBox (active and inactive)
            cbType.setItems(listeOffres);
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les offres: " + e.getMessage());
        }
    }

    // Helper: trouver une offre par son nom (parmi les offres actives)
    private Offre trouverOffreParNom(String nom) {
        for (Offre o : listeOffres) {
            if (o.getNom().equals(nom)) {
                return o;
            }
        }
        return null;
    }

    private void remplirFormulaire(Abonnement a) {
        tfUserId.setText(String.valueOf(a.getUserId()));
        // Trouver l'offre correspondante au type
        Offre offreCorrespondante = trouverOffreParNom(a.getType());
        cbType.setValue(offreCorrespondante);
        tfPrix.setText(String.valueOf(a.getPrix()));
        if (a.getDateDebut() != null)
            dpDebut.setValue(a.getDateDebut().toLocalDate());
        if (a.getDateFin() != null)
            dpFin.setValue(a.getDateFin().toLocalDate());
        cbStatut.setValue(a.getStatut());
    }

    private void viderFormulaire() {
        tfUserId.clear();
        cbType.setValue(null);
        tfPrix.clear();
        dpDebut.setValue(null);
        dpFin.setValue(null);
        cbStatut.setValue(null);
    }

    // --- VALIDATION ---
    private boolean validerDates() {
        if (dpDebut.getValue() == null || dpFin.getValue() == null) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner les dates de début et de fin.");
            return false;
        }
        if (dpFin.getValue().isBefore(dpDebut.getValue())) {
            WindowUtils.showAlert("Erreur", "La date de fin ne peut pas être antérieure à la date de début.");
            return false;
        }
        return true;
    }

    private boolean validerStatutExpire() {
        if (dpFin.getValue() != null && dpFin.getValue().isBefore(java.time.LocalDate.now())) {
            if (!"EXPIRE".equals(cbStatut.getValue())) {
                WindowUtils.showAlert("Erreur", "La date de fin est passée. Le statut doit être 'EXPIRE'.");
                return false;
            }
        }
        return true;
    }

    // --- CRUD ACTIONS ---

    @FXML
    void ajouter(ActionEvent event) {
        if (!validerDates())
            return;
        if (!validerStatutExpire())
            return;
        if (cbType.getValue() == null) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner une offre.");
            return;
        }
        Offre offreSelectionnee = cbType.getValue();
        // Validate: can't add ACTIF subscription to inactive offer
        if ("ACTIF".equals(cbStatut.getValue()) && !offreSelectionnee.isEstActive()) {
            WindowUtils.showAlert("Erreur", "Impossible d'ajouter un abonnement actif \u00e0 une offre inactive.");
            return;
        }
        try {
            Abonnement a = new Abonnement(
                    offreSelectionnee.getId(),
                    offreSelectionnee.getNom(),
                    Float.parseFloat(tfPrix.getText()),
                    Date.valueOf(dpDebut.getValue()),
                    Date.valueOf(dpFin.getValue()),
                    cbStatut.getValue(),
                    Integer.parseInt(tfUserId.getText()));
            as.ajouter(a);
            chargerDonnees();
            vider(event);
            WindowUtils.showAlert("Succès", "Abonnement ajouté !");
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    @FXML
    void modifier(ActionEvent event) {
        Abonnement selected = tableAbonnements.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Veuillez sélectionner un abonnement.");
            return;
        }
        if (!validerDates())
            return;
        if (!validerStatutExpire())
            return;
        if (cbType.getValue() == null) {
            WindowUtils.showAlert("Erreur", "Veuillez sélectionner une offre.");
            return;
        }
        Offre offreSelectionnee = cbType.getValue();
        // Validate: can't set ACTIF status for subscription to inactive offer
        if ("ACTIF".equals(cbStatut.getValue()) && !offreSelectionnee.isEstActive()) {
            WindowUtils.showAlert("Erreur",
                    "Impossible de rendre actif un abonnement lié à une offre inactive ('" + offreSelectionnee.getNom()
                            + "').");
            return;
        }

        try {
            selected.setUserId(Integer.parseInt(tfUserId.getText()));
            selected.setOffreId(offreSelectionnee.getId());
            selected.setType(offreSelectionnee.getNom());
            selected.setPrix(Float.parseFloat(tfPrix.getText()));
            selected.setDateDebut(Date.valueOf(dpDebut.getValue()));
            selected.setDateFin(Date.valueOf(dpFin.getValue()));
            selected.setStatut(cbStatut.getValue());

            as.modifier(selected);
            chargerDonnees();
            vider(event);
            WindowUtils.showAlert("Succès", "Abonnement modifié !");
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        Abonnement selected = tableAbonnements.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Veuillez sélectionner un abonnement.");
            return;
        }

        if (WindowUtils.showConfirmation("Supprimer cet abonnement ?", "Cette action est irréversible.")) {
            try {
                as.supprimer(selected.getId());
                chargerDonnees();
                vider(event);
                WindowUtils.showAlert("Succès", "Abonnement supprimé !");
            } catch (Exception e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    @FXML
    void vider(ActionEvent event) {
        viderFormulaire();
        tableAbonnements.getSelectionModel().clearSelection();
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }

    @FXML
    void resetFiltre(ActionEvent event) {
        tfFiltreUserId.clear();
        tfFiltreId.clear();
        dpFiltreDebut.setValue(null);
        dpFiltreFin.setValue(null);
        cbFiltreStatut.getSelectionModel().selectFirst();
        sliderPrix.setValue(sliderPrix.getMax());
        tableAbonnements.setItems(listeAbonnements);
        mettreAJourInfo();
    }

    @FXML
    void voirStatistiques(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AbonnementsStatistiques.fxml", "Statistiques Abonnements");
    }

    private void mettreAJourInfo() {
        WindowUtils.updateInfoLabel(lblInfo, tableAbonnements.getItems().size(), "abonnement");
    }

}
