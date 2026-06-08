package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.services.Parcelle_Cultures.ParcelleService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import java.sql.SQLException;

public class AdminParcellesController implements Initializable {

    @FXML
    private TableView<Parcelle> tableParcelles;
    @FXML
    private TableColumn<Parcelle, Integer> colId;
    @FXML
    private TableColumn<Parcelle, Double> colSurface;
    @FXML
    private TableColumn<Parcelle, String> colLocalisation;
    @FXML
    private TableColumn<Parcelle, String> colTypeSol;
    @FXML
    private TableColumn<Parcelle, String> colSystemeIrrigation;
    @FXML
    private TableColumn<Parcelle, String> colStatut;
    @FXML
    private TableColumn<Parcelle, String> colAgriculteur;

    @FXML
    private TextField tfRecherche;
    @FXML
    private ComboBox<String> cbFilterTypeSol;
    @FXML
    private ComboBox<String> cbFilterIrrigation;
    @FXML
    private ComboBox<String> cbFilterStatut;
    @FXML
    private Label lblInfo;
    @FXML
    private VBox mainContainer;

    private ParcelleService ps = new ParcelleService();

    private List<Parcelle> allParcelles = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Configuration des colonnes
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colSurface.setCellValueFactory(new PropertyValueFactory<>("surface"));
            colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
            colTypeSol.setCellValueFactory(new PropertyValueFactory<>("typeSol"));
            colSystemeIrrigation.setCellValueFactory(new PropertyValueFactory<>("systemeIrrigation"));
            colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

            colAgriculteur.setCellValueFactory(cellData -> {
                String nomAgriculteur = cellData.getValue().getNomAgriculteurTemp();
                return new SimpleStringProperty(nomAgriculteur != null ? nomAgriculteur : "N/A");
            });

            chargerParcelles();

            // Initialiser les filtres
            initialiserFiltres();

            // Écouteurs de recherche et filtrage
            if (tfRecherche != null) {
                tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> appliquerFiltres());
            }
            if (cbFilterTypeSol != null) {
                cbFilterTypeSol.setOnAction(e -> appliquerFiltres());
            }
            if (cbFilterIrrigation != null) {
                cbFilterIrrigation.setOnAction(e -> appliquerFiltres());
            }
            if (cbFilterStatut != null) {
                cbFilterStatut.setOnAction(e -> appliquerFiltres());
            }

        } catch (Exception e) {
            WindowUtils.showAlert("Erreur Fatale", "Impossible d'initialiser la vue parcelles : " + e.getMessage());
        }
    }

    private void chargerParcelles() {
        try {
            allParcelles = ps.recuperer();
            tableParcelles.setItems(FXCollections.observableArrayList(allParcelles));
            mettreAJourInfo(allParcelles.size());
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les parcelles : " + e.getMessage());
        }
    }

    private void rechercherParcelles(String keyword) {
        appliquerFiltres();
    }

    private void initialiserFiltres() {
        java.util.Set<String> typesSol = new java.util.TreeSet<>();
        java.util.Set<String> irrigations = new java.util.TreeSet<>();
        java.util.Set<String> statuts = new java.util.TreeSet<>();
        typesSol.add("Tous");
        irrigations.add("Tous");
        statuts.add("Tous");
        for (Parcelle p : allParcelles) {
            if (p.getTypeSol() != null && !p.getTypeSol().isBlank()) typesSol.add(p.getTypeSol());
            if (p.getSystemeIrrigation() != null && !p.getSystemeIrrigation().isBlank()) irrigations.add(p.getSystemeIrrigation());
            if (p.getStatut() != null && !p.getStatut().isBlank()) statuts.add(p.getStatut());
        }
        if (cbFilterTypeSol != null) { cbFilterTypeSol.setItems(FXCollections.observableArrayList(typesSol)); cbFilterTypeSol.setValue("Tous"); }
        if (cbFilterIrrigation != null) { cbFilterIrrigation.setItems(FXCollections.observableArrayList(irrigations)); cbFilterIrrigation.setValue("Tous"); }
        if (cbFilterStatut != null) { cbFilterStatut.setItems(FXCollections.observableArrayList(statuts)); cbFilterStatut.setValue("Tous"); }
    }

    private void appliquerFiltres() {
        String keyword = (tfRecherche != null && tfRecherche.getText() != null) ? tfRecherche.getText().trim().toLowerCase() : "";
        String typeSolFilter = (cbFilterTypeSol != null && cbFilterTypeSol.getValue() != null) ? cbFilterTypeSol.getValue() : "Tous";
        String irrigationFilter = (cbFilterIrrigation != null && cbFilterIrrigation.getValue() != null) ? cbFilterIrrigation.getValue() : "Tous";
        String statutFilter = (cbFilterStatut != null && cbFilterStatut.getValue() != null) ? cbFilterStatut.getValue() : "Tous";

        List<Parcelle> filtered = allParcelles.stream()
                .filter(p -> {
                    if (!keyword.isEmpty()) {
                        boolean matchText = (p.getLocalisation() != null && p.getLocalisation().toLowerCase().contains(keyword))
                                || (p.getTypeSol() != null && p.getTypeSol().toLowerCase().contains(keyword))
                                || (p.getNomAgriculteurTemp() != null && p.getNomAgriculteurTemp().toLowerCase().contains(keyword));
                        if (!matchText) return false;
                    }
                    if (!"Tous".equals(typeSolFilter) && (p.getTypeSol() == null || !p.getTypeSol().equals(typeSolFilter))) return false;
                    if (!"Tous".equals(irrigationFilter) && (p.getSystemeIrrigation() == null || !p.getSystemeIrrigation().equals(irrigationFilter))) return false;
                    if (!"Tous".equals(statutFilter) && (p.getStatut() == null || !p.getStatut().equals(statutFilter))) return false;
                    return true;
                })
                .toList();
        tableParcelles.setItems(FXCollections.observableArrayList(filtered));
        mettreAJourInfo(filtered.size());
    }

    private void mettreAJourInfo(int count) {
        WindowUtils.updateInfoLabel(lblInfo, count, "parcelle");
    }

    @FXML
    void voirCultures(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AdminCultures.fxml", "Gestion des Cultures");
    }

    @FXML
    void voirStatsAgriculture(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AdminAgricultureStats.fxml", "Statistiques Agricoles Globales");
    }

    @FXML
    void rafraichir(ActionEvent event) {
        if (tfRecherche != null) tfRecherche.clear();
        if (cbFilterTypeSol != null) cbFilterTypeSol.setValue("Tous");
        if (cbFilterIrrigation != null) cbFilterIrrigation.setValue("Tous");
        if (cbFilterStatut != null) cbFilterStatut.setValue("Tous");
        chargerParcelles();
        initialiserFiltres();
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
