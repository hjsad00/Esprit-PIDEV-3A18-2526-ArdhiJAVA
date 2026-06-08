package tn.neuron.ardhi.controllers.gestionemployecontroller;

//import pdf pour taille, size, tableau, ecriture, couleur ...
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import tn.neuron.ardhi.services.gestionemployeservice.NotificationService;
// Autres imports JavaFX, etc.
import javafx.animation.*;
import javafx.collections.FXCollections;//filtrage dynamique
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task; //faire des opérations en arrière-plan
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;//Button,TableView,ComboBox,Label,DatePicker
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;//Permet de choisir où enregistrer le PDF.
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;// Utilisé pour animations
//IMPORTS PROJET
import tn.neuron.ardhi.models.gestionemployemodel.Employe;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.services.gestionemployeservice.EmployeService;
import tn.neuron.ardhi.services.gestionemployeservice.TacheService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import tn.neuron.ardhi.utils.gestionemployeutils.NotificationToast;
import tn.neuron.ardhi.utils.gestionemployeutils.CacheManager;
import tn.neuron.ardhi.utils.gestionemployeutils.LanguageManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences; //sauvegarder:filtres,tri,préférences
import tn.neuron.ardhi.models.gestionemployemodel.Notification;
import tn.neuron.ardhi.services.gestionemployeservice.NotificationService;
import tn.neuron.ardhi.utils.gestionemployeutils.AgriculteurContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import java.io.IOException;
import tn.neuron.ardhi.utils.gestionemployeutils.RecommendationDialog;
import tn.neuron.ardhi.utils.gestionemployeutils.RiskPredictionDialog;
import tn.neuron.ardhi.services.gestionemployeservice.TacheRiskAnalyzer;
import java.util.Optional;
import com.google.api.services.calendar.model.Event;
import tn.neuron.ardhi.services.gestionemployeservice.GoogleCalendarService;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.stream.Collectors;
import java.awt.Desktop;
public class TacheController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private TableView<Tache>           tableTaches;
    @FXML private TableColumn<Tache,Integer> colId;
    @FXML private TableColumn<Tache,String>  colTitre;
    @FXML private TableColumn<Tache,String>  colDescription;
    @FXML private TableColumn<Tache,String>  colStatut;
    @FXML private TableColumn<Tache,LocalDate>    colDateDebut;
    @FXML private TableColumn<Tache,LocalDate>    colDateFin;
    @FXML private TableColumn<Tache,Integer> colIdEmploye;
    @FXML private TableColumn<Tache,Integer> colPriorite;
    @FXML private TableColumn<Tache,String>  colCategorie;

    @FXML private TextField                  txtRecherche;
    @FXML private ComboBox<String>           cmbStatut;
    @FXML private ComboBox<String>           cmbPriorite;
    @FXML private ComboBox<String>           cmbCategorie;
    @FXML private ComboBox<Employe>          cmbEmploye;
    @FXML private ComboBox<String>           cmbTri;
    @FXML private DatePicker                dateDebutFilter;
    @FXML private DatePicker                dateFinFilter;
    @FXML private Label                      lblTotalTaches;
    @FXML private Label                      lblEnCours;
    @FXML private Label                      lblTerminees;
    @FXML private Label                      lblEnAttente;
    @FXML private Label                      lblSelection;
    @FXML private Label                      lblTempsChargement;
    @SuppressWarnings("unused")
    @FXML private Button                     btnRetour;
    @SuppressWarnings("unused")
    @FXML private Button                     btnAjouter;
    @FXML private Button                     btnModifier;
    @FXML private Button                     btnSupprimer;
    @SuppressWarnings("unused")
    @FXML private Button                     btnExporterPDF;
    @SuppressWarnings("unused")
    @FXML private Button                     btnActualiser;
    @FXML private Button                     btnAnalyseIA;   // 🔮 Module IA Prédiction
    @FXML private ProgressIndicator         progressIndicator;
    @FXML private VBox                      statsContainer;
    @FXML private HBox                      filterContainer;
    @FXML private ToggleButton             toggleFiltresAvances;
    @FXML private GridPane                 advancedFilters;

    // ── Services & State ─────────────────────────────────────────────────────
    private TacheService   tacheService;
    private EmployeService employeService;
    private CacheManager   cacheManager;
    private ObservableList<Tache> masterList;
    private FilteredList<Tache>   filteredList;
    private SortedList<Tache>     sortedList;
    private boolean triCroissant = true;
    private Stack<Tache> deletedTachesStack = new Stack<>();
    private Preferences prefs;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Constantes ───────────────────────────────────────────────────────────
    private static final String[] STATUTS = {"Tous", "En attente", "En cours", "Terminé", "Validé", "Annulé"};
    private static final String[] PRIORITES = {"Toutes", "Basse", "Moyenne", "Haute", "Critique"};
    private static final String[] CATEGORIES = {"Toutes", "Plantation", "Récolte", "Irrigation", "Fertilisation", "Maintenance", "Administratif"};
    private static final String[] CRITERES_TRI = {"ID", "Titre", "Statut", "Priorité", "Date Début", "Date Fin", "Catégorie"};
    private NotificationService notificationService;
    // ── Initialize ───────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        long startTime = System.currentTimeMillis();

        tacheService = new TacheService();
        employeService = new EmployeService();
        cacheManager = CacheManager.getInstance();
        masterList = FXCollections.observableArrayList();
        prefs = Preferences.userNodeForPackage(TacheController.class);
        // Injecter TacheService dans GoogleCalendarService pour sauvegarder les IDs d'événements
        GoogleCalendarService.getInstance().setTacheService(tacheService);


        setupUI();//separationcode
        setupTableColumns();
        setupComboBoxes();
        setupFilters();
        setupFilteredAndSortedLists();
        setupTableSelectionListener();
        setupAnimations();
        setupBtnAnalyseIA();  // 🔮 Module IA

        loadTachesAsync();
        loadEmployesAsync();

        long endTime = System.currentTimeMillis();
        if (lblTempsChargement != null) {
            LanguageManager lm = LanguageManager.getInstance();
            lblTempsChargement.setText(lm.get("common.loading.time").replace("{0}", String.valueOf(endTime - startTime)));
        }

        // Appliquer RTL si arabe
        javafx.application.Platform.runLater(() -> {
            if (tableTaches != null && tableTaches.getScene() != null) {
                LanguageManager.getInstance().applyOrientation(
                        (javafx.scene.Parent) tableTaches.getScene().getRoot()
                );
            }
        });
    }

    private void setupUI() {
        // Initialiser les filtres avancés cachés
        if (advancedFilters != null) {
            advancedFilters.setVisible(false);
            advancedFilters.setManaged(false);
        }

        // Toggle pour filtres avancés
        if (toggleFiltresAvances != null) {
            toggleFiltresAvances.setOnAction(e -> {
                boolean visible = !advancedFilters.isVisible();
                advancedFilters.setVisible(visible);
                advancedFilters.setManaged(visible);

                // Animation
                FadeTransition ft = new FadeTransition(Duration.millis(300), advancedFilters);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            });
        }

        // Progress indicator caché par défaut
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPriorite.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colIdEmploye.setCellValueFactory(new PropertyValueFactory<>("idEmploye"));


        // Coloration statut avec icônes
        colStatut.setCellFactory(col -> new TableCell<Tache, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setGraphic(null);
                    return;
                }
                setText(item);
                HBox box = new HBox(5);
                box.setAlignment(Pos.CENTER_LEFT);

                Label icon = new Label();
                icon.setStyle("-fx-font-size: 14px;");

                switch (item.toUpperCase().replace(" ", "_")) {
                    case "EN_ATTENTE":
                        icon.setText("⏳");
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        box.setStyle("-fx-background-color: rgba(231,76,60,0.1); -fx-padding: 4 8; -fx-background-radius: 12;");
                        break;
                    case "EN_COURS":
                        icon.setText("🔄");
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        box.setStyle("-fx-background-color: rgba(243,156,18,0.1); -fx-padding: 4 8; -fx-background-radius: 12;");
                        break;
                    case "TERMINÉ":
                    case "TERMINE":
                        icon.setText("✅");
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        box.setStyle("-fx-background-color: rgba(39,174,96,0.1); -fx-padding: 4 8; -fx-background-radius: 12;");
                        break;
                    case "VALIDÉ":
                    case "VALIDE":
                        icon.setText("⭐");
                        setStyle("-fx-text-fill: #8e44ad; -fx-font-weight: bold;");
                        box.setStyle("-fx-background-color: rgba(142,68,173,0.1); -fx-padding: 4 8; -fx-background-radius: 12;");
                        break;
                    case "ANNULÉ":
                    case "ANNULE":
                        icon.setText("❌");
                        setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
                        box.setStyle("-fx-background-color: rgba(127,140,141,0.1); -fx-padding: 4 8; -fx-background-radius: 12;");
                        break;
                }
                box.getChildren().addAll(icon, new Label(item));//icone+texte
                setGraphic(box);
            }
        });

        // Coloration priorité
        colPriorite.setCellFactory(col -> new TableCell<Tache, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                String priorite = getPrioriteString(item);
                setText(priorite);

                switch (item) {
                    case 1: setStyle("-fx-text-fill: #27ae60;"); break;
                    case 2: setStyle("-fx-text-fill: #f39c12;"); break;
                    case 3: setStyle("-fx-text-fill: #e67e22;"); break;
                    case 4: setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); break;
                    default: setStyle("");
                }
            }
        });

        // Formatage dates
        colDateDebut.setCellFactory(col -> new TableCell<Tache, LocalDate>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(dateFormatter));
                }
            }
        });

        colDateFin.setCellFactory(col -> new TableCell<Tache, LocalDate>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(dateFormatter));
                }
            }
        });

        // Nom employé avec cache
        colIdEmploye.setCellFactory(col -> new TableCell<Tache, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }

                Employe emp = cacheManager.getEmploye(item);
                if (emp == null) {
                    emp = employeService.getEmployeById(item);
                    if (emp != null) cacheManager.putEmploye(item, emp);
                }
                setText(emp != null ? emp.getNom() + " " + emp.getPrenom() : "ID: " + item);
            }
        });
    }

    private void setupComboBoxes() {
        // Statut
        if (cmbStatut != null) {
            cmbStatut.setItems(FXCollections.observableArrayList(STATUTS));
            cmbStatut.setValue(loadFilterPreference("lastStatut", "Tous"));
        }

        // Priorité
        if (cmbPriorite != null) {
            cmbPriorite.setItems(FXCollections.observableArrayList(PRIORITES));
            cmbPriorite.setValue(loadFilterPreference("lastPriorite", "Toutes"));
        }

        // Catégorie
        if (cmbCategorie != null) {
            cmbCategorie.setItems(FXCollections.observableArrayList(CATEGORIES));
            cmbCategorie.setValue(loadFilterPreference("lastCategorie", "Toutes"));
        }

        // Tri
        if (cmbTri != null) {
            cmbTri.setItems(FXCollections.observableArrayList(CRITERES_TRI));
            cmbTri.setValue(loadFilterPreference("lastTri", "Date Début"));
        }

        // Recherche
        if (txtRecherche != null) {
            txtRecherche.setText(loadFilterPreference("lastRecherche", ""));
        }
    }

    private void setupFilters() {
        // Setup ComboBox Employé
        if (cmbEmploye != null) {
            cmbEmploye.setItems(FXCollections.observableArrayList());
            cmbEmploye.getItems().add(0, null);
            cmbEmploye.setButtonCell(createEmployeCell("Tous les employés"));
            cmbEmploye.setCellFactory(lv -> createEmployeCell("Tous les employés"));
        }

        // Add listeners pour filtres automatiques
        if (txtRecherche != null) {
            txtRecherche.textProperty().addListener((obs, o, n) -> {
                handleRechercher();
                saveFilterState();
            });
        }

        if (cmbStatut != null) {
            cmbStatut.valueProperty().addListener((obs, o, n) -> {
                handleFilterStatut();
                saveFilterState();
            });
        }

        if (cmbPriorite != null) {
            cmbPriorite.valueProperty().addListener((obs, o, n) -> handleFilterPriorite());
        }

        if (cmbCategorie != null) {
            cmbCategorie.valueProperty().addListener((obs, o, n) -> handleFilterCategorie());
        }

        if (cmbEmploye != null) {
            cmbEmploye.valueProperty().addListener((obs, o, n) -> handleFilterEmploye());
        }

        if (dateDebutFilter != null) {
            dateDebutFilter.valueProperty().addListener((obs, o, n) -> handleFilterDates());
        }

        if (dateFinFilter != null) {
            dateFinFilter.valueProperty().addListener((obs, o, n) -> handleFilterDates());
        }
    }

    private void setupFilteredAndSortedLists() {
        filteredList = new FilteredList<>(masterList, p -> true);
        sortedList = new SortedList<>(filteredList);
        // ✅ PAS DE BIND - On contrôle le tri manuellement avec les boutons ⬆️ ⬇️
        // sortedList.comparatorProperty().bind(tableTaches.comparatorProperty());
        tableTaches.setItems(sortedList);
    }

    private void setupTableSelectionListener() {
        tableTaches.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            // ✅ Activer/désactiver les boutons selon la sélection
            boolean hasSelection = (n != null);

            if (btnModifier != null) {
                btnModifier.setDisable(!hasSelection);
            }
            if (btnSupprimer != null) {
                btnSupprimer.setDisable(!hasSelection);
            }

            // Mettre à jour le label de sélection
            if (lblSelection != null) {
                if (hasSelection) {
                    lblSelection.setText("📌 " + n.getTitre() + " (" + n.getStatut() + ")");
                } else {
                    lblSelection.setText("Aucune sélection");
                }
            }
        });
    }
    private void setupKeyboardShortcuts() {
        txtRecherche.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // ✅ CORRECT : getAccelerators() sur Scene, PAS sur Node
                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                        () -> {
                            if (txtRecherche != null) txtRecherche.requestFocus();
                        }
                );

                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                        () -> handleAjouter()
                );

                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN),
                        () -> handleModifier()
                );

                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
                        () -> handleSupprimer()
                );

                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN),
                        () -> handleExportPDF()
                );

                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN),
                        () -> handleActualiser()
                );

                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
                        () -> handleUndo()
                );

                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.ESCAPE),
                        () -> handleRetour()
                );
            }
        });
    }

    private void setupAnimations() {
        // Animation d'entrée pour le tableau
        FadeTransition fadeTable = new FadeTransition(Duration.millis(500), tableTaches);
        fadeTable.setFromValue(0);
        fadeTable.setToValue(1);
        fadeTable.play();

        // Animation pour les statistiques
        if (statsContainer != null) {
            ScaleTransition scaleStats = new ScaleTransition(Duration.millis(800), statsContainer);
            scaleStats.setFromX(0.8);
            scaleStats.setFromY(0.8);
            scaleStats.setToX(1);
            scaleStats.setToY(1);
            scaleStats.setInterpolator(Interpolator.EASE_OUT);
            scaleStats.play();
        }
    }

    // ── Chargement Asynchrone (filtré par rôle) ─────────────────────────────
    private void loadTachesAsync() {
        if (progressIndicator != null) progressIndicator.setVisible(true);
        tableTaches.setDisable(true);

        // Déterminer le contexte courant selon le rôle
        final Role currentRole;
        final Integer idAgriculteur;
        final Integer idEmploye;
        try {
            tn.neuron.ardhi.models.UserAndDiag.User currentUser = UserSession.getInstance().getUser();
            currentRole   = currentUser.getRole();
            idAgriculteur = AgriculteurContext.getActiveAgriculteurId();
            idEmploye     = currentUser.getId();
        } catch (Exception ex) {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            tableTaches.setDisable(false);
            return;
        }

        Task<List<Tache>> task = new Task<>() {
            @Override
            protected List<Tache> call() {
                updateMessage("Chargement des tâches...");
                if (currentRole == Role.ADMIN) {
                    // Admin supervisant un agriculteur spécifique
                    if (idAgriculteur != null) {
                        return tacheService.getTachesByAgriculteur(idAgriculteur);
                    }
                    // Admin sans contexte d'agriculteur → ne rien afficher
                    return java.util.Collections.emptyList();
                } else if (currentRole == Role.AGRICULTEUR) {
                    // Agriculteur → ses propres tâches
                    return tacheService.getTachesByAgriculteur(idEmploye);
                } else {
                    // Employé → tâches qui lui sont assignées
                    return tacheService.getTachesByEmployeId(idEmploye);
                }
            }
        };

        task.setOnSucceeded(e -> {
            masterList.clear();
            masterList.addAll(task.getValue());
            updateStatistics();
            if (progressIndicator != null) progressIndicator.setVisible(false);
            tableTaches.setDisable(false);

            NotificationToast.showNotification(
                    "✅ " + masterList.size() + " tâches chargées",
                    NotificationToast.SUCCESS,
                    2000
            );
        });

        task.setOnFailed(e -> {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            tableTaches.setDisable(false);
            NotificationToast.showNotification(
                    "❌ Erreur de chargement: " + task.getException().getMessage(),
                    NotificationToast.ERROR,
                    3000
            );
        });

        new Thread(task).start();
    }

    private void loadEmployesAsync() {
        Task<List<Employe>> task = new Task<>() {
            @Override
            protected List<Employe> call() {
                return employeService.getAllEmployes();
            }
        };

        task.setOnSucceeded(e -> {
            ObservableList<Employe> employes = FXCollections.observableArrayList(task.getValue());
            if (cmbEmploye != null) {
                cmbEmploye.setItems(employes);
                cmbEmploye.getItems().add(0, null);
            }

            // Mettre en cache (ignorer null ajouté pour ComboBox)
            employes.stream()
                    .filter(emp -> emp != null)
                    .forEach(emp -> cacheManager.putEmploye(emp.getId(), emp));
        });

        new Thread(task).start();
    }

    // ── Filtres ─────────────────────────────────────────────────────────────
    @FXML
    private void handleRechercher() {
        String searchText = txtRecherche.getText().toLowerCase().trim();
        filteredList.setPredicate(t -> {
            if (searchText.isEmpty()) return true;

            return t.getTitre().toLowerCase().contains(searchText)
                    || t.getDescription().toLowerCase().contains(searchText)
                    || t.getStatut().toLowerCase().contains(searchText)
                    || getEmployeName(t.getIdEmploye()).toLowerCase().contains(searchText);
        });
    }

    @FXML
    private void handleFilterStatut() {
        updateFilter();
    }

    @FXML
    private void handleFilterPriorite() {
        updateFilter();
    }

    @FXML
    private void handleFilterCategorie() {
        updateFilter();
    }

    @FXML
    private void handleFilterEmploye() {
        updateFilter();
    }

    @FXML
    private void handleFilterDates() {
        updateFilter();
    }

    private void updateFilter() {
        String statut = cmbStatut != null ? cmbStatut.getValue() : "Tous";
        String priorite = cmbPriorite != null ? cmbPriorite.getValue() : "Toutes";
        String categorie = cmbCategorie != null ? cmbCategorie.getValue() : "Toutes";
        Employe employe = cmbEmploye != null ? cmbEmploye.getValue() : null;
        LocalDate dateDebut = dateDebutFilter != null ? dateDebutFilter.getValue() : null;
        LocalDate dateFin = dateFinFilter != null ? dateFinFilter.getValue() : null;

        filteredList.setPredicate(t -> {
            // Statut
            if (!"Tous".equals(statut) && !t.getStatut().equalsIgnoreCase(statut))
                return false;

            // Priorité
            if (!"Toutes".equals(priorite)) {
                int prioriteValue = getPrioriteValue(priorite);
                Integer tPriorite = t.getPriorite();
                if (tPriorite == null || !tPriorite.equals(prioriteValue))
                    return false;
            }

            // ✅ Catégorie – getCategorie() au lieu de getCategory()
            if (!"Toutes".equals(categorie)) {
                String tCategorie = t.getCategorie();
                if (tCategorie == null || !tCategorie.equalsIgnoreCase(categorie))
                    return false;
            }

            // Employé
            if (employe != null && t.getIdEmploye() != null && !t.getIdEmploye().equals(employe.getId()))
                return false;

            // ✅ Dates – plus de toEpochDay, plus de conversion inutile
            if (dateDebut != null) {
                if (t.getDateDebut() == null) return false;
                if (t.getDateDebut().isBefore(dateDebut)) return false;
            }

            if (dateFin != null) {
                if (t.getDateFin() == null) return false;
                if (t.getDateFin().isAfter(dateFin)) return false;
            }

            return true;
        });
    }
    @FXML
    private void handleTri() {
        if (cmbTri == null || cmbTri.getValue() == null) return;
        Comparator<Tache> c = getComparatorForCritere(cmbTri.getValue());
        if (!triCroissant) c = c.reversed();
        FXCollections.sort(masterList, c);
    }

    @FXML
    private void handleTriCroissant() {
        triCroissant = true;
        handleTri();
        saveFilterState();
    }

    @FXML
    private void handleTriDecroissant() {
        triCroissant = false;
        handleTri();
        saveFilterState();
    }

    private Comparator<Tache> getComparatorForCritere(String critere) {
        switch (critere) {
            case "ID": return Comparator.comparing(Tache::getId);
            case "Titre": return Comparator.comparing(Tache::getTitre);
            case "Statut": return Comparator.comparing(Tache::getStatut);
            case "Priorité": return Comparator.comparing(Tache::getPriorite, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Date Début": return Comparator.comparing(Tache::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Date Fin": return Comparator.comparing(Tache::getDateFin, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Catégorie": return Comparator.comparing(Tache::getCategorie, Comparator.nullsLast(Comparator.naturalOrder()));
            default: return Comparator.comparing(Tache::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder()));
        }
    }

    // ── Statistiques ─────────────────────────────────────────────────────────
    private void updateStatistics() {
        if (lblTotalTaches == null || lblEnCours == null) return;

        int total = masterList.size();
        long enCours = masterList.stream()
                .filter(t -> "En cours".equalsIgnoreCase(t.getStatut()))
                .count();
        long terminees = masterList.stream()
                .filter(t -> "Terminé".equalsIgnoreCase(t.getStatut()))
                .count();
        long enAttente = masterList.stream()
                .filter(t -> "En attente".equalsIgnoreCase(t.getStatut()))
                .count();

        lblTotalTaches.setText(String.valueOf(total));
        lblEnCours.setText(String.valueOf(enCours));
        if (lblTerminees != null) lblTerminees.setText(String.valueOf(terminees));
        if (lblEnAttente != null) lblEnAttente.setText(String.valueOf(enAttente));
    }

    @FXML
    private void handleShowStatistics() {
        Stage statsStage = new Stage();
        statsStage.setTitle("📊 Statistiques des Tâches");
        statsStage.initModality(Modality.APPLICATION_MODAL);
        statsStage.initStyle(StageStyle.DECORATED);

        // Graphique circulaire des statuts
        long enAttente = masterList.stream().filter(t -> "En attente".equalsIgnoreCase(t.getStatut())).count();
        long enCours = masterList.stream().filter(t -> "En cours".equalsIgnoreCase(t.getStatut())).count();
        long termine = masterList.stream().filter(t -> "Terminé".equalsIgnoreCase(t.getStatut())).count();
        long valide = masterList.stream().filter(t -> "Validé".equalsIgnoreCase(t.getStatut())).count();
        long annule = masterList.stream().filter(t -> "Annulé".equalsIgnoreCase(t.getStatut())).count();

        PieChart pieChart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("⏳ En attente (" + enAttente + ")", enAttente),
                new PieChart.Data("🔄 En cours (" + enCours + ")", enCours),
                new PieChart.Data("✅ Terminé (" + termine + ")", termine),
                new PieChart.Data("⭐ Validé (" + valide + ")", valide),
                new PieChart.Data("❌ Annulé (" + annule + ")", annule)
        ));
        pieChart.setTitle("📈 Répartition par Statut");
        pieChart.setPrefSize(600, 400);
        pieChart.setAnimated(true);
        pieChart.setLabelLineLength(10);
        pieChart.setLegendVisible(true);

        // Graphique en barres des priorités
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("🎯 Tâches par Priorité");
        xAxis.setLabel("Priorité");
        yAxis.setLabel("Nombre");

        XYChart.Series<String, Number> seriesPriorite = new XYChart.Series<>();
        seriesPriorite.setName("Tâches");

        long basse = masterList.stream().filter(t -> t.getPriorite() != null && t.getPriorite() == 1).count();
        long moyenne = masterList.stream().filter(t -> t.getPriorite() != null && t.getPriorite() == 2).count();
        long haute = masterList.stream().filter(t -> t.getPriorite() != null && t.getPriorite() == 3).count();
        long critique = masterList.stream().filter(t -> t.getPriorite() != null && t.getPriorite() == 4).count();

        seriesPriorite.getData().add(new XYChart.Data<>("Basse", basse));
        seriesPriorite.getData().add(new XYChart.Data<>("Moyenne", moyenne));
        seriesPriorite.getData().add(new XYChart.Data<>("Haute", haute));
        seriesPriorite.getData().add(new XYChart.Data<>("Critique", critique));
        barChart.getData().add(seriesPriorite);
        barChart.setPrefSize(600, 400);

        // Graphique en barres des employés
        CategoryAxis xAxisEmp = new CategoryAxis();
        NumberAxis yAxisEmp = new NumberAxis();
        BarChart<String, Number> barChartEmp = new BarChart<>(xAxisEmp, yAxisEmp);
        barChartEmp.setTitle("👥 Tâches par Employé");
        xAxisEmp.setLabel("Employé");
        yAxisEmp.setLabel("Nombre");

        XYChart.Series<String, Number> seriesEmploye = new XYChart.Series<>();
        seriesEmploye.setName("Tâches assignées");

        // Utilisation de java.util.HashMap au lieu de Collectors.groupingBy pour éviter les conflits
        Map<Integer, Long> tachesParEmploye = new HashMap<>();
        for (Tache t : masterList) {
            if (t.getIdEmploye() != null) {
                tachesParEmploye.put(t.getIdEmploye(),
                        tachesParEmploye.getOrDefault(t.getIdEmploye(), 0L) + 1);
            }
        }

        tachesParEmploye.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    Employe emp = cacheManager.getEmploye(entry.getKey());
                    if (emp == null) {
                        emp = employeService.getEmployeById(entry.getKey());
                        if (emp != null) cacheManager.putEmploye(entry.getKey(), emp);
                    }
                    seriesEmploye.getData().add(new XYChart.Data<>(
                            emp != null ? emp.getNom() : "ID: " + entry.getKey(),
                            entry.getValue()
                    ));
                });
        barChartEmp.getData().add(seriesEmploye);
        barChartEmp.setPrefSize(600, 400);

        // Graphique d'évolution temporelle
        CategoryAxis xAxisLine = new CategoryAxis();
        xAxisLine.setLabel("Date");
        NumberAxis yAxisLine = new NumberAxis();
        yAxisLine.setLabel("Nombre de tâches");

        LineChart<String, Number> lineChart = new LineChart<>(xAxisLine, yAxisLine);
        lineChart.setTitle("📅 Évolution des tâches");
        lineChart.setPrefSize(600, 300);

        XYChart.Series<String, Number> seriesEvolution = new XYChart.Series<>();
        seriesEvolution.setName("Nouvelles tâches");

        // Grouper par date (30 derniers jours) - avec HashMap
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> parDate = new HashMap<>();
        for (Tache t : masterList) {
            if (t.getDateDebut() != null) {
                LocalDate date = LocalDate.from(LocalDate.ofEpochDay(t.getDateDebut().toEpochDay()));
                if (date.isAfter(today.minusDays(30))) {
                    parDate.put(date, parDate.getOrDefault(date, 0L) + 1);
                }
            }
        }

        parDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry ->
                        seriesEvolution.getData().add(new XYChart.Data<>(
                                entry.getKey().format(dateFormatter),
                                entry.getValue()
                        ))
                );

        lineChart.getData().add(seriesEvolution);

        // Layout principal
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: white;");

        Tab tabStatuts = new Tab("📊 Statuts", new VBox(20, pieChart));
        tabStatuts.setClosable(false);

        Tab tabPriorites = new Tab("🎯 Priorités", new VBox(20, barChart));
        tabPriorites.setClosable(false);

        Tab tabEmployes = new Tab("👥 Employés", new VBox(20, barChartEmp));
        tabEmployes.setClosable(false);

        Tab tabEvolution = new Tab("📈 Évolution", new VBox(20, lineChart));
        tabEvolution.setClosable(false);

        Tab tabResume = new Tab("📋 Résumé", createResumePanel());
        tabResume.setClosable(false);

        tabPane.getTabs().addAll(tabResume, tabStatuts, tabPriorites, tabEmployes, tabEvolution);

        VBox root = new VBox(20, tabPane);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #F5F7F0, #E8EBE0);");

        Scene scene = new Scene(root, 900, 700);
        statsStage.setScene(scene);
        statsStage.show();
    }

    private VBox createResumePanel() {
        VBox resume = new VBox(15);
        resume.setPadding(new Insets(20));
        resume.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        Label title = new Label("📋 Résumé des statistiques");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #6B7F3F;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);

        int total = masterList.size();
        double tauxCompletion = total > 0 ?
                (masterList.stream().filter(t -> "Terminé".equalsIgnoreCase(t.getStatut())).count() * 100.0 / total) : 0;

        long enRetard = masterList.stream()
                .filter(t -> t.getDateFin() != null
                        && !"Terminé".equalsIgnoreCase(t.getStatut())
                        && !"Validé".equalsIgnoreCase(t.getStatut())
                        && t.getDateFin().isBefore(LocalDate.now()))   // ✅ plus de toEpochDay()
                .count();

        long sansEmploye = masterList.stream()
                .filter(t -> t.getIdEmploye() == null)
                .count();

        grid.add(createMetricCard("📊 Total tâches", String.valueOf(total), "#6B7F3F"), 0, 0);
        grid.add(createMetricCard("✅ Taux complétion", String.format("%.1f%%", tauxCompletion), "#27ae60"), 1, 0);
        grid.add(createMetricCard("⚠️ En retard", String.valueOf(enRetard), "#e74c3c"), 2, 0);
        grid.add(createMetricCard("👤 Non assignées", String.valueOf(sansEmploye), "#f39c12"), 0, 1);

        resume.getChildren().addAll(title, grid);
        return resume;
    }

    private VBox createMetricCard(String label, String value, String color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 10; -fx-border-color: " + color + "40; -fx-border-radius: 10;");
        card.setPrefWidth(180);

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

        card.getChildren().addAll(lblValue, lblLabel);
        return card;
    }

    // ── CRUD amélioré ───────────────────────────────────────────────────────
    @FXML
    private void handleAjouter() {
        Dialog<Tache> dialog = createEnhancedTacheDialog("➕ Ajouter une Tâche", null);
        Optional<Tache> result = dialog.showAndWait();

        result.ifPresent(tache -> {
            if (progressIndicator != null) progressIndicator.setVisible(true);

            Task<Boolean> task = new Task<>() { //Crée une tâche asynchrone (s'exécute en arrière-plan sans bloquer l'interface)
                @Override//Appelle le service pour créer la tâche dans la base de données
                protected Boolean call() {
                    updateMessage("Création de la tâche...");
                    return tacheService.createTache(tache);
                }
            };
            task.setOnSucceeded(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                if (task.getValue()) {
                    NotificationToast.showNotification(
                            "✅ Tâche créée avec succès !",
                            NotificationToast.SUCCESS,
                            2500
                    );
                    loadTachesAsync();
                    // 🔔 Analyser les notifications
                    Integer idAgri = AgriculteurContext.getActiveAgriculteurId();
                    if (idAgri != null) {
                        if (notificationService != null) {
                            notificationService.analyserNotifications(idAgri);
                        }
                    }
                    // 📅 Sync Google Calendar — après BD (tâche a un ID)
                    if (tache.getDateDebut() != null) {
                        new Thread(() -> {
                            try {
                                GoogleCalendarService gcal = GoogleCalendarService.getInstance();
                                if (gcal.connecter()) {
                                    String evId = gcal.creerEvenement(tache);
                                    if (evId != null) tacheService.saveGoogleEventId(tache.getId(), evId);
                                }
                            } catch (Exception ex) {
                                System.err.println("[GCal] Erreur création : " + ex.getMessage());
                            }
                        }, "GCal-Create").start();
                    }
                }
            });

            task.setOnFailed(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                NotificationToast.showNotification(
                        "❌ Erreur: " + task.getException().getMessage(),
                        NotificationToast.ERROR,
                        3000
                );
            });

            new Thread(task).start(); //Lance la tâche dans un nouveau thread (en arrière-plan)
        });
    }

    @FXML
    private void handleModifier() {
        Tache selected = tableTaches.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationToast.showNotification(
                    "⚠️ Veuillez sélectionner une tâche",
                    NotificationToast.WARNING,
                    2000
            );
            return;
        }

        Dialog<Tache> dialog = createEnhancedTacheDialog("✏️ Modifier la Tâche", selected);
        Optional<Tache> result = dialog.showAndWait();

        result.ifPresent(tache -> {
            if (progressIndicator != null) progressIndicator.setVisible(true);

            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return tacheService.updateTache(tache);
                }
            };

            task.setOnSucceeded(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                if (task.getValue()) {
                    NotificationToast.showNotification(
                            "✅ Tâche modifiée avec succès !",
                            NotificationToast.SUCCESS,
                            2500
                    );
                    loadTachesAsync();

                    // 🔔 Déclencher l'analyse des notifications
                    Integer idAgri = AgriculteurContext.getActiveAgriculteurId();
                    if (idAgri != null) {
                        notificationService.analyserNotifications(idAgri);
                    }

                    // 📅 Sync Google Calendar — modification
                    new Thread(() -> {
                        try {
                            GoogleCalendarService gcal = GoogleCalendarService.getInstance();
                            if (gcal.connecter()) {
                                String existingId = tache.getGoogleEventId();
                                if (existingId != null && !existingId.isBlank()) {
                                    // Événement connu → mettre à jour
                                    boolean ok = gcal.mettreAJourEvenement(existingId, tache);
                                    System.out.println("[GCal] MAJ : " + (ok ? "✅" : "❌") + " id=" + existingId);
                                } else {
                                    // Jamais synchronisé → créer et mémoriser
                                    String newId = gcal.creerEvenement(tache);
                                    if (newId != null) tacheService.saveGoogleEventId(tache.getId(), newId);
                                    System.out.println("[GCal] 1er sync après modif : " + newId);
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("[GCal] Erreur MAJ : " + ex.getMessage());
                        }
                    }, "GCal-Update").start();
                }

            });

            task.setOnFailed(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                NotificationToast.showNotification(
                        "❌ Erreur: " + task.getException().getMessage(),
                        NotificationToast.ERROR,
                        3000
                );
            });

            new Thread(task).start();
        });
    }

    @FXML
    private void handleSupprimer() {
        //Récupère la tâche sélectionnée
        //Vérifie qu'une tâche est sélectionnée
        Tache selected = tableTaches.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationToast.showNotification(
                    "⚠️ Veuillez sélectionner une tâche",
                    NotificationToast.WARNING,
                    2000
            );

            return;
        }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("🗑️ Confirmation de suppression");
        conf.setHeaderText("Supprimer la tâche : " + selected.getTitre());
        conf.setContentText("Cette action est irréversible. Voulez-vous continuer ?");

        ButtonType btnSupprimer = new ButtonType("🗑️ Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("❌ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        conf.getButtonTypes().setAll(btnSupprimer, btnAnnuler);

        DialogPane dialogPane = conf.getDialogPane();
        Button deleteButton = (Button) dialogPane.lookupButton(btnSupprimer);
        if (deleteButton != null) {
            deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        }

        Optional<ButtonType> result = conf.showAndWait();
        if (result.isPresent() && result.get() == btnSupprimer) {
            if (progressIndicator != null) progressIndicator.setVisible(true);

            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    updateMessage("Suppression de la tâche...");
                    return tacheService.deleteTache(selected.getId());
                }//supprimer de la base dd
            };
//undo (pile)
            task.setOnSucceeded(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                if (task.getValue()) {
                    deletedTachesStack.push(selected);
                    NotificationToast.showNotification(
                            "✅ Tâche supprimée (Ctrl+Z pour annuler)",
                            NotificationToast.SUCCESS,
                            3000
                    );
                    loadTachesAsync();
                    // 🔔 Analyser les notifications
                    Integer idAgri = AgriculteurContext.getActiveAgriculteurId();
                    if (idAgri != null) {
                        notificationService.analyserNotifications(idAgri);
                    }

                    // 📅 Sync Google Calendar — suppression
                    String gId = selected.getGoogleEventId();
                    if (gId != null && !gId.isBlank()) {
                        new Thread(() -> {
                            try {
                                GoogleCalendarService gcal = GoogleCalendarService.getInstance();
                                if (gcal.connecter()) {
                                    boolean ok = gcal.supprimerEvenement(gId);
                                    System.out.println("[GCal] Suppression : " + (ok ? "✅" : "❌") + " id=" + gId);
                                }
                            } catch (Exception ex) {
                                System.err.println("[GCal] Erreur suppression : " + ex.getMessage());
                            }
                        }, "GCal-Delete").start();
                    }
                }
            });

            task.setOnFailed(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                NotificationToast.showNotification(
                        "❌ Erreur: " + task.getException().getMessage(),
                        NotificationToast.ERROR,
                        3000
                );
            });

            new Thread(task).start();
        }
    }

    @FXML
    private void handleUndo() {//annule la supprission
        if (!deletedTachesStack.isEmpty()) {
            Tache toRestore = deletedTachesStack.pop(); //lifo la derniere

            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return tacheService.createTache(toRestore);
                }
            };
//Recrée la tâche dans la base de données
            task.setOnSucceeded(e -> {
                if (task.getValue()) {
                    NotificationToast.showNotification(
                            "🔄 Tâche restaurée : " + toRestore.getTitre(),
                            NotificationToast.SUCCESS,
                            2500
                    );
                    loadTachesAsync();
                    // 🔔 Analyser les notifications
                    Integer idAgri = AgriculteurContext.getActiveAgriculteurId();
                    if (idAgri != null) {
                        notificationService.analyserNotifications(idAgri);
                    }
                }
            });

            new Thread(task).start();
        } else {//pile vide
            NotificationToast.showNotification(
                    "ℹ️ Aucune tâche à restaurer",
                    NotificationToast.INFO,
                    2000
            );
        }
    }

    @FXML
    private void handleActualiser() {
        if (txtRecherche != null) txtRecherche.clear(); //Vide la barre de recherche
        if (cmbStatut != null) cmbStatut.setValue("Tous");//Réinitialise tous les filtres
        if (cmbPriorite != null) cmbPriorite.setValue("Toutes");
        if (cmbCategorie != null) cmbCategorie.setValue("Toutes");
        if (cmbEmploye != null) cmbEmploye.setValue(null);
        if (dateDebutFilter != null) dateDebutFilter.setValue(null);//Efface les filtres de date
        if (dateFinFilter != null) dateFinFilter.setValue(null);
        triCroissant = true;//pardefaut

        loadTachesAsync();

        NotificationToast.showNotification(
                "🔄 Données actualisées",
                NotificationToast.INFO,
                1500
        );
    }

    // ── Export PDF amélioré ─────────────────────────────────────────────────

    @FXML
    private void handlePlanification() {
        ouvrirPlanificationSemaine();
    }

    @FXML
    private void handleExportPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("📄 Enregistrer le rapport PDF");//crée un sélecteur de fichier Nom par défaut : rapport_taches_15-02-2026.pdf
        fc.setInitialFileName("rapport_taches_" + LocalDate.now().format(dateFormatter).replace("/", "-") + ".pdf");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"), //fichier accepté pdf
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File file = fc.showSaveDialog(tableTaches.getScene().getWindow()); //Affiche le dialogue "Enregistrer sous"
        if (file == null) return;
        if (progressIndicator != null) progressIndicator.setVisible(true);

        Task<Void> task = new Task<>() {   // ✅ Task<Void>, pas Task<VoId>
            @Override
            protected Void call() throws Exception {   // ✅ protected Void call()
                updateMessage("Génération du PDF...");

                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(doc, new FileOutputStream(file));
                doc.open();

                Font fTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BaseColor.BLACK);
                Paragraph title = new Paragraph("📋 RAPPORT DES TÂCHES", fTitle);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(10);
                doc.add(title);

                Font fSub = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.DARK_GRAY);
                String statutFilter = cmbStatut != null ? cmbStatut.getValue() : "Tous";
                String prioriteFilter = cmbPriorite != null ? cmbPriorite.getValue() : "Toutes";

                Paragraph info = new Paragraph(
                        "Généré le : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                                " | Filtre actuel : " + statutFilter +
                                " | " + prioriteFilter,
                        fSub
                );
                info.setAlignment(Element.ALIGN_CENTER);
                info.setSpacingAfter(20);
                doc.add(info);

                PdfPTable table = new PdfPTable(8);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{0.5f, 1.5f, 2.5f, 1.2f, 1f, 1f, 1.2f, 1.5f});

                Font fHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);
                String[] headers = {"ID", "Titre", "Description", "Statut", "Priorité", "Début", "Fin", "Employé"};

                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, fHeader));
                    cell.setBackgroundColor(new BaseColor(107, 127, 63));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(8);
                    table.addCell(cell);
                }

                Font fData = FontFactory.getFont(FontFactory.HELVETICA, 10);

                for (Tache t : sortedList) {
                    String prioriteStr = getPrioriteString(t.getPriorite());
                    table.addCell(new Phrase(prioriteStr, fData));
                    table.addCell(new Phrase(String.valueOf(t.getId()), fData));
                    table.addCell(new Phrase(t.getTitre(), fData));

                    String desc = t.getDescription();
                    if (desc != null && desc.length() > 100) desc = desc.substring(0, 97) + "...";
                    table.addCell(new Phrase(desc != null ? desc : "", fData));

                    PdfPCell statutCell = new PdfPCell(new Phrase(t.getStatut(), fData));
                    switch (t.getStatut().toUpperCase().replace(" ", "_")) {
                        case "EN_ATTENTE":
                            statutCell.setBackgroundColor(BaseColor.RED);
                            break;
                        case "EN_COURS":
                            statutCell.setBackgroundColor(BaseColor.ORANGE);
                            break;
                        case "TERMINÉ":
                        case "TERMINE":
                            statutCell.setBackgroundColor(BaseColor.GREEN);
                            break;
                        case "VALIDÉ":
                        case "VALIDE":
                            statutCell.setBackgroundColor(BaseColor.BLUE);
                            break;
                    }
                    table.addCell(statutCell);
                    table.addCell(new Phrase(prioriteStr, fData));

                    table.addCell(new Phrase(t.getDateDebut() != null ?
                            t.getDateDebut().format(dateFormatter) : "", fData));
                    table.addCell(new Phrase(t.getDateFin() != null ?
                            t.getDateFin().format(dateFormatter) : "", fData));
                    String empNom = getEmployeName(t.getIdEmploye());
                    table.addCell(new Phrase(empNom, fData));
                }

                doc.add(table);

                doc.newPage();

                Paragraph statsTitle = new Paragraph("📊 STATISTIQUES", fTitle);
                statsTitle.setAlignment(Element.ALIGN_CENTER);
                statsTitle.setSpacingAfter(20);
                doc.add(statsTitle);

                PdfPTable statsTable = new PdfPTable(2);
                statsTable.setWidthPercentage(50);
                statsTable.setHorizontalAlignment(Element.ALIGN_CENTER);

                addStatRow(statsTable, "Total tâches", String.valueOf(masterList.size()));
                addStatRow(statsTable, "En cours", String.valueOf(
                        masterList.stream().filter(t -> "En cours".equalsIgnoreCase(t.getStatut())).count()));
                addStatRow(statsTable, "Terminées", String.valueOf(
                        masterList.stream().filter(t -> "Terminé".equalsIgnoreCase(t.getStatut())).count()));
                addStatRow(statsTable, "En attente", String.valueOf(
                        masterList.stream().filter(t -> "En attente".equalsIgnoreCase(t.getStatut())).count()));

                doc.add(statsTable);

                doc.close();
                return null;
            }

            private void addStatRow(PdfPTable table, String label, String value) {
                Font fLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
                Font fValue = FontFactory.getFont(FontFactory.HELVETICA, 12);

                table.addCell(new Phrase(label, fLabel));
                table.addCell(new Phrase(value, fValue));
            }
        };

        task.setOnSucceeded(e -> {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            NotificationToast.showNotification(
                    "✅ PDF exporté avec succès !\n" + file.getName(),
                    NotificationToast.SUCCESS,
                    3000
            );
        });

        task.setOnFailed(e -> {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            NotificationToast.showNotification(
                    "❌ Erreur d'export PDF : " + task.getException().getMessage(),
                    NotificationToast.ERROR,
                    4000
            );
        });

        new Thread(task).start();
    }



    // ── Dialogue amélioré ───────────────────────────────────────────────────
    private Dialog<Tache> createEnhancedTacheDialog(String title, Tache tache) {
        Dialog<Tache> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(550);  // Réduit de 650 à 550
        dialog.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom right, #F5F7F0, #E8EBE0);");

        ButtonType btnOK = new ButtonType(
                tache == null ? "➕ Ajouter" : "✏️ Modifier",
                ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(btnOK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(btnOK);
        if (okButton != null) {
            okButton.setStyle("-fx-background-color: #6B7F3F; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
            okButton.setEffect(new DropShadow(10, Color.rgb(107, 127, 63, 0.3)));
        }

        // ── Champs ─────────────────────────────────────────────────────────
        TextField txtTitre = new TextField();
        txtTitre.setText(tache != null ? tache.getTitre() : "");
        txtTitre.setPromptText("Titre de la tâche");
        txtTitre.setStyle("-fx-background-radius: 8; -fx-padding: 10;");

        TextArea txtDescription = new TextArea();
        String description = (tache != null && tache.getDescription() != null) ? tache.getDescription() : "";
        txtDescription.setText(description);
        txtDescription.setPromptText("Description détaillée...");
        txtDescription.setPrefRowCount(3);
        txtDescription.setWrapText(true);
        txtDescription.setStyle("-fx-background-radius: 8; -fx-padding: 10;");

        ComboBox<String> cmbStatutDlg = new ComboBox<>(
                FXCollections.observableArrayList("En attente", "En cours", "Terminé", "Validé", "Annulé")
        );
        cmbStatutDlg.setValue(tache != null ? tache.getStatut() : "En attente");
        cmbStatutDlg.setPrefWidth(200);
        cmbStatutDlg.setStyle("-fx-background-radius: 8; -fx-padding: 8;");

        ComboBox<String> cmbPrioriteDlg = new ComboBox<>(
                FXCollections.observableArrayList("Basse", "Moyenne", "Haute", "Critique")
        );
        int priorite = tache != null && tache.getPriorite() != null ? tache.getPriorite() : 2;
        cmbPrioriteDlg.setValue(getPrioriteString(priorite));
        cmbPrioriteDlg.setPrefWidth(200);
        cmbPrioriteDlg.setStyle("-fx-background-radius: 8; -fx-padding: 8;");

        ComboBox<String> cmbCategorieDlg = new ComboBox<>(
                FXCollections.observableArrayList("Plantation", "Récolte", "Irrigation", "Fertilisation", "Maintenance", "Administratif")
        );
        cmbCategorieDlg.setValue(tache != null && tache.getCategorie() != null ?
                tache.getCategorie() : "Plantation");
        cmbCategorieDlg.setPrefWidth(200);
        cmbCategorieDlg.setStyle("-fx-background-radius: 8; -fx-padding: 8;");

        DatePicker dpDebut = new DatePicker();
        dpDebut.setValue(tache != null && tache.getDateDebut() != null ?
                tache.getDateDebut() : LocalDate.now());
        dpDebut.setPromptText("jj/mm/aaaa");
        dpDebut.setStyle("-fx-background-radius: 8; -fx-padding: 8;");

        DatePicker dpFin = new DatePicker();
        dpFin.setValue(tache != null && tache.getDateFin() != null ?
                tache.getDateFin() : LocalDate.now().plusDays(7));
        dpFin.setPromptText("jj/mm/aaaa");
        dpFin.setStyle("-fx-background-radius: 8; -fx-padding: 8;");

        ComboBox<Employe> cmbEmployeDlg = new ComboBox<>();
        ObservableList<Employe> employes = FXCollections.observableArrayList(employeService.getAllEmployes());
        cmbEmployeDlg.setItems(employes);
        cmbEmployeDlg.getItems().add(0, null);
        //Utilise le cache pour éviter les requêtes répétées à la BDD
        if (tache != null && tache.getIdEmploye() != null) {
            Employe emp = cacheManager.getEmploye(tache.getIdEmploye());
            if (emp == null) {
                emp = employeService.getEmployeById(tache.getIdEmploye());
                if (emp != null) cacheManager.putEmploye(tache.getIdEmploye(), emp);
            }
            cmbEmployeDlg.setValue(emp);
        }
        cmbEmployeDlg.setButtonCell(createEmployeCell("Non assigné"));
        cmbEmployeDlg.setCellFactory(lv -> createEmployeCell("Non assigné"));
        cmbEmployeDlg.setPrefWidth(300);
        cmbEmployeDlg.setStyle("-fx-background-radius: 8; -fx-padding: 8;");
        Button btnRecommander = new Button("🤖 Recommander un employé");
        btnRecommander.setStyle(
                "-fx-background-color: #27ae60;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 10 15;" +
                        "-fx-cursor: hand;"
        );
        btnRecommander.setOnAction(e -> {
            // Si c'est une modification, on a déjà l'ID
            if (tache != null && tache.getId() > 0) {
                Optional<Employe> choisi = RecommendationDialog.showAndWait(tache.getId());
                if (choisi.isPresent()) {
                    cmbEmployeDlg.setValue(choisi.get());
                    NotificationToast.showNotification(
                            "✓ Employé recommandé sélectionné",
                            NotificationToast.SUCCESS,
                            2000
                    );
                }
            } else {
                NotificationToast.showNotification(
                        "⚠️ Sauvegardez d'abord la tâche pour obtenir des recommandations",
                        NotificationToast.WARNING,
                        3000
                );
            }
        });


        Label errTitre = createErrorLabel();
        Label errDescription = createErrorLabel();
        Label errDates = createErrorLabel();
        // ── Erreur employe obligatoire ──────────────────────────────────
        Label errEmploye = createErrorLabel();
        errEmploye.setText("⚠ Employé requis");
        errEmploye.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");
        HBox employeBox = new HBox(10);
        employeBox.getChildren().addAll(cmbEmployeDlg, btnRecommander);
        // ── Grille principale avec sections ────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));

        Label section1 = new Label("📋 INFORMATIONS PRINCIPALES");
        section1.setStyle("-fx-font-weight: bold; -fx-text-fill: #6B7F3F; -fx-font-size: 14px;");
        grid.add(section1, 0, 0, 3, 1);

        grid.add(createLabel("Titre *:"), 0, 1);
        grid.add(txtTitre, 1, 1);
        grid.add(errTitre, 2, 1);

        grid.add(createLabel("Description *:"), 0, 2);
        grid.add(txtDescription, 1, 2, 2, 1);

        Label section2 = new Label("⚙️ STATUT & PRIORITÉ");
        section2.setStyle("-fx-font-weight: bold; -fx-text-fill: #6B7F3F; -fx-font-size: 14px; -fx-margin-top: 20;");
        grid.add(section2, 0, 3, 3, 1);

        grid.add(createLabel("Statut:"), 0, 4);
        grid.add(cmbStatutDlg, 1, 4);

        grid.add(createLabel("Priorité:"), 0, 5);
        grid.add(cmbPrioriteDlg, 1, 5);

        grid.add(createLabel("Catégorie:"), 0, 6);
        grid.add(cmbCategorieDlg, 1, 6);

        Label section3 = new Label("📅 DATES");
        section3.setStyle("-fx-font-weight: bold; -fx-text-fill: #6B7F3F; -fx-font-size: 14px; -fx-margin-top: 20;");
        grid.add(section3, 0, 7, 3, 1);

        grid.add(createLabel("Date début *:"), 0, 8);
        grid.add(dpDebut, 1, 8);

        grid.add(createLabel("Date fin:"), 0, 9);
        grid.add(dpFin, 1, 9);
        grid.add(errDates, 2, 9);

        Label section4 = new Label("👤 AFFECTATION");
        section4.setStyle("-fx-font-weight: bold; -fx-text-fill: #6B7F3F; -fx-font-size: 14px; -fx-margin-top: 20;");
        grid.add(section4, 0, 10, 3, 1);

        grid.add(createLabel("Employé * :"), 0, 11);
        grid.add(employeBox, 1, 11, 2, 1);
        grid.add(errEmploye, 1, 12, 2, 1);


//controle saisie
        String styleErr = "-fx-border-color: #e74c3c; -fx-border-radius: 8; -fx-border-width: 2;";
        String styleNormal = "";

        txtTitre.textProperty().addListener((obs, o, n) -> {
            if (n.trim().isEmpty()) {
                txtTitre.setStyle(styleErr);
                errTitre.setText("⚠ Titre requis");
            } else {
                txtTitre.setStyle(styleNormal);
                errTitre.setText("");
            }
            if (okButton != null) {
                updateOkButtonState(okButton, txtTitre, txtDescription, dpDebut, errDates, cmbEmployeDlg);
            }
        });

        txtDescription.textProperty().addListener((obs, o, n) -> {
            if (n.trim().isEmpty()) {
                txtDescription.setStyle(styleErr);
                errDescription.setText("⚠ Description requise");
            } else {
                txtDescription.setStyle(styleNormal);
                errDescription.setText("");
            }
            if (okButton != null) {
                updateOkButtonState(okButton, txtTitre, txtDescription, dpDebut, errDates, cmbEmployeDlg);
            }
        });

        dpDebut.valueProperty().addListener((obs, o, n) -> {
            if (n == null) {
                errDates.setText("⚠ Date début requise");
                if (okButton != null) okButton.setDisable(true);
            } else {
                validateDates(dpDebut, dpFin, errDates, okButton);
            }
            if (okButton != null) {
                updateOkButtonState(okButton, txtTitre, txtDescription, dpDebut, errDates, cmbEmployeDlg);
            }
        });

        dpFin.valueProperty().addListener((obs, o, n) -> {
            validateDates(dpDebut, dpFin, errDates, okButton);
        });

        // ── Validation employe obligatoire ──────────────────────────────
        cmbEmployeDlg.valueProperty().addListener((obs, o, n) -> {
            if (n == null) {
                cmbEmployeDlg.setStyle("-fx-border-color: #e74c3c; -fx-border-radius: 8; -fx-border-width: 2;");
                errEmploye.setText("⚠ Employé requis");
            } else {
                cmbEmployeDlg.setStyle("");
                errEmploye.setText("");
            }
            if (okButton != null) {
                updateOkButtonState(okButton, txtTitre, txtDescription, dpDebut, errDates, cmbEmployeDlg);
            }
        });

        if (okButton != null) {
            updateOkButtonState(okButton, txtTitre, txtDescription, dpDebut, errDates, cmbEmployeDlg);
        }

        // ✅ FIX: Ajouter le contenu du formulaire au dialogue
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != btnOK) return null;

            if (!validateFinal(txtTitre, txtDescription, dpDebut, dpFin, errTitre, errDescription, errDates)) {
                return null;
            }

            // ✔ Double-verification : employe obligatoire
            if (cmbEmployeDlg.getValue() == null) {
                errEmploye.setText("⚠ Vous devez sélectionner un employé !");
                errEmploye.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                cmbEmployeDlg.setStyle("-fx-border-color: #e74c3c; -fx-border-radius: 8; -fx-border-width: 2;");
                return null;
            }

            Tache resultat = tache != null ? tache : new Tache();

            resultat.setTitre(txtTitre.getText().trim());
            resultat.setDescription(txtDescription.getText().trim());
            resultat.setStatut(cmbStatutDlg.getValue());
            resultat.setPriorite(getPrioriteValue(cmbPrioriteDlg.getValue()));
            resultat.setCategorie(cmbCategorieDlg.getValue());
            resultat.setDateDebut(dpDebut.getValue());
            resultat.setDateFin(dpFin.getValue());
            // Récupère l'employé sélectionné (obligatoire)
            Employe emp = cmbEmployeDlg.getValue();
            resultat.setIdEmploye(emp.getId());

            return resultat;
        });

        return dialog;
    }
    //Active/désactive le bouton OK selon la validité du formulaire
//Toutes les conditions doivent être vraies
    private void updateOkButtonState(Button okButton, TextField titre, TextArea desc,
                                     DatePicker debut, Label errDates, ComboBox<Employe> employe) {
        boolean isValid = !titre.getText().trim().isEmpty()
                && !desc.getText().trim().isEmpty()
                && debut.getValue() != null
                && errDates.getText().isEmpty()
                && employe.getValue() != null;   // ✔ Employe obligatoire

        okButton.setDisable(!isValid);
    }

    // Surcharge retrocompatible (sans employe) — ne devrait plus etre appelee
    private void updateOkButtonState(Button okButton, TextField titre, TextArea desc,
                                     DatePicker debut, Label errDates) {
        boolean isValid = !titre.getText().trim().isEmpty()
                && !desc.getText().trim().isEmpty()
                && debut.getValue() != null
                && errDates.getText().isEmpty();
        okButton.setDisable(!isValid);
    }

    private boolean validateFinal(TextField titre, TextArea desc, DatePicker debut, DatePicker fin,
                                  Label errTitre, Label errDesc, Label errDates) {
        boolean valid = true;

        if (titre.getText().trim().isEmpty()) {
            errTitre.setText("⚠ Titre requis");
            valid = false;
        }

        if (desc.getText().trim().isEmpty()) {
            errDesc.setText("⚠ Description requise");
            valid = false;
        }

        if (debut.getValue() == null) {
            errDates.setText("⚠ Date début requise");
            valid = false;
        }

        if (fin.getValue() != null && debut.getValue() != null && fin.getValue().isBefore(debut.getValue())) {
            errDates.setText("⚠ Date fin < date début");
            valid = false;
        }

        return valid;
    }
    //Date fin doit être après date début
    private void validateDates(DatePicker debut, DatePicker fin, Label errDates, Button okButton) {
        if (debut.getValue() == null) {
            errDates.setText("⚠ Date début requise");
            if (okButton != null) okButton.setDisable(true);
            return;
        }

        if (fin.getValue() != null && fin.getValue().isBefore(debut.getValue())) {
            errDates.setText("⚠ Date fin < date début");
            if (okButton != null) okButton.setDisable(true);
            return;
        }

        errDates.setText("");
        if (okButton != null) okButton.setDisable(false);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        return label;
    }

    private Label createErrorLabel() {
        Label label = new Label();
        label.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
        label.setWrapText(true);
        label.setMaxWidth(150);
        return label;
    }

    private ListCell<Employe> createEmployeCell(String defaultText) {
        return new ListCell<Employe>() {
            @Override
            protected void updateItem(Employe item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(defaultText);
                    setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
                } else {
                    setText(item.getNom() + " " + item.getPrenom() + " (" + item.getPoste() + ")");
                    setStyle("");
                }
            }
        };
    }
    //Convertit un nombre en texte de priorité
    private String getPrioriteString(Integer priorite) {
        if (priorite == null) return "Moyenne";
        switch (priorite) {
            case 1: return "Basse";
            case 2: return "Moyenne";
            case 3: return "Haute";
            case 4: return "Critique";
            default: return "Moyenne";
        }
    }
    //Convertit un texte en nombre de priorité (pour BDD)
    private int getPrioriteValue(String priorite) {
        switch (priorite) {
            case "Basse": return 1;
            case "Moyenne": return 2;
            case "Haute": return 3;
            case "Critique": return 4;
            default: return 2;
        }
    }
    //Récupère le nom d'un employé par son ID
//Utilise le cache pour optimiser les performances
    private String getEmployeName(Integer idEmploye) {
        if (idEmploye == null) return "Non assigné";
        Employe emp = cacheManager.getEmploye(idEmploye);
        if (emp == null) {
            emp = employeService.getEmployeById(idEmploye);
            if (emp != null) cacheManager.putEmploye(idEmploye, emp);
        }
        return emp != null ? emp.getNom() + " " + emp.getPrenom() : "ID: " + idEmploye;
    }

    // ── Préférences ─────────────────────────────────────────────────────────
    private void saveFilterState() {
        if (cmbStatut != null) prefs.put("lastStatut", cmbStatut.getValue());
        if (cmbPriorite != null) prefs.put("lastPriorite", cmbPriorite.getValue());
        if (cmbCategorie != null) prefs.put("lastCategorie", cmbCategorie.getValue());
        if (cmbTri != null) prefs.put("lastTri", cmbTri.getValue());
        if (txtRecherche != null) prefs.put("lastRecherche", txtRecherche.getText());
        prefs.putBoolean("triCroissant", triCroissant);
    }

    private String loadFilterPreference(String key, String defaultValue) {
        return prefs.get(key, defaultValue);
    }

    @SuppressWarnings("unused")
    private boolean loadBooleanPreference(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    // ── Retour intelligent avec condition par role ─────────────────────────
    @FXML
    public void handleRetour() {
        try {
            ResourceBundle bundle = LanguageManager.getInstance().getBundle();
            Stage currentStage = (Stage) btnRetour.getScene().getWindow();

            // Determiner la destination selon le role
            Role role = UserSession.getInstance().getUser().getRole();
            String fxmlPath;
            String titre;

            if (role == Role.ADMIN) {
                // Admin → retour vers la liste des agriculteurs
                fxmlPath = "/fxml/gestionemploye/admin_agriculteurs_list.fxml";
                titre = "Gestion des Agriculteurs - Ardhi";
                AgriculteurContext.getInstance().clear(); // Nettoyer le contexte de supervision
            } else {
                // Agriculteur → retour vers son dashboard
                fxmlPath = "/fxml/gestionemploye/Dashboard_employe_agriculture.fxml";
                titre = "Ardhi - Dashboard";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath), bundle);
            if (loader.getLocation() == null) {
                showAlert("Erreur", "Fichier introuvable : " + fxmlPath);
                return;
            }
            Parent root = loader.load();
            Stage destStage = new Stage();
            destStage.setTitle(titre);
            destStage.setScene(new Scene(root));
            destStage.show();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur de navigation", e.getMessage());
        }
    }

    private void showError(String message) { showAlert("Erreur", message); }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private Parent loadFXMLWithResources(String fxmlPath) throws IOException {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.FRENCH);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath), bundle);
            return loader.load();
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement ressources pour: " + fxmlPath);
            e.printStackTrace();
            // Fallback: charger sans ressources
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            return loader.load();
        }
    }


    // ═══════════════════════════════════════════════════════════════════════
    // PLANIFICATION SEMAINE + GOOGLE CALENDAR (OAuth2)
    // ═══════════════════════════════════════════════════════════════════════

    /** Ouvre la fenetre de planification semaine avec vue grille + Google Calendar */
    private void ouvrirPlanificationSemaine() {
        Stage stage = new Stage();
        stage.setTitle("📅 Planification de la Semaine");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setMinWidth(1100); stage.setMinHeight(700);

        final LocalDate[] lundi = {
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        };

        // ── ROOT ─────────────────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#f0f4f8;");

        // ── EN-TETE ───────────────────────────────────────────────────────
        HBox header = new HBox(10);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:linear-gradient(to right,#27ae60,#1a5c38);");

        Label lblTitre = new Label("📅  PLANIFICATION DES TÂCHES");
        lblTitre.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;");

        Label lblSemaine = new Label();
        lblSemaine.setStyle("-fx-font-size:12px;-fx-text-fill:#d4f1e4;-fx-font-weight:bold;");

        // Indicateur statut Google Calendar
        Label lblGCal = new Label("⬤ Google Calendar : vérification...");
        lblGCal.setStyle("-fx-font-size:10px;-fx-text-fill:#aed6f1;");

        Button btnPrev   = pBtn("◀ Préc.");
        Button btnNext   = pBtn("Suiv. ▶");
        Button btnAuj    = pBtn("📍 Auj.");
        Button btnSync   = new Button("🔄 Sync Google");
        btnSync.setStyle("-fx-background-color:#4285f4;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:7 12;-fx-cursor:hand;");
        Button btnPush   = new Button("⬆ Envoyer semaine");
        btnPush.setStyle("-fx-background-color:#34a853;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:7 12;-fx-cursor:hand;");
        Button btnICS    = new Button("⬇ Export .ics");
        btnICS.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:7 12;-fx-cursor:hand;");
        Button btnGCLink = new Button("🌐 Ouvrir GCal");
        btnGCLink.setStyle("-fx-background-color:#ea4335;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:7 12;-fx-cursor:hand;");
        Button btnNettoyer = new Button("🧹 Nettoyer GCal");
        btnNettoyer.setStyle("-fx-background-color:#7f8c8d;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:7 12;-fx-cursor:hand;");
        btnNettoyer.setTooltip(new Tooltip("Supprime de Google Calendar les \u00e9v\u00e9nements dont la t\u00e2che a \u00e9t\u00e9 supprim\u00e9e"));
        Button btnClose  = pBtn("✕");

        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region(); sp2.setPrefWidth(8);
        header.getChildren().addAll(lblTitre, btnPrev, lblSemaine, btnNext, btnAuj,
                sp1, lblGCal, sp2, btnSync, btnPush, btnNettoyer, btnICS, btnGCLink, btnClose);

        // ── SCROLL GRILLE ─────────────────────────────────────────────────
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        final VBox[] gHolder = {new VBox()};
        scroll.setContent(gHolder[0]);

        Runnable refresh = () -> {
            LocalDate l = lundi[0];
            lblSemaine.setText("  " + l.format(DateTimeFormatter.ofPattern("dd/MM"))
                    + " → " + l.plusDays(6).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "  ");
            VBox g = buildWeekGrid(l);
            gHolder[0] = g;
            scroll.setContent(g);
        };

        // ── Navigation ────────────────────────────────────────────────────
        btnPrev.setOnAction(e -> { lundi[0] = lundi[0].minusWeeks(1); refresh.run(); });
        btnNext.setOnAction(e -> { lundi[0] = lundi[0].plusWeeks(1);  refresh.run(); });
        btnAuj.setOnAction(e  -> {
            lundi[0] = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            refresh.run();
        });

        // ── ICS Export ─────────────────────────────────────────────────────
        btnICS.setOnAction(e -> exporterICS(lundi[0], stage));

        // ── Ouvrir Google Calendar dans navigateur ─────────────────────────
        btnGCLink.setOnAction(e -> {
            try {
                String url = "https://calendar.google.com/calendar/r/week/"
                        + lundi[0].getYear() + "/" + lundi[0].getMonthValue() + "/" + lundi[0].getDayOfMonth();
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) { showError("Navigateur : " + ex.getMessage()); }
        });

        // ── Sync Google Calendar (Pull) ────────────────────────────────────
        btnSync.setOnAction(e -> syncDepuisGoogle(lundi[0], lblGCal, refresh));

        // ── Push tâches semaine → Google Calendar ─────────────────────────
        btnPush.setOnAction(e -> envoyerSemaineGoogle(lundi[0], lblGCal));

        // ── Nettoyer les événements orphelins dans Google Calendar ─────────
        btnNettoyer.setOnAction(e -> nettoyerGoogleCalendar(lblGCal));

        btnClose.setOnAction(e -> stage.close());

        root.getChildren().addAll(header, scroll);
        stage.setScene(new Scene(root, 1280, 790));

        // Vérifier connexion Google Calendar en arrière-plan
        javafx.concurrent.Task<Boolean> checkTask = new javafx.concurrent.Task<>() {
            @Override protected Boolean call() {
                return GoogleCalendarService.getInstance().connecter();
            }
        };
        checkTask.setOnSucceeded(ev -> {
            boolean ok = checkTask.getValue();
            lblGCal.setText(ok ? "⬤ Google Calendar : connecté" : "⬤ Google Calendar : non connecté");
            lblGCal.setStyle("-fx-font-size:10px;-fx-text-fill:" + (ok ? "#a9dfbf" : "#f1948a") + ";");
            btnSync.setDisable(!ok);
            btnPush.setDisable(!ok);
            btnNettoyer.setDisable(!ok);
            if (ok) {
                String nom = GoogleCalendarService.getInstance().getNomCalendrier();
                if (nom != null) lblGCal.setText("⬤ " + nom);
            }
        });
        btnSync.setDisable(true); btnPush.setDisable(true); btnNettoyer.setDisable(true);
        new Thread(checkTask).start();

        refresh.run();
        stage.show();
    }

    // ── Grille 7 colonnes ─────────────────────────────────────────────────────

    private VBox buildWeekGrid(LocalDate lundi) {
        VBox outer = new VBox(0);

        String[] NOM_JOURS = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche"};
        String[] EMOJIS    = {"💼","📋","🌱","🔧","✅","🌿","☀"};

        // En-têtes
        GridPane hdr = new GridPane();
        hdr.setStyle("-fx-background-color:#2c3e50;");
        for (int j = 0; j < 7; j++) {
            LocalDate jour = lundi.plusDays(j);
            boolean today  = jour.equals(LocalDate.now());
            VBox ch = new VBox(2);
            ch.setAlignment(Pos.CENTER);
            ch.setPadding(new Insets(10, 4, 10, 4));
            ch.setPrefWidth(170);
            ch.setStyle(today ? "-fx-background-color:#27ae60;" : "-fx-background-color:#34495e;");
            Label j1 = new Label(EMOJIS[j] + " " + NOM_JOURS[j]);
            j1.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12px;");
            Label j2 = new Label(jour.format(DateTimeFormatter.ofPattern("dd/MM")));
            j2.setStyle("-fx-text-fill:" + (today ? "#d4f1e4" : "#95a5a6") + ";-fx-font-size:11px;");
            ch.getChildren().addAll(j1, j2);
            hdr.add(ch, j, 0);
            GridPane.setHgrow(ch, Priority.ALWAYS);
        }

        // Corps
        GridPane grille = new GridPane();
        grille.setHgap(1); grille.setVgap(1);
        grille.setStyle("-fx-background-color:#dee2e6;-fx-padding:1;");

        long totalSem = 0;
        for (int j = 0; j < 7; j++) {
            LocalDate jour = lundi.plusDays(j);
            boolean today  = jour.equals(LocalDate.now());
            VBox col = new VBox(6);
            col.setPrefWidth(170); col.setMinHeight(430);
            col.setPadding(new Insets(10, 8, 10, 8));
            col.setStyle("-fx-background-color:" + (today ? "#f0fff4" : "white") + ";");

            long count = 0;
            for (Tache t : masterList) {
                if (t.getDateDebut() == null) continue;
                LocalDate fin = t.getDateFin() != null ? t.getDateFin() : t.getDateDebut();
                if (!jour.isBefore(t.getDateDebut()) && !jour.isAfter(fin)) {
                    col.getChildren().add(cardTache(t));
                    count++; totalSem++;
                }
            }
            if (count == 0) {
                Label v = new Label("Aucune tâche");
                v.setStyle("-fx-text-fill:#bdc3c7;-fx-font-style:italic;-fx-font-size:11px;");
                col.getChildren().add(v);
            }
            grille.add(col, j, 0);
            GridPane.setHgrow(col, Priority.ALWAYS);
        }

        // Barre stats
        long enCours   = masterList.stream().filter(t->"En cours".equals(t.getStatut())).count();
        long terminees = masterList.stream().filter(t->"Terminé".equals(t.getStatut())||"Validé".equals(t.getStatut())).count();
        long enAttente = masterList.stream().filter(t->"En attente".equals(t.getStatut())).count();
        long ft = totalSem;
        HBox stats = new HBox(14);
        stats.setPadding(new Insets(10, 20, 10, 20));
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.setStyle("-fx-background-color:#ecf0f1;-fx-border-color:#dee2e6;-fx-border-width:1 0 0 0;");
        stats.getChildren().addAll(
                statChip("📌 Semaine : " + ft, "#2c3e50"),
                statChip("🔵 En cours : " + enCours, "#2980b9"),
                statChip("✅ Terminées : " + terminees, "#27ae60"),
                statChip("⏳ En attente : " + enAttente, "#f39c12")
        );

        outer.getChildren().addAll(hdr, grille, stats);
        return outer;
    }

    private VBox cardTache(Tache t) {
        VBox c = new VBox(4);
        c.setPadding(new Insets(8, 10, 8, 10));
        String bg  = gcBg(t.getPriorite()), brd = gcBrd(t.getPriorite());
        c.setStyle("-fx-background-color:"+bg+";-fx-background-radius:8;"
                +"-fx-border-color:"+brd+";-fx-border-width:0 0 0 4;-fx-border-radius:0 8 8 0;"
                +"-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),4,0,0,2);-fx-cursor:hand;");

        Label lt = new Label(gcEmoji(t.getStatut()) + " " + t.getTitre());
        lt.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-wrap-text:true;-fx-text-fill:#2c3e50;");
        lt.setMaxWidth(155); lt.setWrapText(true);

        Label le = new Label(t.getIdEmploye()!=null?"👤 Emp. #"+t.getIdEmploye():"👥 Non assigné");
        le.setStyle("-fx-font-size:10px;-fx-text-fill:#7f8c8d;");

        HBox tags = new HBox(4);
        if (t.getPriorite() != null) {
            Label lp = new Label(gcLblPrio(t.getPriorite()));
            lp.setStyle("-fx-background-color:"+brd+";-fx-text-fill:white;-fx-background-radius:4;-fx-padding:2 6;-fx-font-size:9px;-fx-font-weight:bold;");
            tags.getChildren().add(lp);
        }
        Label ls = new Label(t.getStatut()!=null?t.getStatut():"—");
        ls.setStyle("-fx-background-color:#ecf0f1;-fx-text-fill:#555;-fx-background-radius:4;-fx-padding:2 6;-fx-font-size:9px;");
        tags.getChildren().add(ls);

        c.getChildren().addAll(lt, le, tags);
        if (t.getCategorie()!=null&&!t.getCategorie().isBlank()) {
            Label lc = new Label("🏷 "+t.getCategorie());
            lc.setStyle("-fx-font-size:9px;-fx-text-fill:#95a5a6;");
            c.getChildren().add(lc);
        }
        if (t.getDescription()!=null&&!t.getDescription().isBlank()) {
            Tooltip tip = new Tooltip(t.getDescription());
            tip.setWrapText(true); tip.setMaxWidth(280);
            Tooltip.install(c, tip);
        }
        c.setOnMouseEntered(e->c.setOpacity(0.8));
        c.setOnMouseExited(e->c.setOpacity(1.0));
        return c;
    }

    // ── Helpers visuels ──────────────────────────────────────────────────────
    private String gcBg(Integer p)  { if(p==null)return"#fafafa"; return switch(p){case 4->"#fff5f5";case 3->"#fff9e6";case 2->"#f0f9ff";default->"#f7fff7";}; }
    private String gcBrd(Integer p) { if(p==null)return"#bdc3c7"; return switch(p){case 4->"#e74c3c";case 3->"#e67e22";case 2->"#3498db";default->"#27ae60";}; }
    private String gcLblPrio(Integer p) { if(p==null)return"—"; return switch(p){case 4->"🔴 Critique";case 3->"🟠 Haute";case 2->"🔵 Moyenne";default->"🟢 Basse";}; }
    private String gcEmoji(String s) { if(s==null)return"⬜"; return switch(s){case"En cours"->"🔵";case"Terminé"->"✅";case"Validé"->"✔";case"Annulé"->"❌";default->"⏳";}; }
    private Button pBtn(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:7 11;-fx-cursor:hand;-fx-font-size:11px;");
        b.setOnMouseEntered(e->b.setOpacity(0.75)); b.setOnMouseExited(e->b.setOpacity(1.0));
        return b;
    }
    private Label statChip(String txt, String col) {
        Label l = new Label(txt);
        l.setStyle("-fx-text-fill:"+col+";-fx-font-size:11px;-fx-font-weight:bold;"
                +"-fx-background-color:white;-fx-background-radius:6;-fx-padding:4 10;"
                +"-fx-border-color:"+col+";-fx-border-width:1;-fx-border-radius:6;");
        return l;
    }

    // ── SYNC : Google Calendar → Ardhi (Pull) ────────────────────────────────

    /**
     * Récupère les événements Google Calendar de la semaine et les affiche,
     * en filtrant uniquement les événements correspondant aux tâches de l'agriculteur actif.
     */
    private void syncDepuisGoogle(LocalDate lundi, Label lblStatus, Runnable refresh) {
        lblStatus.setText("⬤ Synchronisation...");
        lblStatus.setStyle("-fx-font-size:10px;-fx-text-fill:#f0e68c;");

        javafx.concurrent.Task<List<Event>> task = new javafx.concurrent.Task<>() {
            @Override protected List<Event> call() {
                return GoogleCalendarService.getInstance().getEvenements(lundi, lundi.plusDays(6));
            }
        };

        task.setOnSucceeded(e -> {
            List<Event> allEvents = task.getValue();

            // ── Filtrer : ne garder que les événements liés aux tâches du contexte actuel ──
            // On construit deux ensembles pour la correspondance :
            // 1) Les googleEventId connus (persistés en base) → correspondance exacte
            // 2) Les titres des tâches → correspondance par le résumé Google "[Ardhi] titre"
            java.util.Set<String> knownEventIds = new java.util.HashSet<>();
            java.util.Set<String> knownTitles   = new java.util.HashSet<>();
            for (Tache t : masterList) {
                if (t.getGoogleEventId() != null && !t.getGoogleEventId().isBlank())
                    knownEventIds.add(t.getGoogleEventId());
                if (t.getTitre() != null)
                    knownTitles.add(t.getTitre().trim().toLowerCase());
            }

            List<Event> events = allEvents.stream()
                    .filter(ev -> {
                        // Correspondance par ID
                        if (ev.getId() != null && knownEventIds.contains(ev.getId())) return true;
                        // Correspondance par titre : le résumé contient "[Ardhi] <titre_tache>"
                        String summary = ev.getSummary();
                        if (summary == null) return false;
                        String lc = summary.toLowerCase();
                        // Vérifier que c'est un événement Ardhi
                        if (!lc.contains("[ardhi]")) return false;
                        // Extraire la partie après [ardhi]
                        int idx = lc.indexOf("[ardhi]");
                        String titlePart = lc.substring(idx + "[ardhi]".length()).trim();
                        return knownTitles.contains(titlePart);
                    })
                    .collect(Collectors.toList());

            lblStatus.setText("⬤ Sync OK : " + events.size() + " evt(s) Google");
            lblStatus.setStyle("-fx-font-size:10px;-fx-text-fill:#a9dfbf;");

            if (events.isEmpty()) {
                NotificationToast.showNotification(
                        "📅 Aucun événement Google Calendar correspondant cette semaine",
                        NotificationToast.SUCCESS, 2500);
                return;
            }

            // Afficher dialog avec les événements filtrés
            Stage dlg = new Stage();
            dlg.setTitle("📅 Événements Google Calendar - semaine");
            dlg.initModality(Modality.APPLICATION_MODAL);

            VBox content = new VBox(8);
            content.setPadding(new Insets(16));
            content.setStyle("-fx-background-color:#f8f9fa;");

            Label titre = new Label("📅 " + events.size() + " événement(s) cette semaine dans Google Calendar");
            titre.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#2c3e50;");
            content.getChildren().add(titre);

            for (Event ev : events) {
                HBox row = new HBox(10);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color:white;-fx-background-radius:8;"
                        +"-fx-border-color:#dee2e6;-fx-border-width:1;-fx-border-radius:8;");
                row.setAlignment(Pos.CENTER_LEFT);

                String dateStr = "";
                if (ev.getStart() != null) {
                    if (ev.getStart().getDate() != null)
                        dateStr = ev.getStart().getDate().toString();
                    else if (ev.getStart().getDateTime() != null)
                        dateStr = ev.getStart().getDateTime().toString().substring(0,10);
                }

                Label d = new Label("📅 " + dateStr);
                d.setStyle("-fx-font-size:11px;-fx-text-fill:#7f8c8d;-fx-min-width:90;");
                Label s = new Label(ev.getSummary() != null ? ev.getSummary() : "(sans titre)");
                s.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-text-fill:#2c3e50;");
                s.setMaxWidth(350); s.setWrapText(true);
                row.getChildren().addAll(d, s);
                content.getChildren().add(row);
            }

            Button close = new Button("Fermer");
            close.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-weight:bold;"
                    +"-fx-background-radius:8;-fx-padding:8 20;-fx-cursor:hand;");
            close.setOnAction(ev -> dlg.close());
            HBox btnRow = new HBox(close);
            btnRow.setAlignment(Pos.CENTER);
            btnRow.setPadding(new Insets(8, 0, 0, 0));
            content.getChildren().add(btnRow);

            ScrollPane sp = new ScrollPane(content);
            sp.setFitToWidth(true);
            sp.setPrefHeight(Math.min(events.size() * 60 + 100, 500));
            dlg.setScene(new Scene(sp, 600, Math.min(events.size() * 70 + 120, 520)));
            dlg.show();
        });

        task.setOnFailed(e -> {
            lblStatus.setText("⬤ Erreur sync");
            lblStatus.setStyle("-fx-font-size:10px;-fx-text-fill:#f1948a;");
            showError("Erreur sync : " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    // ── NETTOYAGE : Supprimer les événements orphelins de Google Calendar ─────

    /**
     * Détecte et supprime depuis Google Calendar tous les événements [Ardhi]
     * dont la tâche correspondante a été supprimée en base de données.
     */
    private void nettoyerGoogleCalendar(Label lblStatus) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("🧹 Nettoyage Google Calendar");
        confirm.setHeaderText("Supprimer les événements obsolètes de Google Calendar ?");
        confirm.setContentText(
                "Cette opération va scanner tous les événements [Ardhi] dans Google Calendar\n" +
                "et supprimer ceux dont la tâche a été supprimée de la base de données.\n\n" +
                "Continuer ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        lblStatus.setText("⬤ Nettoyage en cours...");
        lblStatus.setStyle("-fx-font-size:10px;-fx-text-fill:#f0e68c;");

        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() {
                java.util.Set<Integer> idsExistants = tacheService.getAllTaskIds();
                return GoogleCalendarService.getInstance().nettoyerEvenementsOrphelins(idsExistants);
            }
        };

        task.setOnSucceeded(e -> {
            int nb = task.getValue();
            lblStatus.setText("⬤ Nettoyage terminé : " + nb + " supprimé(s)");
            lblStatus.setStyle("-fx-font-size:10px;-fx-text-fill:#a9dfbf;");
            if (nb > 0) {
                NotificationToast.showNotification(
                        "🧹 " + nb + " événement(s) obsolète(s) supprimé(s) de Google Calendar !",
                        NotificationToast.SUCCESS, 4000);
            } else {
                NotificationToast.showNotification(
                        "✅ Google Calendar est déjà propre ! Aucun orphelin trouvé.",
                        NotificationToast.INFO, 3000);
            }
        });

        task.setOnFailed(e -> {
            lblStatus.setText("⬤ Erreur nettoyage");
            lblStatus.setStyle("-fx-font-size:10px;-fx-text-fill:#f1948a;");
            showError("Erreur nettoyage : " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    // ── PUSH : Ardhi → Google Calendar ───────────────────────────────────────

    /**
     * Envoie toutes les tâches de la semaine vers Google Calendar.
     * Crée un événement par tâche ayant une date de début.
     */
    private void envoyerSemaineGoogle(LocalDate lundi, Label lblStatus) {
        LocalDate dim = lundi.plusDays(6);
        List<Tache> semaine = masterList.stream()
                .filter(t -> t.getDateDebut() != null)
                .filter(t -> !t.getDateDebut().isBefore(lundi) && !t.getDateDebut().isAfter(dim))
                .collect(Collectors.toList());

        if (semaine.isEmpty()) {
            NotificationToast.showNotification(
                    "⚠ Aucune tâche avec date début cette semaine",
                    NotificationToast.WARNING, 2500);
            return;
        }

        // Confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Envoyer vers Google Calendar");
        confirm.setHeaderText("⬆ Exporter " + semaine.size() + " tâche(s) vers Google Calendar");
        confirm.setContentText(
                "Les tâches suivantes seront créées comme événements :\n\n"
                        + semaine.stream()
                        .limit(5)
                        .map(t -> "• " + t.getTitre() + " (" + t.getDateDebut() + ")")
                        .collect(Collectors.joining("\n"))
                        + (semaine.size() > 5 ? "\n... et " + (semaine.size()-5) + " autre(s)" : "")
                        + "\n\nContinuer ?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            lblStatus.setText("⬤ Envoi en cours...");
            lblStatus.setStyle("-fx-font-size:10px;-fx-text-fill:#f0e68c;");

            javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
                @Override protected Integer call() {
                    return GoogleCalendarService.getInstance().creerEvenements(semaine);
                }
            };

            task.setOnSucceeded(e -> {
                int nb = task.getValue();
                lblStatus.setText("⬤ " + nb + "/" + semaine.size() + " evt(s) envoyés");
                lblStatus.setStyle("-fx-font-size:10px;-fx-text-fill:#a9dfbf;");
                NotificationToast.showNotification(
                        "✅ " + nb + " tâche(s) envoyée(s) vers Google Calendar !",
                        NotificationToast.SUCCESS, 3500);
            });

            task.setOnFailed(e -> {
                lblStatus.setText("⬤ Erreur envoi");
                lblStatus.setStyle("-fx-font-size:10px;-fx-text-fill:#f1948a;");
                showError("Erreur : " + task.getException().getMessage());
            });

            new Thread(task).start();
        });
    }
    /**
     * Synchroniser TOUTES les tâches vers Google Calendar
     * Bouton dans la vue Gestion des Tâches
     */
    @FXML
    private void handleSyncGoogleAll() {
        GoogleCalendarService gcal = GoogleCalendarService.getInstance();

        // Vérifier connexion
        if (!gcal.connecter()) {
            showError("Connexion Google Calendar échouée.\n" + gcal.getLastError());
            return;
        }

        // Récupérer toutes les tâches
        List<Tache> toutes = masterList.stream()
                .filter(t -> t.getDateDebut() != null) // Seulement celles avec une date
                .collect(Collectors.toList());

        if (toutes.isEmpty()) {
            NotificationToast.showNotification(
                    "⚠ Aucune tâche avec date de début à synchroniser",
                    NotificationToast.WARNING, 2500);
            return;
        }

        // Confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Synchronisation Google Calendar");
        confirm.setHeaderText("📤 Synchroniser " + toutes.size() + " tâche(s)");
        confirm.setContentText(
                "Les tâches suivantes seront envoyées vers Google Calendar :\n\n"
                        + toutes.stream()
                        .limit(5)
                        .map(t -> "• " + t.getTitre() + " (" + t.getDateDebut() + ")")
                        .collect(Collectors.joining("\n"))
                        + (toutes.size() > 5 ? "\n... et " + (toutes.size()-5) + " autre(s)" : "")
                        + "\n\nContinuer ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        // Synchronisation en arrière-plan
        javafx.concurrent.Task<Integer> task = new javafx.concurrent.Task<>() {
            @Override
            protected Integer call() {
                System.out.println("[Sync] Début synchronisation de " + toutes.size() + " tâches...");
                return gcal.creerEvenements(toutes);
            }
        };

        task.setOnSucceeded(e -> {
            int nb = task.getValue();
            NotificationToast.showNotification(
                    "✅ " + nb + "/" + toutes.size() + " tâche(s) synchronisée(s) !",
                    NotificationToast.SUCCESS, 3500);
            System.out.println("[Sync] Terminé : " + nb + " tâches synchronisées");
        });

        task.setOnFailed(e -> {
            showError("Erreur synchronisation : " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }
    // ═══════════════════════════════════════════════════════════════════════
    // EXPORT .ICS  (fallback sans OAuth : Google · Outlook · Apple)
    // ═══════════════════════════════════════════════════════════════════════

    private void exporterICS(LocalDate lundi, Stage parent) {
        LocalDate dim = lundi.plusDays(6);
        List<Tache> semaine = masterList.stream()
                .filter(t -> t.getDateDebut() != null)
                .filter(t -> !t.getDateDebut().isBefore(lundi) && !t.getDateDebut().isAfter(dim))
                .collect(Collectors.toList());

        if (semaine.isEmpty()) {
            NotificationToast.showNotification("⚠ Aucune tâche avec date début cette semaine",
                    NotificationToast.WARNING, 2500);
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter vers calendrier (.ics)");
        fc.setInitialFileName("ardhi_semaine_" + lundi.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".ics");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("iCalendar (.ics)", "*.ics"));
        File f = fc.showSaveDialog(parent);
        if (f == null) return;

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("BEGIN:VCALENDAR\r\n");
            sb.append("VERSION:2.0\r\n");
            sb.append("PRODID:-//Ardhi//Gestion Employes//FR\r\n");
            sb.append("CALSCALE:GREGORIAN\r\n");
            sb.append("METHOD:PUBLISH\r\n");
            sb.append("X-WR-CALNAME:Ardhi - Planification\r\n");
            sb.append("X-WR-TIMEZONE:Africa/Tunis\r\n");
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));

            for (Tache t : semaine) {
                LocalDate deb = t.getDateDebut();
                LocalDate fin = (t.getDateFin()!=null ? t.getDateFin() : deb).plusDays(1);
                String prio = t.getPriorite()==null?"5":switch(t.getPriorite()){case 4->"1";case 3->"3";case 2->"5";default->"9";};
                String desc = icsEsc(
                        (t.getDescription()!=null?t.getDescription():"")
                                + "\\nStatut: "+(t.getStatut()!=null?t.getStatut():"—")
                                + "\\nPriorité: "+gcLblPrio(t.getPriorite())
                                + "\\nCatégorie: "+(t.getCategorie()!=null?t.getCategorie():"—")
                                + (t.getIdEmploye()!=null?"\\nEmployé #"+t.getIdEmploye():"")
                                + "\\n\\nGénéré par Ardhi");

                sb.append("BEGIN:VEVENT\r\n");
                sb.append("UID:ardhi-").append(t.getId()).append("-").append(System.nanoTime()).append("@ardhi.tn\r\n");
                sb.append("DTSTAMP:").append(ts).append("\r\n");
                sb.append("DTSTART;VALUE=DATE:").append(deb.format(DateTimeFormatter.ofPattern("yyyyMMdd"))).append("\r\n");
                sb.append("DTEND;VALUE=DATE:").append(fin.format(DateTimeFormatter.ofPattern("yyyyMMdd"))).append("\r\n");
                sb.append("SUMMARY:").append(icsEsc(gcEmoji(t.getStatut())+" "+t.getTitre())).append("\r\n");
                sb.append("DESCRIPTION:").append(desc).append("\r\n");
                if (t.getCategorie()!=null) sb.append("CATEGORIES:").append(t.getCategorie()).append("\r\n");
                sb.append("PRIORITY:").append(prio).append("\r\n");
                sb.append("STATUS:").append(icsStatut(t.getStatut())).append("\r\n");
                sb.append("COLOR:").append(icsColor(t.getPriorite())).append("\r\n");
                sb.append("END:VEVENT\r\n");
            }
            sb.append("END:VCALENDAR\r\n");

            java.nio.file.Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            NotificationToast.showNotification("✅ "+semaine.size()+" tâche(s) exportée(s) en .ics",
                    NotificationToast.SUCCESS, 3000);

            Alert c2 = new Alert(Alert.AlertType.CONFIRMATION);
            c2.setTitle("Export réussi"); c2.setHeaderText("✅ Fichier .ics créé");
            c2.setContentText(semaine.size()+" tâche(s) exportée(s).\nOuvrir maintenant ?");
            c2.showAndWait().ifPresent(b -> {
                if (b==ButtonType.OK) try { Desktop.getDesktop().open(f); }
                catch(Exception ex){ showError("Ouvrir : "+ex.getMessage()); }
            });
        } catch(Exception e) { showError("Export ICS : "+e.getMessage()); }
    }

    private String icsEsc(String s) {
        if(s==null)return"";
        return s.replace("\\","\\\\").replace(";","\\;").replace(",","\\,").replace("\n","\\n");
    }
    private String icsStatut(String s) {
        if(s==null)return"NEEDS-ACTION";
        return switch(s){case"En cours"->"IN-PROCESS";case"Terminé","Validé"->"COMPLETED";case"Annulé"->"CANCELLED";default->"NEEDS-ACTION";};
    }
    private String icsColor(Integer p) {
        if(p==null)return"auto";
        return switch(p){case 4->"tomato";case 3->"tangerine";case 2->"peacock";default->"sage";};
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  🔮 MODULE IA — Prédiction de risque de retard
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Configure le bouton "🔮 Analyse IA".
     * Il se désactive automatiquement si aucune tâche n'est sélectionnée.
     */
    private void setupBtnAnalyseIA() {
        if (btnAnalyseIA == null) return;

        // Style de base (désactivé)
        btnAnalyseIA.setDisable(true);
        appliquerStyleIA(false);

        // Active/désactive selon la sélection de la table
        tableTaches.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean actif = sel != null;
            btnAnalyseIA.setDisable(!actif);
            appliquerStyleIA(actif);
        });

        // Hover effects
        btnAnalyseIA.setOnMouseEntered(e -> {
            if (!btnAnalyseIA.isDisabled())
                btnAnalyseIA.setStyle(btnAnalyseIA.getStyle()
                        .replace("#6c3483", "#8e44ad").replace("#4a235a", "#8e44ad"));
        });
        btnAnalyseIA.setOnMouseExited(e -> {
            if (!btnAnalyseIA.isDisabled()) appliquerStyleIA(true);
        });

        btnAnalyseIA.setOnAction(e -> handleAnalyseIA());
    }

    private void appliquerStyleIA(boolean actif) {
        btnAnalyseIA.setStyle(
                "-fx-background-color: " + (actif ? "#6c3483" : "#4a235a") + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 13; " +
                        "-fx-padding: 8 18 8 18; " +
                        "-fx-opacity: " + (actif ? "1.0" : "0.5") + "; " +
                        "-fx-cursor: " + (actif ? "hand" : "default") + ";"
        );
    }

    /**
     * Lance l'analyse IA sur la tâche sélectionnée.
     * Ouvre la fenêtre RiskPredictionDialog avec le résultat.
     */
    @FXML
    private void handleAnalyseIA() {
        Tache tache = tableTaches.getSelectionModel().getSelectedItem();
        if (tache == null) {
            NotificationToast.showNotification(
                    "⚠️ Sélectionnez une tâche à analyser", NotificationToast.WARNING, 3000);
            return;
        }

        // Récupérer l'employé assigné (peut être null)
        Employe employe = null;
        if (tache.getIdEmploye() != null && employeService != null) {
            try {
                employe = employeService.getEmployeById(tache.getIdEmploye());
            } catch (Exception ex) {
                System.err.println("⚠️ Employé introuvable id=" + tache.getIdEmploye());
            }
        }

        NotificationToast.showNotification(
                "🔮 Analyse IA en cours...", NotificationToast.INFO, 1500);

        final Employe emp = employe;
        javafx.application.Platform.runLater(() ->
                RiskPredictionDialog.show(tache, emp)
        );
    }

}