package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.Region;
import javafx.scene.effect.DropShadow;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.layout.Priority;

import javafx.stage.Stage;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.Parcelle_Cultures.GroqFieldRecommender;
import tn.neuron.ardhi.services.Parcelle_Cultures.GroqFieldRecommender.FieldRecommendation;
import tn.neuron.ardhi.services.Parcelle_Cultures.ParcelleService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import java.sql.SQLException;

public class AgriculteurParcellesController implements Initializable {

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

    private Parcelle selectedParcelle = null;
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
    private Label lblSurfaceTotale;

    @FXML
    private TextField tfSurface;
    @FXML
    private TextField tfLocalisation;
    @FXML
    private TextField tfTypeSol;
    @FXML
    private TextField tfSystemeIrrigation;
    @FXML
    private CheckBox chkStatutActive;
    @FXML
    private TextField tfLatitude;
    @FXML
    private TextField tfLongitude;

    @FXML
    private Label lblTitreFormulaire;
    @FXML
    private VBox mainContainer;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;
    @FXML
    private VBox boxSmartIA;
    @FXML
    private Button btnSmartIA;

    // === Groq Recommendation ===
    @FXML
    private VBox boxGeminiReco;
    @FXML
    private Label lblGeminiExplication;
    @FXML
    private ProgressIndicator piGemini;
    @FXML
    private Button btnAppliquerReco;
    @FXML
    private Button btnReessayerReco;

    private ParcelleService ps = new ParcelleService();
    private GroqFieldRecommender groqRecommender = new GroqFieldRecommender();
    private FieldRecommendation derniereRecommandation;
    private User currentUser;
    private List<Parcelle> mesParcelles = new ArrayList<>();


    
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
            colSurface.setCellValueFactory(new PropertyValueFactory<>("surface"));
            colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
            colTypeSol.setCellValueFactory(new PropertyValueFactory<>("typeSol"));
            colSystemeIrrigation.setCellValueFactory(new PropertyValueFactory<>("systemeIrrigation"));
            colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

            chargerMesParcelles();
            updateButtonState(false);

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

            // Écouteur de sélection dans le tableau
            tableParcelles.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectionnerParcelle(newSelection);
                } else {
                    clearSelection();
                }
            });

            clearSelection();

            // Setup table deselection
            WindowUtils.setupTableDeselection(tableParcelles, mainContainer);

        } catch (Exception e) {
            WindowUtils.showAlert("Erreur", "Impossible d'initialiser la vue : " + e.getMessage());
        }
    }

    private void chargerMesParcelles() {
        try {
            mesParcelles = ps.recupererParAgriculteur(currentUser.getId());
            tableParcelles.setItems(FXCollections.observableArrayList(mesParcelles));
            mettreAJourInfo(mesParcelles.size());
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger vos parcelles : " + e.getMessage());
        }
    }

    private void clearSelection() {
        selectedParcelle = null;
        lblTitreFormulaire.setText("Ajouter une parcelle");
        viderChamps();
        updateButtonState(false);
    }

    private void selectionnerParcelle(Parcelle p) {
        selectedParcelle = p;
        lblTitreFormulaire.setText("Modifier: " + p.getLocalisation());
        tfSurface.setText(String.valueOf(p.getSurface()));
        tfLocalisation.setText(p.getLocalisation());
        if (tfTypeSol != null) tfTypeSol.setText(p.getTypeSol());
        if (tfSystemeIrrigation != null) tfSystemeIrrigation.setText(p.getSystemeIrrigation());
        if (chkStatutActive != null) chkStatutActive.setSelected("active".equalsIgnoreCase(p.getStatut()));
        if (p.getLatitude() != null) tfLatitude.setText(String.valueOf(p.getLatitude()));
        else tfLatitude.clear();
        if (p.getLongitude() != null) tfLongitude.setText(String.valueOf(p.getLongitude()));
        else tfLongitude.clear();

        updateButtonState(true);
    }

    private void initialiserFiltres() {
        // Extraire les valeurs uniques depuis les données
        java.util.Set<String> typesSol = new java.util.TreeSet<>();
        java.util.Set<String> irrigations = new java.util.TreeSet<>();
        java.util.Set<String> statuts = new java.util.TreeSet<>();
        typesSol.add("Tous");
        irrigations.add("Tous");
        statuts.add("Tous");
        for (Parcelle p : mesParcelles) {
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

        List<Parcelle> filtered = mesParcelles.stream()
                .filter(p -> {
                    // Filtre texte
                    if (!keyword.isEmpty()) {
                        boolean matchText = (p.getLocalisation() != null && p.getLocalisation().toLowerCase().contains(keyword))
                                || (p.getTypeSol() != null && p.getTypeSol().toLowerCase().contains(keyword));
                        if (!matchText) return false;
                    }
                    // Filtre type sol
                    if (!"Tous".equals(typeSolFilter) && (p.getTypeSol() == null || !p.getTypeSol().equals(typeSolFilter))) return false;
                    // Filtre irrigation
                    if (!"Tous".equals(irrigationFilter) && (p.getSystemeIrrigation() == null || !p.getSystemeIrrigation().equals(irrigationFilter))) return false;
                    // Filtre statut
                    if (!"Tous".equals(statutFilter) && (p.getStatut() == null || !p.getStatut().equals(statutFilter))) return false;
                    return true;
                })
                .toList();
        tableParcelles.setItems(FXCollections.observableArrayList(filtered));
        mettreAJourInfo(filtered.size());
    }

    private void mettreAJourInfo(int count) {
        WindowUtils.updateInfoLabel(lblInfo, count, "parcelle");

        // Calculer surface totale
        double surfaceTotale = ps.getSurfaceTotaleParAgriculteur(currentUser.getId());
        lblSurfaceTotale.setText(String.format("Surface totale: %.2f ha", surfaceTotale));
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
        // Afficher le bouton Smart IA uniquement quand une parcelle est sélectionnée
        if (boxSmartIA != null) {
            boxSmartIA.setVisible(isEditMode);
            boxSmartIA.setManaged(isEditMode);
        }
    }

    /**
     * Ouvre l'interface Smart Agriculture pour la parcelle sélectionnée.
     * Passe la parcelle au SmartParcelleController via setParcelle().
     */
    @FXML
    void ouvrirSmartParcelle(ActionEvent event) {
        Parcelle selected = selectedParcelle;
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez une parcelle pour lancer l'analyse IA.");
            return;
        }

        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/SmartParcelle.fxml",
            "Ardhi Smart - " + selected.getLocalisation(), 
            (SmartParcelleController ctrl) -> {
                ctrl.setParcelle(selected);
            });
    }

    @FXML
    void ajouterParcelle(ActionEvent event) {
        try {
            double surface;
            try {
                surface = Double.parseDouble(tfSurface.getText().trim());
                if (surface <= 0) {
                    WindowUtils.showAlert("Erreur", "La surface doit être supérieure à 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                WindowUtils.showAlert("Erreur", "La surface doit être un nombre valide.");
                return;
            }

            String loc = tfLocalisation.getText().trim();
            String typeSol = tfTypeSol != null ? tfTypeSol.getText().trim() : "";
            String sysIrr = tfSystemeIrrigation != null ? tfSystemeIrrigation.getText().trim() : "";
            String statut = (chkStatutActive != null && chkStatutActive.isSelected()) ? "active" : "repos";

            Parcelle p = new Parcelle(surface, loc, typeSol, sysIrr, statut, currentUser.getId());

            // GPS (Optionnel)
            try {
                if (!tfLatitude.getText().trim().isEmpty()) {
                    p.setLatitude(Double.parseDouble(tfLatitude.getText().trim()));
                }
                if (!tfLongitude.getText().trim().isEmpty()) {
                    p.setLongitude(Double.parseDouble(tfLongitude.getText().trim()));
                }
            } catch (NumberFormatException e) {
                WindowUtils.showAlert("Attention", "Coordonnées GPS invalides. Elles seront ignorées.");
            }

            ps.ajouter(p);

            WindowUtils.showAlert("Succès", "Parcelle ajoutée avec succès !");
            chargerMesParcelles();
            viderChamps();
            clearSelection();

        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    @FXML
    void modifierParcelle(ActionEvent event) {
        Parcelle selected = selectedParcelle;
        if (selected == null)
            return;

        try {
            double surface;
            try {
                surface = Double.parseDouble(tfSurface.getText().trim());
                if (surface <= 0) {
                    WindowUtils.showAlert("Erreur", "La surface doit être supérieure à 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                WindowUtils.showAlert("Erreur", "La surface doit être un nombre valide.");
                return;
            }

            selected.setSurface(surface);
            selected.setLocalisation(tfLocalisation.getText().trim());
            if (tfTypeSol != null)
                selected.setTypeSol(tfTypeSol.getText().trim());
            if (tfSystemeIrrigation != null)
                selected.setSystemeIrrigation(tfSystemeIrrigation.getText().trim());
            if (chkStatutActive != null) {
                selected.setStatut(chkStatutActive.isSelected() ? "active" : "repos");
            }

            // GPS (Optionnel)
            try {
                if (!tfLatitude.getText().trim().isEmpty()) {
                    selected.setLatitude(Double.parseDouble(tfLatitude.getText().trim()));
                } else {
                    selected.setLatitude(null); // Or 0.0, depending on how you want to handle empty
                }
                if (!tfLongitude.getText().trim().isEmpty()) {
                    selected.setLongitude(Double.parseDouble(tfLongitude.getText().trim()));
                } else {
                    selected.setLongitude(null); // Or 0.0
                }
            } catch (NumberFormatException e) {
                WindowUtils.showAlert("Attention", "Coordonnées GPS invalides. Elles seront ignorées.");
            }

            ps.modifier(selected);

            WindowUtils.showAlert("Succès", "Parcelle modifiée avec succès !");
            chargerMesParcelles();
            clearSelection();

        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Erreur lors de la modification : " + e.getMessage());
        }
    }

    @FXML
    void supprimerParcelle(ActionEvent event) {
        Parcelle selected = selectedParcelle;
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez une parcelle à supprimer.");
            return;
        }

        if (WindowUtils.showConfirmation("Supprimer cette parcelle ?",
                "⚠️ ATTENTION: Toutes les cultures associées seront également supprimées.")) {
            try {
                ps.supprimer(selected.getId());
                chargerMesParcelles();
                clearSelection();
                WindowUtils.showAlert("Succès", "Parcelle supprimée avec succès !");
            } catch (SQLException e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void voirCultures(MouseEvent event) {
        Parcelle selected = selectedParcelle;
        if (selected == null) {
            WindowUtils.showAlert("Attention", "Sélectionnez une parcelle pour voir ses cultures.");
            return;
        }
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurCultures.fxml",
                "Cultures - " + selected.getLocalisation());
    }

    @FXML
    void rafraichir(ActionEvent event) {
        if (tfRecherche != null)
            tfRecherche.clear();
        if (cbFilterTypeSol != null) cbFilterTypeSol.setValue("Tous");
        if (cbFilterIrrigation != null) cbFilterIrrigation.setValue("Tous");
        if (cbFilterStatut != null) cbFilterStatut.setValue("Tous");
        chargerMesParcelles();
        initialiserFiltres();
        clearSelection();
        viderChamps(); // Clear fields after refresh
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
        clearSelection();
    }

    private void viderChamps() {
        if (tfSurface != null)
            tfSurface.clear();
        if (tfLocalisation != null)
            tfLocalisation.clear();
        if (tfTypeSol != null)
            tfTypeSol.clear();
        if (tfSystemeIrrigation != null)
            tfSystemeIrrigation.clear();
        if (chkStatutActive != null)
            chkStatutActive.setSelected(true);
        // Reset Gemini recommendation
        derniereRecommandation = null;
        if (boxGeminiReco != null) {
            boxGeminiReco.setVisible(false);
            boxGeminiReco.setManaged(false);
        }
    }

    @FXML
    void ouvrirSelecteurCarte(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Parcelle_Cultures/MapPicker.fxml"));
            VBox root = loader.load();
            MapPickerController ctrl = loader.getController();
            
            ctrl.setOnLocationSelected(result -> {
                if (tfLatitude != null) tfLatitude.setText(String.format("%.6f", result.lat).replace(",", "."));
                if (tfLongitude != null) tfLongitude.setText(String.format("%.6f", result.lon).replace(",", "."));
                if (result.adresse != null && !result.adresse.isEmpty() && tfLocalisation != null) {
                    tfLocalisation.setText(result.adresse);
                }
                // Déclencher la recommandation Gemini après sélection sur la carte
                lancerRecommandationGemini(result.adresse, result.lat, result.lon);
            });

            Stage stage = new Stage();
            stage.setTitle("Choisir l'emplacement de la parcelle");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Throwable cause = e;
            while (cause.getCause() != null) cause = cause.getCause();
            WindowUtils.showAlert("Erreur", "Impossible d'ouvrir la carte : " + cause.getMessage());
        }
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurDashboard.fxml", "Ardhi - Espace Cultures");
    }

    @FXML
    void ouvrirStatsAgriculteur(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurStats.fxml", "Ardhi - Tableau de bord Agricole");
    }

    // ==================== GEMINI RECOMMANDATION ====================

    /**
     * Lance une recommandation Groq en arrière-plan après sélection de la localisation.
     */
    private void lancerRecommandationGemini(String localisation, Double lat, Double lon) {
        if (!groqRecommender.isDisponible()) return;
        if (localisation == null || localisation.isBlank()) return;

        // Afficher le box avec spinner de chargement
        Platform.runLater(() -> {
            if (boxGeminiReco != null) {
                boxGeminiReco.setVisible(true);
                boxGeminiReco.setManaged(true);
            }
            if (piGemini != null) piGemini.setVisible(true);
            if (lblGeminiExplication != null) lblGeminiExplication.setText("Analyse de la région en cours...");
            if (btnAppliquerReco != null) btnAppliquerReco.setDisable(true);
        });

        Task<FieldRecommendation> task = new Task<>() {
            @Override
            protected FieldRecommendation call() {
                return groqRecommender.recommander(localisation, lat, lon);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            FieldRecommendation reco = task.getValue();
            if (piGemini != null) piGemini.setVisible(false);

            if (reco != null && reco.isSuccess()) {
                derniereRecommandation = reco;
                String source = groqRecommender.getSourceUtilisee();

                // Auto-appliquer le type de sol (obligatoire)
                if (!reco.typeSol.isEmpty() && tfTypeSol != null) {
                    tfTypeSol.setText(reco.typeSol);
                }

                StringBuilder sb = new StringBuilder();
                sb.append("✅ Type de sol appliqué automatiquement : ").append(reco.typeSol).append("\n\n");
                if (!reco.systemeIrrigation.isEmpty()) sb.append("💧 Irrigation suggérée : ").append(reco.systemeIrrigation).append("\n");
                if (!reco.surfaceRecommandee.isEmpty()) sb.append("📏 Surface suggérée : ").append(reco.surfaceRecommandee).append(" ha\n");
                if (!reco.explication.isEmpty()) sb.append("\n").append(reco.explication);
                sb.append("\n\n🤖 Source : ").append(source != null ? source : "IA");
                if (lblGeminiExplication != null) lblGeminiExplication.setText(sb.toString());
                // Bouton Appliquer = seulement irrigation + surface
                boolean hasOptional = !reco.systemeIrrigation.isEmpty() || !reco.surfaceRecommandee.isEmpty();
                if (btnAppliquerReco != null) btnAppliquerReco.setDisable(!hasOptional);
                if (btnReessayerReco != null) btnReessayerReco.setVisible(false);
            } else {
                String errMsg = (reco != null && reco.erreur != null) ? reco.erreur : "Erreur inconnue";
                if (lblGeminiExplication != null)
                    lblGeminiExplication.setText("⚠ " + errMsg);
                if (btnAppliquerReco != null) btnAppliquerReco.setDisable(true);
                if (btnReessayerReco != null) btnReessayerReco.setVisible(true);
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            if (piGemini != null) piGemini.setVisible(false);
            Throwable ex = task.getException();
            String msg = (ex != null) ? ex.getMessage() : "Erreur inconnue";
            if (lblGeminiExplication != null) lblGeminiExplication.setText("⚠ " + msg);
            if (btnAppliquerReco != null) btnAppliquerReco.setDisable(true);
            if (btnReessayerReco != null) btnReessayerReco.setVisible(true);
        }));

        new Thread(task, "gemini-field-reco").start();
    }

    /**
     * Applique les recommandations Gemini dans les champs du formulaire.
     */
    @FXML
    void appliquerRecommandationGemini(ActionEvent event) {
        if (derniereRecommandation == null) return;

        // Appliquer seulement irrigation + surface (sol déjà appliqué automatiquement)
        if (!derniereRecommandation.systemeIrrigation.isEmpty() && tfSystemeIrrigation != null)
            tfSystemeIrrigation.setText(derniereRecommandation.systemeIrrigation);
        if (!derniereRecommandation.surfaceRecommandee.isEmpty() && tfSurface != null)
            tfSurface.setText(derniereRecommandation.surfaceRecommandee);

        // Masquer le box après application
        if (boxGeminiReco != null) {
            boxGeminiReco.setStyle(boxGeminiReco.getStyle() + "; -fx-border-color: #43A047; -fx-border-width: 2;");
            if (lblGeminiExplication != null)
                lblGeminiExplication.setText("✅ Irrigation et surface appliquées ! Vous pouvez modifier les valeurs.");
            if (btnAppliquerReco != null) btnAppliquerReco.setDisable(true);
        }
    }

    /**
     * Réessayer la recommandation Gemini avec la localisation actuelle.
     */
    @FXML
    void reessayerRecommandationGemini(ActionEvent event) {
        String loc = (tfLocalisation != null) ? tfLocalisation.getText() : null;
        Double lat = null, lon = null;
        try {
            if (tfLatitude != null && !tfLatitude.getText().isBlank())
                lat = Double.parseDouble(tfLatitude.getText().replace(",", "."));
            if (tfLongitude != null && !tfLongitude.getText().isBlank())
                lon = Double.parseDouble(tfLongitude.getText().replace(",", "."));
        } catch (NumberFormatException ignored) {}

        if (loc != null && !loc.isBlank()) {
            lancerRecommandationGemini(loc, lat, lon);
        }
    }

    /**
     * Ignore les recommandations Gemini.
     */
    @FXML
    void ignorerRecommandationGemini(ActionEvent event) {
        derniereRecommandation = null;
        if (boxGeminiReco != null) {
            boxGeminiReco.setVisible(false);
            boxGeminiReco.setManaged(false);
        }
    }

    @FXML
    void ouvrirDossierCredit(ActionEvent event) {
        Parcelle selected = selectedParcelle;
        if (selected == null) {
            WindowUtils.showAlert("Sélection requise", "Veuillez sélectionner une parcelle avant de générer un dossier de crédit.");
            return;
        }
        WindowUtils.<CreditDossierController>loadScene(event,
                "/fxml/Parcelle_Cultures/CreditDossier.fxml",
                "Ardhi - Dossier Crédit Agricole",
                controller -> controller.setParcelle(selected));
    }
}
