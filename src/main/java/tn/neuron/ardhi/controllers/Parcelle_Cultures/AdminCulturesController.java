package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.Parcelle_Cultures.Culture;
import tn.neuron.ardhi.services.Parcelle_Cultures.CultureService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class AdminCulturesController implements Initializable {

    @FXML
    private TableView<Culture> tableCultures;
    @FXML
    private TableColumn<Culture, Integer> colId;
    @FXML
    private TableColumn<Culture, String> colNomCulture;
    @FXML
    private TableColumn<Culture, String> colTypeCulture;
    @FXML
    private TableColumn<Culture, String> colSaison;
    @FXML
    private TableColumn<Culture, String> colEtatCulture;
    @FXML
    private TableColumn<Culture, String> colDatePlantation;
    @FXML
    private TableColumn<Culture, String> colDateRecolte;
    @FXML
    private TableColumn<Culture, String> colParcelle;
    @FXML
    private TableColumn<Culture, String> colAgriculteur;

    @FXML
    private TextField tfRecherche;
    @FXML
    private ComboBox<String> cbFilterTypeCulture;
    @FXML
    private ComboBox<String> cbFilterSaison;
    @FXML
    private ComboBox<String> cbFilterEtat;
    @FXML
    private Label lblInfo;
    @FXML
    private VBox mainContainer;

    private CultureService cs = new CultureService();

    private List<Culture> allCultures = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Configuration des colonnes
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colNomCulture.setCellValueFactory(new PropertyValueFactory<>("nomCulture"));
            colTypeCulture.setCellValueFactory(new PropertyValueFactory<>("typeCulture"));
            colSaison.setCellValueFactory(new PropertyValueFactory<>("saison"));
            colEtatCulture.setCellValueFactory(new PropertyValueFactory<>("etatCulture"));

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

            colDatePlantation.setCellValueFactory(cellData -> {
                if (cellData.getValue().getDatePlantation() != null) {
                    return new SimpleStringProperty(dateFormat.format(cellData.getValue().getDatePlantation()));
                } else {
                    return new SimpleStringProperty("");
                }
            });

            colDateRecolte.setCellValueFactory(cellData -> {
                if (cellData.getValue().getDateRecoltePrevue() != null) {
                    return new SimpleStringProperty(dateFormat.format(cellData.getValue().getDateRecoltePrevue()));
                } else {
                    return new SimpleStringProperty("");
                }
            });

            colParcelle.setCellValueFactory(cellData -> {
                String nomParcelle = cellData.getValue().getNomParcelleTemp();
                return new SimpleStringProperty(nomParcelle != null ? nomParcelle : "N/A");
            });

            colAgriculteur.setCellValueFactory(cellData -> {
                String nomAgriculteur = cellData.getValue().getNomAgriculteurTemp();
                return new SimpleStringProperty(nomAgriculteur != null ? nomAgriculteur : "N/A");
            });

            chargerCultures();

            // Initialiser les filtres
            initialiserFiltres();

            // Écouteurs de recherche et filtrage
            if (tfRecherche != null) {
                tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> appliquerFiltres());
            }
            if (cbFilterTypeCulture != null) {
                cbFilterTypeCulture.setOnAction(e -> appliquerFiltres());
            }
            if (cbFilterSaison != null) {
                cbFilterSaison.setOnAction(e -> appliquerFiltres());
            }
            if (cbFilterEtat != null) {
                cbFilterEtat.setOnAction(e -> appliquerFiltres());
            }

        } catch (Exception e) {
            WindowUtils.showAlert("Erreur Fatale", "Impossible d'initialiser la vue cultures : " + e.getMessage());
        }
    }

    private void chargerCultures() {
        try {
            allCultures = cs.recuperer();
            tableCultures.setItems(FXCollections.observableArrayList(allCultures));
            mettreAJourInfo(allCultures.size());
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les cultures : " + e.getMessage());
        }
    }

    private void rechercherCultures(String keyword) {
        appliquerFiltres();
    }

    private void initialiserFiltres() {
        java.util.Set<String> types = new java.util.TreeSet<>();
        java.util.Set<String> saisons = new java.util.TreeSet<>();
        java.util.Set<String> etats = new java.util.TreeSet<>();
        types.add("Tous");
        saisons.add("Tous");
        etats.add("Tous");
        for (Culture c : allCultures) {
            if (c.getTypeCulture() != null && !c.getTypeCulture().isBlank()) types.add(c.getTypeCulture());
            if (c.getSaison() != null && !c.getSaison().isBlank()) saisons.add(c.getSaison());
            if (c.getEtatCulture() != null && !c.getEtatCulture().isBlank()) etats.add(c.getEtatCulture());
        }
        if (cbFilterTypeCulture != null) { cbFilterTypeCulture.setItems(FXCollections.observableArrayList(types)); cbFilterTypeCulture.setValue("Tous"); }
        if (cbFilterSaison != null) { cbFilterSaison.setItems(FXCollections.observableArrayList(saisons)); cbFilterSaison.setValue("Tous"); }
        if (cbFilterEtat != null) { cbFilterEtat.setItems(FXCollections.observableArrayList(etats)); cbFilterEtat.setValue("Tous"); }
    }

    private void appliquerFiltres() {
        String keyword = (tfRecherche != null && tfRecherche.getText() != null) ? tfRecherche.getText().trim().toLowerCase() : "";
        String typeFilter = (cbFilterTypeCulture != null && cbFilterTypeCulture.getValue() != null) ? cbFilterTypeCulture.getValue() : "Tous";
        String saisonFilter = (cbFilterSaison != null && cbFilterSaison.getValue() != null) ? cbFilterSaison.getValue() : "Tous";
        String etatFilter = (cbFilterEtat != null && cbFilterEtat.getValue() != null) ? cbFilterEtat.getValue() : "Tous";

        List<Culture> filtered = allCultures.stream()
                .filter(c -> {
                    if (!keyword.isEmpty()) {
                        boolean matchText = (c.getNomCulture() != null && c.getNomCulture().toLowerCase().contains(keyword))
                                || (c.getTypeCulture() != null && c.getTypeCulture().toLowerCase().contains(keyword))
                                || (c.getNomParcelleTemp() != null && c.getNomParcelleTemp().toLowerCase().contains(keyword));
                        if (!matchText) return false;
                    }
                    if (!"Tous".equals(typeFilter) && (c.getTypeCulture() == null || !c.getTypeCulture().equals(typeFilter))) return false;
                    if (!"Tous".equals(saisonFilter) && (c.getSaison() == null || !c.getSaison().equals(saisonFilter))) return false;
                    if (!"Tous".equals(etatFilter) && (c.getEtatCulture() == null || !c.getEtatCulture().equals(etatFilter))) return false;
                    return true;
                })
                .toList();
        tableCultures.setItems(FXCollections.observableArrayList(filtered));
        mettreAJourInfo(filtered.size());
    }

    private void mettreAJourInfo(int count) {
        if (lblInfo != null) WindowUtils.updateInfoLabel(lblInfo, count, "culture");
    }

    @FXML
    void rafraichir(ActionEvent event) {
        if (tfRecherche != null) tfRecherche.clear();
        if (cbFilterTypeCulture != null) cbFilterTypeCulture.setValue("Tous");
        if (cbFilterSaison != null) cbFilterSaison.setValue("Tous");
        if (cbFilterEtat != null) cbFilterEtat.setValue("Tous");
        chargerCultures();
        initialiserFiltres();
    }

    @FXML
    void voirStatistiquesAgricoles(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AdminAgricultureStats.fxml", "Statistiques Agricoles Globales");
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AdminParcelles.fxml", "Gestion des Parcelles");
    }

    @FXML
    void retourDashboard(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
