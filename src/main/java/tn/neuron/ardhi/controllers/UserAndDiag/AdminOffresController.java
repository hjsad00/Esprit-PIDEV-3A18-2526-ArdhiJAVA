package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.Offre;
import tn.neuron.ardhi.services.UserAndDiag.OffreService;
import tn.neuron.ardhi.utils.UserAndDiag.ValidationUtils;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur admin pour la gestion des offres d'abonnement.
 */
public class AdminOffresController implements Initializable {

    @FXML
    private TableView<Offre> tableOffres;
    @FXML
    private TableColumn<Offre, Integer> colId;
    @FXML
    private TableColumn<Offre, String> colNom;
    @FXML
    private TableColumn<Offre, String> colDescription;
    @FXML
    private TableColumn<Offre, Float> colPrix;
    @FXML
    private TableColumn<Offre, Boolean> colActif;
    @FXML
    private TableColumn<Offre, Boolean> colRecommande;

    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfDescription;
    @FXML
    private TextField tfPrix;
    @FXML
    private TextArea taAvantages;
    @FXML
    private TextField tfCouleurPrimaire;
    @FXML
    private TextField tfCouleurSecondaire;
    @FXML
    private CheckBox cbActif;
    @FXML
    private CheckBox cbRecommande;
    @FXML
    private TextField tfDiagnosticsParHeure;
    @FXML
    private CheckBox cbAccesTraitement;
    @FXML
    private CheckBox cbAccesPlanTraitement;

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

    private OffreService os = new OffreService();
    private ObservableList<Offre> listeOffres = FXCollections.observableArrayList();
    private ObservableList<Offre> listeFiltree = FXCollections.observableArrayList();

    // Filter fields
    @FXML
    private TextField tfFiltreId;
    @FXML
    private TextField tfFiltreNom;
    @FXML
    private javafx.scene.control.Slider sliderPrix;
    @FXML
    private Label lblPrixMax;
    @FXML
    private Label lblInfo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prixMensuel"));
        colActif.setCellValueFactory(new PropertyValueFactory<>("estActive"));
        colRecommande.setCellValueFactory(new PropertyValueFactory<>("estRecommandee"));

        // Custom cell factory for boolean columns
        colActif.setCellFactory(col -> new TableCell<Offre, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item ? "✓ Oui" : "✗ Non");
                    setStyle(item ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
                }
            }
        });

        colRecommande.setCellFactory(col -> new TableCell<Offre, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item ? "⭐ Oui" : "Non");
                    setStyle(item ? "-fx-text-fill: #f39c12; -fx-font-weight: bold;" : "");
                }
            }
        });

        // Load data
        chargerDonnees();

        // Selection listener - MODE SWITCHING
        tableOffres.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // --- MODE MODIFICATION ---
                lblTitreFormulaire.setText("Modification de l'offre #" + newVal.getId());
                remplirFormulaire(newVal);

                btnAjouter.setVisible(false);
                btnModifier.setVisible(true);
                btnSupprimer.setVisible(true);
            } else {
                // --- MODE AJOUT ---
                lblTitreFormulaire.setText("Ajouter une nouvelle offre");
                viderFormulaire();

                btnAjouter.setVisible(true);
                btnModifier.setVisible(false);
                btnSupprimer.setVisible(false);
            }
        });

        // Bind managed property to visible property so they don't take space when
        // hidden
        btnAjouter.managedProperty().bind(btnAjouter.visibleProperty());
        btnModifier.managedProperty().bind(btnModifier.visibleProperty());
        btnSupprimer.managedProperty().bind(btnSupprimer.visibleProperty());

        tableOffres.getSelectionModel().clearSelection();
        btnAjouter.setVisible(true);
        btnModifier.setVisible(false);
        btnSupprimer.setVisible(false);

        // Setup filter listeners
        setupFiltreListeners();

        // Setup table deselection logic
        WindowUtils.setupTableDeselection(tableOffres, mainContainer);

    }

    private void setupFiltreListeners() {
        // ID filter
        tfFiltreId.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltre());
        // Name filter
        tfFiltreNom.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltre());
        // Price slider
        sliderPrix.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (lblPrixMax != null) {
                lblPrixMax.setText(String.format("%.0f DT", newVal.doubleValue()));
            }
            appliquerFiltre();
        });
    }

    // Correct implementation follows

    private void appliquerFiltre() {
        String filtreId = tfFiltreId.getText().trim();
        String filtreNom = tfFiltreNom.getText().trim();
        double prixMax = sliderPrix.getValue();

        try {
            List<Offre> candidats;
            if (!filtreNom.isEmpty()) {
                candidats = os.rechercher(filtreNom);
            } else {
                // Use the cached full list to avoid DB hit on just slider move,
                // OR fetch fresh if we want consistency.
                // Let's use the cached 'listeOffres' which is populated by 'recuperer()'
                candidats = new ArrayList<>(listeOffres);
            }

            listeFiltree.clear();
            for (Offre o : candidats) {
                boolean matchId = filtreId.isEmpty() || String.valueOf(o.getId()).contains(filtreId);
                boolean matchPrix = o.getPrixMensuel() <= prixMax;

                if (matchId && matchPrix) {
                    listeFiltree.add(o);
                }
            }
            tableOffres.setItems(listeFiltree);
            mettreAJourInfo();

        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de la recherche : " + e.getMessage());
        }
    }

    @FXML
    void resetFiltre(ActionEvent event) {
        tfFiltreId.clear();
        tfFiltreNom.clear();
        sliderPrix.setValue(sliderPrix.getMax());

        tableOffres.setItems(listeOffres);
        mettreAJourInfo();
    }

    private void chargerDonnees() {
        try {
            listeOffres.clear();
            listeOffres.addAll(os.recuperer());
            tableOffres.setItems(listeOffres);
            mettreAJourInfo();
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les offres: " + e.getMessage());
        }
    }

    private void remplirFormulaire(Offre o) {
        tfNom.setText(o.getNom());
        tfDescription.setText(o.getDescription());
        tfPrix.setText(String.valueOf(o.getPrixMensuel()));
        // Convertir les avantages séparés par | en lignes
        String avantages = o.getAvantages();
        if (avantages != null) {
            taAvantages.setText(avantages.replace("|", "\n"));
        } else {
            taAvantages.setText("");
        }
        tfCouleurPrimaire.setText(o.getCouleurPrimaire());
        tfCouleurSecondaire.setText(o.getCouleurSecondaire());
        cbActif.setSelected(o.isEstActive());
        cbRecommande.setSelected(o.isEstRecommandee());
        // New subscription limit fields
        tfDiagnosticsParHeure.setText(String.valueOf(o.getDiagnosticsParHeure()));
        cbAccesTraitement.setSelected(o.isAccesTraitement());
        cbAccesPlanTraitement.setSelected(o.isAccesPlanTraitement());
    }

    private void viderFormulaire() {
        tfNom.clear();
        tfDescription.clear();
        tfPrix.clear();
        taAvantages.clear();
        tfCouleurPrimaire.setText("#6B7F3F");
        tfCouleurSecondaire.setText("#4A5A2B");
        cbActif.setSelected(true);
        cbRecommande.setSelected(false);
        // Reset subscription limit fields
        tfDiagnosticsParHeure.setText("3");
        cbAccesTraitement.setSelected(false);
        cbAccesPlanTraitement.setSelected(false);
    }

    // --- VALIDATION ---
    private boolean validerFormulaire() {
        if (tfNom.getText().trim().isEmpty()) {
            WindowUtils.showAlert("Erreur", "Le nom de l'offre est obligatoire.");
            tfNom.requestFocus();
            return false;
        }
        try {
            float prix = Float.parseFloat(tfPrix.getText());
            if (prix <= 0) {
                WindowUtils.showAlert("Erreur", "Le prix doit être supérieur à 0.");
                tfPrix.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "Le prix doit être un nombre valide.");
            tfPrix.requestFocus();
            return false;
        }

        // Validation des couleurs hexadécimales
        String couleurPrimaire = tfCouleurPrimaire.getText().trim();
        String couleurSecondaire = tfCouleurSecondaire.getText().trim();

        if (!ValidationUtils.validerCouleurHex(couleurPrimaire)) {
            WindowUtils.showAlert("Erreur", "Couleur primaire invalide.\nFormat attendu: #RRGGBB (ex: #6B7F3F)");
            tfCouleurPrimaire.requestFocus();
            return false;
        }

        if (!ValidationUtils.validerCouleurHex(couleurSecondaire)) {
            WindowUtils.showAlert("Erreur", "Couleur secondaire invalide.\nFormat attendu: #RRGGBB (ex: #4A5A2B)");
            tfCouleurSecondaire.requestFocus();
            return false;
        }

        return true;
    }

    // --- CRUD ACTIONS ---
    @FXML
    void ajouter(ActionEvent event) {
        if (!validerFormulaire())
            return;

        try {
            Offre o = new Offre(
                    tfNom.getText().trim(),
                    tfDescription.getText().trim(),
                    Float.parseFloat(tfPrix.getText()),
                    taAvantages.getText().trim().replace("\n", "|"),
                    tfCouleurPrimaire.getText().trim(),
                    tfCouleurSecondaire.getText().trim(),
                    cbActif.isSelected(),
                    cbRecommande.isSelected());
            // Set subscription limit fields
            int diagPerHour = 3;
            try {
                diagPerHour = Integer.parseInt(tfDiagnosticsParHeure.getText().trim());
            } catch (NumberFormatException ignored) {
            }
            o.setDiagnosticsParHeure(diagPerHour);
            o.setAccesTraitement(cbAccesTraitement.isSelected());
            o.setAccesPlanTraitement(cbAccesPlanTraitement.isSelected());

            os.ajouter(o);
            chargerDonnees();
            vider(event);
            WindowUtils.showAlert("Succès", "Offre ajoutée !");
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    @FXML
    void modifier(ActionEvent event) {
        Offre selected = tableOffres.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Veuillez sélectionner une offre.");
            return;
        }
        if (!validerFormulaire())
            return;

        try {
            selected.setNom(tfNom.getText().trim());
            selected.setDescription(tfDescription.getText().trim());
            selected.setPrixMensuel(Float.parseFloat(tfPrix.getText()));
            selected.setAvantages(taAvantages.getText().trim().replace("\n", "|"));
            selected.setCouleurPrimaire(tfCouleurPrimaire.getText().trim());
            selected.setCouleurSecondaire(tfCouleurSecondaire.getText().trim());
            selected.setEstActive(cbActif.isSelected());
            selected.setEstRecommandee(cbRecommande.isSelected());
            // Set subscription limit fields
            int diagPerHour = 3;
            try {
                diagPerHour = Integer.parseInt(tfDiagnosticsParHeure.getText().trim());
            } catch (NumberFormatException ignored) {
            }
            selected.setDiagnosticsParHeure(diagPerHour);
            selected.setAccesTraitement(cbAccesTraitement.isSelected());
            selected.setAccesPlanTraitement(cbAccesPlanTraitement.isSelected());

            os.modifier(selected);
            chargerDonnees();
            vider(event);
            WindowUtils.showAlert("Succès", "Offre modifiée !");
        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage());
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        Offre selected = tableOffres.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Veuillez sélectionner une offre.");
            return;
        }

        if (WindowUtils.showConfirmation("Confirmation",
                "Supprimer cette offre ? Cette action est irréversible. Les abonnements existants utilisant cette offre ne seront pas supprimés.")) {
            try {
                os.supprimer(selected.getId());
                chargerDonnees();
                vider(event);
                WindowUtils.showAlert("Succès", "Offre supprimée !");
            } catch (Exception e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    @FXML
    void vider(ActionEvent event) {
        viderFormulaire();
        tableOffres.getSelectionModel().clearSelection();
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }

    private void mettreAJourInfo() {
        WindowUtils.updateInfoLabel(lblInfo, tableOffres.getItems().size(), "offre");
    }

}
