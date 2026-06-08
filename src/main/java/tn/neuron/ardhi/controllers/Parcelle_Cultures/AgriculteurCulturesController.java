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
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.Parcelle_Cultures.CultureService;
import tn.neuron.ardhi.services.Parcelle_Cultures.ParcelleService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import java.text.SimpleDateFormat;
import java.sql.Date;
import java.sql.SQLException;

public class AgriculteurCulturesController implements Initializable {

    @FXML
    private TableView<Culture> tableCultures;
    @FXML
    private TableColumn<Culture, Integer> colId;
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
    private TextField tfTypeCulture;
    @FXML
    private ComboBox<String> cbSaison;
    @FXML
    private ComboBox<String> cbEtatCulture;
    @FXML
    private Label lblIndicationEtat;
    @FXML
    private TextField tfSurfaceUtilisee;
    @FXML
    private TextField tfRendementEstime;

    @FXML
    private DatePicker dpDatePlantation;
    @FXML
    private DatePicker dpDateRecolte;
    @FXML
    private ComboBox<Parcelle> cbParcelle;
    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private VBox mainContainer;
    @FXML
    private VBox vboxIndicationParcelle;
    @FXML
    private Label lblIndicationParcelle;
    @FXML
    private Label lblIndicationDetail;
    @FXML
    private Label lblIndicationSurface;
    @FXML
    private Label lblIndicationNbCultures;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private CultureService cs = new CultureService();
    private ParcelleService ps = new ParcelleService();
    private User currentUser;
    private List<Culture> mesCultures = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) {
            WindowUtils.showAlert("Erreur", "Session expirée. Veuillez vous reconnecter.");
            return;
        }

        try {
            // Configuration des colonnes
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
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

            // Charger les parcelles dans le ComboBox
            chargerParcelles();
            chargerMesCultures();
            updateButtonState(false);

            // Listener sur la sélection de parcelle pour afficher les indications
            cbParcelle.setOnAction(e -> mettreAJourIndicationParcelle());

            // === WORKFLOW AMÉLIORÉ ===

            // Date de plantation par défaut = aujourd'hui
            dpDatePlantation.setValue(LocalDate.now());

            // Auto-détection de la saison quand la date de plantation change
            dpDatePlantation.valueProperty().addListener((obs, oldDate, newDate) -> {
                if (newDate != null) {
                    String saison = detecterSaison(newDate);
                    if (cbSaison != null) {
                        cbSaison.setValue(saison);
                    }
                    // Bloquer les dates de récolte antérieures à la plantation
                    configurerDateRecolteMin(newDate);
                }
            });

            // Déclencher l'auto-détection pour la date par défaut
            if (dpDatePlantation.getValue() != null) {
                String saisonInit = detecterSaison(dpDatePlantation.getValue());
                if (cbSaison != null) cbSaison.setValue(saisonInit);
                configurerDateRecolteMin(dpDatePlantation.getValue());
            }

            // État : en mode ajout, forcer "en_croissance" et désactiver le choix
            if (cbEtatCulture != null) {
                cbEtatCulture.setValue("en_croissance");
                cbEtatCulture.setDisable(true);
            }
            if (lblIndicationEtat != null) {
                lblIndicationEtat.setText("✅ Une nouvelle culture est automatiquement \"en croissance\"");
                lblIndicationEtat.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 9; -fx-font-style: italic;");
            }

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

            // Écouteur de sélection
            tableCultures.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldSelection, newSelection) -> {

                        if (newSelection != null) {
                            lblTitreFormulaire.setText("Modifier: " + newSelection.getTypeCulture());
                            if (tfTypeCulture != null)
                                tfTypeCulture.setText(newSelection.getTypeCulture());
                            if (cbSaison != null)
                                cbSaison.setValue(newSelection.getSaison());
                            if (cbEtatCulture != null) {
                                cbEtatCulture.setValue(newSelection.getEtatCulture());
                                cbEtatCulture.setDisable(false); // Permettre de changer l'état en modification
                            }
                            if (lblIndicationEtat != null) {
                                lblIndicationEtat.setText("💡 Vous pouvez changer l'état en modification (ex: marquer comme récoltée)");
                                lblIndicationEtat.setStyle("-fx-text-fill: #E65100; -fx-font-size: 9; -fx-font-style: italic;");
                            }

                            if (newSelection.getDatePlantation() != null) {
                                dpDatePlantation.setValue(newSelection.getDatePlantation().toLocalDate());
                            }
                            if (newSelection.getDateRecoltePrevue() != null) {
                                dpDateRecolte.setValue(newSelection.getDateRecoltePrevue().toLocalDate());
                            }

                            // Sélectionner la parcelle dans le ComboBox
                            cbParcelle.getItems().stream()
                                    .filter(p -> p.getId() == newSelection.getParcelleId())
                                    .findFirst()
                                    .ifPresent(p -> {
                                        cbParcelle.setValue(p);
                                        mettreAJourIndicationParcelle();
                                    });
                            cbParcelle.setDisable(true);

                            tfSurfaceUtilisee.setText(String.valueOf(newSelection.getSurfaceUtilisee()));
                            tfRendementEstime.setText(String.valueOf(newSelection.getRendementEstime()));

                            updateButtonState(true);
                        } else {
                            lblTitreFormulaire.setText("Ajouter une culture");
                            viderChamps();
                            cbParcelle.setDisable(false);
                            // Revenir en mode ajout : forcer en_croissance
                            if (cbEtatCulture != null) {
                                cbEtatCulture.setValue("en_croissance");
                                cbEtatCulture.setDisable(true);
                            }
                            if (lblIndicationEtat != null) {
                                lblIndicationEtat.setText("✅ Une nouvelle culture est automatiquement \"en croissance\"");
                                lblIndicationEtat.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 9; -fx-font-style: italic;");
                            }
                            updateButtonState(false);
                        }
                    });

            tableCultures.getSelectionModel().clearSelection();

            // Setup table deselection
            WindowUtils.setupTableDeselection(tableCultures, mainContainer);

        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible d'initialiser la vue : " + e.getMessage());
        }
    }

    private void chargerParcelles() {
        try {
            List<Parcelle> parcelles = ps.recupererParAgriculteur(currentUser.getId());
            cbParcelle.setItems(FXCollections.observableArrayList(parcelles));
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger vos parcelles : " + e.getMessage());
        }
    }

    private void chargerMesCultures() {
        try {
            mesCultures = cs.recupererParAgriculteur(currentUser.getId());
            tableCultures.setItems(FXCollections.observableArrayList(mesCultures));
            mettreAJourInfo();
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger vos cultures : " + e.getMessage());
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
        for (Culture c : mesCultures) {
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

        List<Culture> filtered = mesCultures.stream()
                .filter(c -> {
                    if (!keyword.isEmpty()) {
                        boolean matchText = (c.getTypeCulture() != null && c.getTypeCulture().toLowerCase().contains(keyword))
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
        mettreAJourInfo();
    }

    private void mettreAJourInfo() {
        int count = tableCultures.getItems().size();
        WindowUtils.updateInfoLabel(lblInfo, count, "culture");
    }

    /**
     * Met à jour les indications dynamiques lorsqu'une parcelle est sélectionnée.
     * Affiche :
     * - Si la parcelle a déjà des cultures (répartition existante) ou non
     * - La surface restante disponible
     * - Le nombre de cultures qu'on peut encore ajouter (1 si pas de répartition, 2+ si répartition)
     */
    private void mettreAJourIndicationParcelle() {
        Parcelle selectedParcelle = cbParcelle.getValue();
        if (selectedParcelle == null) {
            if (vboxIndicationParcelle != null) {
                vboxIndicationParcelle.setVisible(false);
                vboxIndicationParcelle.setManaged(false);
            }
            if (lblIndicationNbCultures != null) {
                lblIndicationNbCultures.setText("💡 Sélectionnez une parcelle pour voir les cultures possibles");
                lblIndicationNbCultures.setStyle("-fx-text-fill: #aab0b5; -fx-font-size: 9; -fx-font-style: italic;");
            }
            if (lblIndicationSurface != null) {
                lblIndicationSurface.setText("💡 Surface en hectares que cette culture occupera sur la parcelle");
                lblIndicationSurface.setStyle("-fx-text-fill: #aab0b5; -fx-font-size: 9; -fx-font-style: italic;");
            }
            return;
        }

        try {
            double surfaceTotale = selectedParcelle.getSurface();
            double surfaceUtilisee = cs.getSurfaceUtiliseeParParcelle(selectedParcelle.getId());
            double surfaceRestante = surfaceTotale - surfaceUtilisee;
            List<Culture> culturesExistantes = cs.recupererParParcelle(selectedParcelle.getId());
            int nbCulturesExistantes = culturesExistantes.size();

            boolean hasRepartition = nbCulturesExistantes > 0 && surfaceRestante > 0.001;
            boolean parcellePleine = surfaceRestante <= 0.001;
            boolean parcelleVide = nbCulturesExistantes == 0;

            vboxIndicationParcelle.setVisible(true);
            vboxIndicationParcelle.setManaged(true);

            if (parcellePleine) {
                // Parcelle complète - aucune surface disponible
                vboxIndicationParcelle.setStyle("-fx-background-color: #FFEBEE; -fx-background-radius: 8; -fx-padding: 10;");
                lblIndicationParcelle.setText("❌ Parcelle complète — Aucune surface disponible");
                lblIndicationParcelle.setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold; -fx-font-size: 11;");
                lblIndicationDetail.setText(String.format(
                        "Surface totale: %.2f ha | Utilisée: %.2f ha | Restante: 0 ha\n" +
                        "Cultures existantes: %d — Vous ne pouvez pas ajouter de nouvelle culture.",
                        surfaceTotale, surfaceUtilisee, nbCulturesExistantes));
                lblIndicationDetail.setStyle("-fx-text-fill: #B71C1C; -fx-font-size: 10;");

                if (lblIndicationNbCultures != null) {
                    lblIndicationNbCultures.setText("❌ Parcelle complète — Aucune culture possible");
                    lblIndicationNbCultures.setStyle("-fx-text-fill: #C62828; -fx-font-size: 9; -fx-font-style: italic;");
                }
                if (lblIndicationSurface != null) {
                    lblIndicationSurface.setText("❌ Plus de surface disponible sur cette parcelle");
                    lblIndicationSurface.setStyle("-fx-text-fill: #C62828; -fx-font-size: 9; -fx-font-style: italic;");
                }

            } else if (parcelleVide) {
                // Parcelle vide - aucune culture existante
                vboxIndicationParcelle.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 8; -fx-padding: 10;");
                lblIndicationParcelle.setText("✅ Parcelle vide — Pas de répartition");
                lblIndicationParcelle.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-font-size: 11;");
                lblIndicationDetail.setText(String.format(
                        "Surface totale disponible: %.2f ha\n" +
                        "Vous pouvez ajouter une seule culture qui occupe toute la surface,\n" +
                        "ou plusieurs cultures en répartissant la surface.",
                        surfaceTotale));
                lblIndicationDetail.setStyle("-fx-text-fill: #1B5E20; -fx-font-size: 10;");

                if (lblIndicationNbCultures != null) {
                    lblIndicationNbCultures.setText(String.format(
                            "✅ 1 culture (toute la surface) ou plusieurs cultures (répartition) — %.2f ha disponibles", surfaceTotale));
                    lblIndicationNbCultures.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 9; -fx-font-style: italic;");
                }
                if (lblIndicationSurface != null) {
                    lblIndicationSurface.setText(String.format(
                            "💡 Surface max: %.2f ha — Si vous utilisez moins, la parcelle aura une répartition", surfaceTotale));
                    lblIndicationSurface.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 9; -fx-font-style: italic;");
                }

            } else if (hasRepartition) {
                // Parcelle avec répartition - il y a de la place pour ajouter des cultures
                vboxIndicationParcelle.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 8; -fx-padding: 10;");
                lblIndicationParcelle.setText(String.format(
                        "🔀 Répartition existante — %d culture(s) déjà plantée(s)", nbCulturesExistantes));
                lblIndicationParcelle.setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold; -fx-font-size: 11;");
                lblIndicationDetail.setText(String.format(
                        "Surface totale: %.2f ha | Utilisée: %.2f ha | Restante: %.2f ha\n" +
                        "✅ Vous pouvez ajouter une nouvelle culture (surface ≤ %.2f ha).",
                        surfaceTotale, surfaceUtilisee, surfaceRestante, surfaceRestante));
                lblIndicationDetail.setStyle("-fx-text-fill: #BF360C; -fx-font-size: 10;");

                if (lblIndicationNbCultures != null) {
                    lblIndicationNbCultures.setText(String.format(
                            "🔀 Répartition: %d culture(s) existante(s) — Vous pouvez en ajouter d'autres (%.2f ha restants)",
                            nbCulturesExistantes, surfaceRestante));
                    lblIndicationNbCultures.setStyle("-fx-text-fill: #E65100; -fx-font-size: 9; -fx-font-style: italic;");
                }
                if (lblIndicationSurface != null) {
                    lblIndicationSurface.setText(String.format(
                            "⚠️ Surface max autorisée: %.2f ha (surface restante de la parcelle)", surfaceRestante));
                    lblIndicationSurface.setStyle("-fx-text-fill: #E65100; -fx-font-size: 9; -fx-font-style: italic;");
                }
            }
        } catch (SQLException e) {
            lblIndicationParcelle.setText("⚠️ Impossible de vérifier la parcelle");
            lblIndicationDetail.setText(e.getMessage());
        }
    }

    private void updateButtonState(boolean isEditMode) {
        if (btnAjouter != null) {
            btnAjouter.setVisible(!isEditMode);
            btnAjouter.setManaged(!isEditMode);
        }
        if (btnModifier != null) {
            btnModifier.setVisible(isEditMode);
            btnModifier.setManaged(isEditMode);
        }
        if (btnSupprimer != null) {
            btnSupprimer.setVisible(isEditMode);
            btnSupprimer.setManaged(isEditMode);
        }
    }

    @FXML
    void ajouterCulture(ActionEvent event) {
        try {
            String type = "";
            if (tfTypeCulture != null)
                type = tfTypeCulture.getText().trim();
            if (type.isEmpty()) {
                WindowUtils.showAlert("Erreur", "Le type est obligatoire.");
                return;
            }

            if (dpDatePlantation.getValue() == null) {
                WindowUtils.showAlert("Erreur", "La date de plantation est obligatoire.");
                return;
            }

            if (dpDateRecolte.getValue() == null) {
                WindowUtils.showAlert("Erreur", "La date de récolte prévue est obligatoire.");
                return;
            }

            // Validation contrainte temporelle
            if (!dpDatePlantation.getValue().isBefore(dpDateRecolte.getValue())) {
                WindowUtils.showAlert("Erreur", "La date de plantation doit être antérieure à la date de récolte.");
                return;
            }

            Parcelle selectedParcelle = cbParcelle.getValue();
            if (selectedParcelle == null) {
                WindowUtils.showAlert("Erreur", "Veuillez sélectionner une parcelle.");
                return;
            }

            Date datePlantation = Date.valueOf(dpDatePlantation.getValue());
            Date dateRecolte = Date.valueOf(dpDateRecolte.getValue());

            String saison = "";
            if (cbSaison != null && cbSaison.getValue() != null)
                saison = cbSaison.getValue().trim();

            String etat = "en_croissance"; // Toujours en_croissance lors de l'ajout

            Culture c = new Culture("", type, saison, datePlantation, dateRecolte, etat, selectedParcelle.getId());
            
            if (!tfSurfaceUtilisee.getText().isEmpty()) {
                try {
                    c.setSurfaceUtilisee(Double.parseDouble(tfSurfaceUtilisee.getText()));
                } catch (NumberFormatException e) {
                    WindowUtils.showAlert("Erreur", "La surface utilisée doit être un nombre valide.");
                    return;
                }
            } else {
                WindowUtils.showAlert("Erreur", "La surface utilisée est obligatoire.");
                return;
            }

            if (!tfRendementEstime.getText().isEmpty()) {
                try {
                    c.setRendementEstime(Double.parseDouble(tfRendementEstime.getText()));
                } catch (NumberFormatException e) {
                    WindowUtils.showAlert("Erreur", "Le rendement estimé doit être un nombre valide.");
                    return;
                }
            }

            cs.ajouter(c);

            WindowUtils.showAlert("Succès", "Culture ajoutée avec succès !");
            chargerMesCultures();
            viderChamps();
            tableCultures.getSelectionModel().clearSelection();
            mettreAJourIndicationParcelle();

        } catch (IllegalArgumentException e) {
            WindowUtils.showAlert("Erreur", e.getMessage());
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    @FXML
    void modifierCulture(ActionEvent event) {
        Culture selected = tableCultures.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        try {
            String type = "";
            if (tfTypeCulture != null)
                type = tfTypeCulture.getText().trim();
            if (type.isEmpty()) {
                WindowUtils.showAlert("Erreur", "Le type est obligatoire.");
                return;
            }

            if (dpDatePlantation.getValue() == null) {
                WindowUtils.showAlert("Erreur", "La date de plantation est obligatoire.");
                return;
            }
            if (dpDateRecolte.getValue() == null) {
                WindowUtils.showAlert("Erreur", "La date de récolte prévue est obligatoire.");
                return;
            }
            if (!dpDatePlantation.getValue().isBefore(dpDateRecolte.getValue())) {
                WindowUtils.showAlert("Erreur", "La date de plantation doit être antérieure à la date de récolte.");
                return;
            }

            String etat = "";
            if (cbEtatCulture != null && cbEtatCulture.getValue() != null)
                etat = cbEtatCulture.getValue().trim();
            if (etat.isEmpty()) {
                WindowUtils.showAlert("Erreur", "Veuillez sélectionner l'état de la culture.");
                return;
            }

            selected.setTypeCulture(type);
            if (cbSaison != null && cbSaison.getValue() != null)
                selected.setSaison(cbSaison.getValue().trim());
            selected.setDatePlantation(Date.valueOf(dpDatePlantation.getValue()));
            selected.setDateRecoltePrevue(Date.valueOf(dpDateRecolte.getValue()));
            selected.setEtatCulture(etat);

            if (!tfSurfaceUtilisee.getText().isEmpty()) {
                try {
                    selected.setSurfaceUtilisee(Double.parseDouble(tfSurfaceUtilisee.getText()));
                } catch (NumberFormatException e) {
                    WindowUtils.showAlert("Erreur", "La surface utilisée doit être un nombre valide.");
                    return;
                }
            } else {
                WindowUtils.showAlert("Erreur", "La surface utilisée est obligatoire.");
                return;
            }

            if (!tfRendementEstime.getText().isEmpty()) {
                try {
                    selected.setRendementEstime(Double.parseDouble(tfRendementEstime.getText()));
                } catch (NumberFormatException e) {
                    WindowUtils.showAlert("Erreur", "Le rendement estimé doit être un nombre valide.");
                    return;
                }
            }

            cs.modifier(selected);

            WindowUtils.showAlert("Succès", "Culture modifiée avec succès !");
            chargerMesCultures();
            tableCultures.getSelectionModel().clearSelection();

        } catch (IllegalArgumentException e) {
            WindowUtils.showAlert("Erreur", e.getMessage());
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de la modification : " + e.getMessage());
        }
    }

    @FXML
    void supprimerCulture(ActionEvent event) {
        Culture selected = tableCultures.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez une culture à supprimer.");
            return;
        }

        if (WindowUtils.showConfirmation("Supprimer cette culture ?",
                "Cette action supprimera définitivement la culture sélectionnée.")) {
            try {
                cs.supprimer(selected.getId());
                chargerMesCultures();
                tableCultures.getSelectionModel().clearSelection();
                WindowUtils.showAlert("Succès", "Culture supprimée avec succès !");
            } catch (SQLException e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void rafraichir(ActionEvent event) {
        if (tfRecherche != null)
            tfRecherche.clear();
        if (cbFilterTypeCulture != null) cbFilterTypeCulture.setValue("Tous");
        if (cbFilterSaison != null) cbFilterSaison.setValue("Tous");
        if (cbFilterEtat != null) cbFilterEtat.setValue("Tous");
        chargerMesCultures();
        initialiserFiltres();
        tableCultures.getSelectionModel().clearSelection();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
        tableCultures.getSelectionModel().clearSelection();
    }

    private void viderChamps() {
        if (tfTypeCulture != null)
            tfTypeCulture.clear();
        if (cbSaison != null)
            cbSaison.setValue(null);
        if (cbEtatCulture != null) {
            cbEtatCulture.setValue("en_croissance");
            cbEtatCulture.setDisable(true);
        }
        dpDatePlantation.setValue(LocalDate.now());
        dpDateRecolte.setValue(null);
        cbParcelle.setValue(null);
        tfSurfaceUtilisee.clear();
        tfRendementEstime.clear();

        // Auto-détecter saison pour la date par défaut
        if (dpDatePlantation.getValue() != null && cbSaison != null) {
            cbSaison.setValue(detecterSaison(dpDatePlantation.getValue()));
        }

        // Réinitialiser les indications
        if (vboxIndicationParcelle != null) {
            vboxIndicationParcelle.setVisible(false);
            vboxIndicationParcelle.setManaged(false);
        }
        if (lblIndicationNbCultures != null) {
            lblIndicationNbCultures.setText("💡 Sélectionnez une parcelle pour voir les cultures possibles");
            lblIndicationNbCultures.setStyle("-fx-text-fill: #aab0b5; -fx-font-size: 9; -fx-font-style: italic;");
        }
        if (lblIndicationSurface != null) {
            lblIndicationSurface.setText("💡 Surface en hectares que cette culture occupera sur la parcelle");
            lblIndicationSurface.setStyle("-fx-text-fill: #aab0b5; -fx-font-size: 9; -fx-font-style: italic;");
        }
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurDashboard.fxml", "Ardhi - Espace Cultures");
    }

    @FXML
    void ouvrirPlanificateurIrrigation(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/IrrigationPlanner.fxml", "Ardhi - 💧 Planificateur Besoins en Eau");
    }

    @FXML
    void ouvrirRoiCalculator(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/RoiCalculator.fxml", "Ardhi - 📊 ROI Agricole");
    }

    // ==================== UTILITAIRES WORKFLOW ====================

    /**
     * Détecte la saison agricole à partir d'une date.
     * Hiver: Déc-Fév | Printemps: Mar-Mai | Été: Jun-Août | Automne: Sep-Nov
     */
    private String detecterSaison(LocalDate date) {
        Month mois = date.getMonth();
        int annee = date.getYear();
        String saison;
        switch (mois) {
            case DECEMBER, JANUARY, FEBRUARY -> saison = "Hiver";
            case MARCH, APRIL, MAY -> saison = "Printemps";
            case JUNE, JULY, AUGUST -> saison = "Été";
            default -> saison = "Automne";
        }
        return saison + " " + annee;
    }

    /**
     * Configure le DatePicker de récolte pour bloquer les dates
     * antérieures ou égales à la date de plantation.
     */
    private void configurerDateRecolteMin(LocalDate datePlantation) {
        dpDateRecolte.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !item.isAfter(datePlantation)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #FFCDD2; -fx-text-fill: #B71C1C;");
                }
            }
        });
        // Si la date de récolte actuelle est invalide, la reset
        if (dpDateRecolte.getValue() != null && !dpDateRecolte.getValue().isAfter(datePlantation)) {
            dpDateRecolte.setValue(null);
        }
    }
}
