package tn.neuron.ardhi.controllers.gestionemployecontroller;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javafx.animation.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;


import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.models.gestionemployemodel.Employe;
import tn.neuron.ardhi.models.gestionemployemodel.Tache;
import tn.neuron.ardhi.services.gestionemployeservice.EmployeService;
import tn.neuron.ardhi.services.gestionemployeservice.PerformanceService;
import tn.neuron.ardhi.services.gestionemployeservice.TacheService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import tn.neuron.ardhi.utils.gestionemployeutils.AgriculteurContext;
import tn.neuron.ardhi.utils.gestionemployeutils.CacheManager;
import tn.neuron.ardhi.utils.gestionemployeutils.LanguageManager;
import tn.neuron.ardhi.utils.gestionemployeutils.NotificationToast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import tn.neuron.ardhi.services.gestionemployeservice.AttestationMailService;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EmployeController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private TableView<Employe> tableEmployees;
    @FXML private TableColumn<Employe, Integer> colId;
    @FXML private TableColumn<Employe, String> colNom;
    @FXML private TableColumn<Employe, String> colPrenom;
    @FXML private TableColumn<Employe, String> colEmail;
    @FXML private TableColumn<Employe, String> colPoste;
    @FXML private TableColumn<Employe, String> colTelephone;
    @FXML private TableColumn<Employe, Boolean> colActif;
    @FXML private TableColumn<Employe, Void> colPerformance;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> cmbTri;
    @FXML private Label lblTotalEmployes;
    @FXML private Label lblActifs;
    @FXML private Label lblSelection;
    @FXML private Label lblTempsChargement;
    @FXML private Button btnRetour;
    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnExporterPDF;
    @FXML private Button btnActualiser;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox statsContainer;
    @FXML private Label lblTop1, lblTop2, lblTop3; // Pour le classement

    // ── Services & State ─────────────────────────────────────────────────────
    private EmployeService employeService;
    private TacheService tacheService;
    private PerformanceService performanceService;
    private final AttestationMailService attestationMailService = new AttestationMailService();
    private CacheManager cacheManager;
    private ObservableList<Employe> masterList;
    private FilteredList<Employe> filteredList;
    private SortedList<Employe> sortedList;
    private boolean triCroissant = true;
    private Stack<Employe> deletedEmployesStack = new Stack<>();
    private Preferences prefs;
    private Map<Integer, PerformanceService.PerformanceData> performanceCache = new HashMap<>();

    // ── Constantes ───────────────────────────────────────────────────────────
    private static final String[] CRITERES_TRI = {"ID", "Nom", "Prénom", "Email", "Poste", "Actif"};
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9]{8}$"   // Exactement 8 chiffres (format tunisien)
    );

    // ── Initialize ───────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        UserSession session = UserSession.getInstance();

        if (session == null || session.getUser() == null) {
            showAccessDeniedAlert("Session expirée",
                    "Veuillez vous reconnecter pour accéder à cette page.");
            return;
        }

        Role userRole = session.getUser().getRole();
        if (userRole != Role.ADMIN && userRole != Role.AGRICULTEUR) {
            showAccessDeniedAlert("Accès refusé",
                    "Ce service est dédié uniquement aux administrateurs et aux agriculteurs.");
            return;
        }

        long startTime = System.currentTimeMillis();

        employeService = new EmployeService();
        tacheService = new TacheService();
        performanceService = new PerformanceService();
        cacheManager = CacheManager.getInstance();
        masterList = FXCollections.observableArrayList();
        prefs = Preferences.userNodeForPackage(EmployeController.class);

        setupUI();
        setupTableColumns();
        setupComboBoxes();
        setupFilteredAndSortedLists();
        setupTableSelectionListener();
        setupKeyboardShortcuts();
        setupAnimations();

        loadEmployesAsync();

        long endTime = System.currentTimeMillis();
        if (lblTempsChargement != null) {
            LanguageManager lm = LanguageManager.getInstance();
            lblTempsChargement.setText(lm.get("common.loading.time").replace("{0}", String.valueOf(endTime - startTime)));
        }

        javafx.application.Platform.runLater(() -> {
            if (tableEmployees != null && tableEmployees.getScene() != null) {
                LanguageManager.getInstance().applyOrientation(
                        (Parent) tableEmployees.getScene().getRoot()
                );
            }
        });
    }

    private void setupUI() {
        if (progressIndicator != null) progressIndicator.setVisible(false);
        if (tableEmployees != null) {
            tableEmployees.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px; -fx-background-color: white;");
            tableEmployees.setRowFactory(tv -> {
                TableRow<Employe> row = new TableRow<>();
                row.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px;");
                row.itemProperty().addListener((obs, oldItem, newItem) -> {
                    if (newItem != null) row.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px;");
                });
                return row;
            });
        }
    }

    // Colonne avatar photo (ajoutee dynamiquement)
    private TableColumn<Employe, Void> colPhoto;

    private void setupTableColumns() {
        // Avatar circulaire en premiere colonne
        colPhoto = new TableColumn<>("");
        colPhoto.setPrefWidth(100);
        colPhoto.setResizable(false);
        colPhoto.setSortable(false);
        colPhoto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow()==null || getTableRow().getItem()==null) { setGraphic(null); return; }
                setGraphic(creerAvatar(getTableRow().getItem(), 82));
            }
        });
        tableEmployees.getColumns().add(0, colPhoto);
        tableEmployees.setFixedCellSize(100); // hauteur ligne pour photo

        // ── Colonne QR Code ───────────────────────────────────────────────
        TableColumn<Employe, Void> colQR = new TableColumn<>("QR");
        colQR.setPrefWidth(100);
        colQR.setMinWidth(100);
        colQR.setResizable(false);
        colQR.setSortable(false);
        colQR.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow()==null || getTableRow().getItem()==null) {
                    setGraphic(null); return;
                }
                Employe emp = getTableRow().getItem();
                setGraphic(creerQRNode(emp));
            }
        });
        // Ajouter apres colPhoto (position 1)
        tableEmployees.getColumns().add(1, colQR);

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPoste.setCellValueFactory(new PropertyValueFactory<>("poste"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colActif.setCellValueFactory(new PropertyValueFactory<>("actif"));

        colActif.setCellFactory(col -> new TableCell<Employe, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item ? "✓ Actif" : "✗ Inactif");
                    setStyle(item ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;" :
                            "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });

        colEmail.setCellFactory(col -> new TableCell<Employe, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: #3498db; -fx-font-style: italic;");
            }
        });

        // Colonne Performance avec cercle coloré
        colPerformance.setCellFactory(column -> new TableCell<Employe, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Employe employe = getTableRow().getItem();
                    PerformanceService.PerformanceData perf = performanceCache.get(employe.getId());
                    if (perf == null) {
                        perf = performanceService.calculatePerformance(employe.getId());
                        performanceCache.put(employe.getId(), perf);
                    }

                    StackPane stack = new StackPane();
                    Circle circle = new Circle(16);
                    circle.setFill(Color.web(perf.getCouleur()));
                    circle.setStroke(Color.WHITE);
                    circle.setStrokeWidth(2);

                    Label scoreLabel = new Label(String.format("%.0f", perf.score));
                    scoreLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 10px;");

                    stack.getChildren().addAll(circle, scoreLabel);

                    Tooltip tooltip = new Tooltip(String.format("Score: %.1f/100\n%s\nCliquez pour détails", perf.score, perf.getAppreciation()));
                    Tooltip.install(stack, tooltip);

                    stack.setOnMouseClicked(e -> afficherPerformance(employe));

                    setGraphic(stack);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void setupComboBoxes() {
        cmbTri.setItems(FXCollections.observableArrayList(CRITERES_TRI));
        cmbTri.setValue("Nom");
        cmbTri.setOnAction(e -> appliquerTri());
    }

    private void setupFilteredAndSortedLists() {
        filteredList = new FilteredList<>(masterList, p -> true);
        sortedList = new SortedList<>(filteredList);
        tableEmployees.setItems(sortedList);

        if (txtRecherche != null) {
            txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredList.setPredicate(employe -> {
                    if (newVal == null || newVal.trim().isEmpty()) return true;
                    String search = newVal.toLowerCase();
                    return employe.getNom().toLowerCase().contains(search) ||
                            employe.getPrenom().toLowerCase().contains(search) ||
                            employe.getEmail().toLowerCase().contains(search) ||
                            (employe.getPoste() != null && employe.getPoste().toLowerCase().contains(search)) ||
                            (employe.getTelephone() != null && employe.getTelephone().contains(search));
                });
                updateStatistics();
            });
        }
    }

    private void setupTableSelectionListener() {
        tableEmployees.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && lblSelection != null) {
                lblSelection.setText("Sélectionné : " + newVal.getNom() + " " + newVal.getPrenom());
            } else if (lblSelection != null) lblSelection.setText("");
        });
    }

    private void setupKeyboardShortcuts() {
        if (tableEmployees != null && tableEmployees.getScene() != null) {
            tableEmployees.getScene().getAccelerators().put(
                    new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::handleAjouter);
            tableEmployees.getScene().getAccelerators().put(
                    new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN), this::handleModifier);
            tableEmployees.getScene().getAccelerators().put(
                    new KeyCodeCombination(KeyCode.DELETE), this::handleSupprimer);
        }
    }

    private void setupAnimations() {
        if (tableEmployees != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), tableEmployees);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    // ── Chargement des données ───────────────────────────────────────────────
    private void loadEmployesAsync() {
        if (progressIndicator != null) progressIndicator.setVisible(true);

        Task<List<Employe>> task = new Task<>() {
            @Override
            protected List<Employe> call() {
                updateMessage("Chargement des employés...");
                return employeService.getAllEmployes();
            }
        };

        task.setOnSucceeded(e -> {
            masterList.setAll(task.getValue());
            autoActiverEmployesAvecTaches();
            updateStatistics();
            refreshPerformanceCache();
            if (progressIndicator != null) progressIndicator.setVisible(false);
        });

        task.setOnFailed(e -> {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            NotificationToast.showNotification("❌ Erreur lors du chargement", NotificationToast.ERROR, 3000);
        });

        new Thread(task).start();
    }

    /**
     * ✅ Active automatiquement un employé s'il a au moins une tâche assignée
     * (sans modifier manuellement son statut)
     */
    private void autoActiverEmployesAvecTaches() {
        for (Employe emp : masterList) {
            List<Tache> taches = tacheService.getTachesByEmployeId(emp.getId());
            boolean aTacheActive = taches.stream()
                    .anyMatch(t -> "En cours".equalsIgnoreCase(t.getStatut()));

            if (aTacheActive && !emp.isActif()) {
                emp.setActif(true);
                employeService.updateEmploye(emp);
                System.out.println("✅ Employé #" + emp.getId() + " (" + emp.getNom() + ") activé automatiquement");
            }
        }
    }

    private void refreshPerformanceCache() {
        performanceCache.clear();
        for (Employe emp : masterList) {
            PerformanceService.PerformanceData perf = performanceService.calculatePerformance(emp.getId());
            // ✅ Assigner le nom de l'employé pour les top performers
            perf.nomEmploye = emp.getPrenom() + " " + emp.getNom();
            performanceCache.put(emp.getId(), perf);
        }
        updateTopPerformers();
    }

    private void updateTopPerformers() {
        // Filtrer uniquement les employés avec un nom valide
        List<PerformanceService.PerformanceData> sorted = performanceCache.values().stream()
                .filter(p -> p.nomEmploye != null && !p.nomEmploye.trim().isEmpty())
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(3)
                .collect(Collectors.toList());

        if (lblTop1 != null) lblTop1.setText(sorted.size() > 0
                ? sorted.get(0).nomEmploye + " (" + String.format("%.0f", sorted.get(0).score) + ")" : "-");
        if (lblTop2 != null) lblTop2.setText(sorted.size() > 1
                ? sorted.get(1).nomEmploye + " (" + String.format("%.0f", sorted.get(1).score) + ")" : "-");
        if (lblTop3 != null) lblTop3.setText(sorted.size() > 2
                ? sorted.get(2).nomEmploye + " (" + String.format("%.0f", sorted.get(2).score) + ")" : "-");
    }

    /**
     * \uD83C\uDFC6 Fenêtre Top 3 style Leaderboard (comme le leaderboard Agriculteurs)
     */
    @FXML
    private void handleShowTopPerformeurs() {
        Stage stage = new Stage();
        stage.setTitle("\uD83C\uDFC6 Top Performeurs");
        stage.initModality(Modality.APPLICATION_MODAL);

        List<PerformanceService.PerformanceData> tops = performanceCache.values().stream()
                .filter(p -> p.nomEmploye != null && !p.nomEmploye.trim().isEmpty())
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(3)
                .collect(Collectors.toList());

        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1a2e05, #2d5016);" +
                        "-fx-min-width: 520px; -fx-min-height: 420px;"
        );

        // Titre
        Label titre = new Label("\uD83C\uDFC6  Top Performeurs - Employ\u00E9s");
        titre.setStyle(
                "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;" +
                        "-fx-padding: 30 0 20 0;"
        );
        titre.setMaxWidth(Double.MAX_VALUE);
        titre.setAlignment(Pos.CENTER);

        VBox listBox = new VBox(12);
        listBox.setPadding(new Insets(10, 30, 20, 30));

        String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49"};
        String[] bgColors = {
                "rgba(255,215,0,0.15)", "rgba(192,192,192,0.15)", "rgba(205,127,50,0.15)"
        };
        String[] borderColors = {"#FFD700", "#C0C0C0", "#CD7F32"};

        for (int i = 0; i < tops.size(); i++) {
            PerformanceService.PerformanceData p = tops.get(i);

            HBox card = new HBox(15);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(16, 20, 16, 20));
            card.setStyle(
                    "-fx-background-color: " + bgColors[i] + ";" +
                            "-fx-background-radius: 12px;" +
                            "-fx-border-color: " + borderColors[i] + ";" +
                            "-fx-border-radius: 12px;" +
                            "-fx-border-width: 1.5px;" +
                            "-fx-cursor: hand;"
            );

            Label medal = new Label(medals[i]);
            medal.setStyle("-fx-font-size: 30px;");

            VBox info = new VBox(3);
            Label nom = new Label(p.nomEmploye);
            nom.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

            Label appreciation = new Label(p.getEmoji() + "  " + p.getAppreciation() +
                    "  |  " + p.totalTaches + " tâche(s)");
            appreciation.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaccaa;");
            info.getChildren().addAll(nom, appreciation);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label score = new Label(String.format("%.0f pts", p.score));
            score.setStyle(
                    "-fx-font-size: 20px; -fx-font-weight: bold;" +
                            "-fx-text-fill: " + borderColors[i] + ";"
            );

            card.getChildren().addAll(medal, info, spacer, score);

            // Clic sur carte = voir détails
            final PerformanceService.PerformanceData perf = p;
            final Employe emp = masterList.stream()
                    .filter(e -> e.getId() == perf.idEmploye)
                    .findFirst().orElse(null);
            if (emp != null) {
                card.setOnMouseClicked(ev -> afficherPerformance(emp));
                card.setOnMouseEntered(ev -> card.setStyle(card.getStyle().replace("cursor: hand", "cursor: hand") +
                        "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.2), 10, 0, 0, 0);"));
            }

            listBox.getChildren().add(card);
        }

        if (tops.isEmpty()) {
            Label empty = new Label("Aucune donnée de performance disponible.");
            empty.setStyle("-fx-text-fill: #aaa; -fx-font-size: 14px;");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            listBox.getChildren().add(empty);
        }

        // Bouton fermer
        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-padding: 10 30; -fx-background-radius: 20px; -fx-cursor: hand; -fx-font-size: 14px;"
        );
        btnFermer.setOnAction(ev -> stage.close());
        HBox btnBox = new HBox(btnFermer);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(10, 0, 25, 0));

        root.getChildren().addAll(titre, listBox, btnBox);

        Scene scene = new Scene(root, 520, 430);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void updateStatistics() {
        int total = filteredList.size();
        long actifs = filteredList.stream().filter(Employe::isActif).count();
        if (lblTotalEmployes != null) lblTotalEmployes.setText(String.valueOf(total));
        if (lblActifs != null) lblActifs.setText(String.valueOf(actifs));
    }

    // ── Actions CRUD ─────────────────────────────────────────────────────────
    @FXML
    private void handleAjouter() {
        Dialog<Employe> dialog = createEmployeDialog("➕ Ajouter un Employé", null);
        Optional<Employe> result = dialog.showAndWait();

        result.ifPresent(employe -> {
            if (progressIndicator != null) progressIndicator.setVisible(true);

            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    updateMessage("Création de l'employé...");
                    return employeService.createEmploye(employe);
                }
            };

            task.setOnSucceeded(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                if (task.getValue()) {
                    NotificationToast.showNotification("✅ Employé ajouté avec succès !", NotificationToast.SUCCESS, 2500);
                    loadEmployesAsync();
                } else {
                    NotificationToast.showNotification("❌ Erreur lors de l'ajout", NotificationToast.ERROR, 3000);
                }
            });

            task.setOnFailed(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                NotificationToast.showNotification("❌ Erreur: " + task.getException().getMessage(), NotificationToast.ERROR, 3000);
            });

            new Thread(task).start();
        });
    }

    @FXML
    private void handleModifier() {
        Employe selected = tableEmployees.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationToast.showNotification("⚠️ Veuillez sélectionner un employé", NotificationToast.WARNING, 2000);
            return;
        }

        Dialog<Employe> dialog = createEmployeDialog("✏️ Modifier l'Employé", selected);
        Optional<Employe> result = dialog.showAndWait();

        result.ifPresent(employe -> {
            if (progressIndicator != null) progressIndicator.setVisible(true);

            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    updateMessage("Modification de l'employé...");
                    return employeService.updateEmploye(employe);
                }
            };

            task.setOnSucceeded(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                if (task.getValue()) {
                    NotificationToast.showNotification("✅ Employé modifié avec succès !", NotificationToast.SUCCESS, 2500);
                    loadEmployesAsync();
                } else {
                    NotificationToast.showNotification("❌ Erreur lors de la modification", NotificationToast.ERROR, 3000);
                }
            });

            task.setOnFailed(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                NotificationToast.showNotification("❌ Erreur: " + task.getException().getMessage(), NotificationToast.ERROR, 3000);
            });

            new Thread(task).start();
        });
    }

    @FXML
    private void handleSupprimer() {
        Employe selected = tableEmployees.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationToast.showNotification("⚠️ Veuillez sélectionner un employé", NotificationToast.WARNING, 2000);
            return;
        }

        List<Tache> taches = tacheService.getTachesByEmployeId(selected.getId());

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("\uD83D\uDDD1️ Confirmation de suppression");
        confirmation.setHeaderText("Supprimer " + selected.getNom() + " " + selected.getPrenom() + " ?");

        if (!taches.isEmpty()) {
            confirmation.setContentText(
                    "⚠️ Attention : Cet employé a " + taches.size() + " tâche(s) assignée(s).\n" +
                            "Les tâches ne seront pas supprimées, mais n'auront plus d'employé assigné.\n\n" +
                            "Voulez-vous vraiment continuer ?"
            );
        } else {
            confirmation.setContentText("Cette action est irréversible.");
        }

        ButtonType btnSupprimer = new ButtonType("\uD83D\uDDD1️ Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(btnSupprimer, btnAnnuler);

        DialogPane dialogPane = confirmation.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 13px;");
        Button deleteButton = (Button) dialogPane.lookupButton(btnSupprimer);
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent() && result.get() == btnSupprimer) {
            if (progressIndicator != null) progressIndicator.setVisible(true);

            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    updateMessage("Suppression de l'employé...");
                    return employeService.deleteEmploye(selected.getId());
                }
            };

            task.setOnSucceeded(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                if (task.getValue()) {
                    deletedEmployesStack.push(selected);
                    NotificationToast.showNotification("✅ Employé supprimé avec succès !", NotificationToast.SUCCESS, 2500);
                    loadEmployesAsync();
                } else {
                    NotificationToast.showNotification("❌ Erreur lors de la suppression", NotificationToast.ERROR, 3000);
                }
            });

            task.setOnFailed(e -> {
                if (progressIndicator != null) progressIndicator.setVisible(false);
                NotificationToast.showNotification("❌ Erreur: " + task.getException().getMessage(), NotificationToast.ERROR, 3000);
            });

            new Thread(task).start();
        }
    }

    @FXML
    private void handleActualiser() {
        loadEmployesAsync();
        NotificationToast.showNotification("\uD83D\uDD04 Données actualisées", NotificationToast.INFO, 1500);
    }

    @FXML
    private void handleRechercher() {
        // La recherche se fait déjà automatiquement via le listener sur txtRecherche
        // Cette méthode est là pour la compatibilité FXML
    }

    // ── Tri ──────────────────────────────────────────────────────────────────
    private void appliquerTri() {
        String critere = cmbTri.getValue();
        if (critere == null) return;

        Comparator<Employe> comparator = getComparator(critere);
        if (!triCroissant) comparator = comparator.reversed();

        sortedList.setComparator(comparator);
    }

    @FXML
    private void handleTri() {
        appliquerTri();
    }

    @FXML
    private void handleTriCroissant() {
        triCroissant = true;
        appliquerTri();
    }

    @FXML
    private void handleTriDecroissant() {
        triCroissant = false;
        appliquerTri();
    }

    private Comparator<Employe> getComparator(String critere) {
        switch (critere) {
            case "ID":
                return Comparator.comparing(Employe::getId);
            case "Nom":
                return Comparator.comparing(Employe::getNom, String.CASE_INSENSITIVE_ORDER);
            case "Prénom":
                return Comparator.comparing(Employe::getPrenom, String.CASE_INSENSITIVE_ORDER);
            case "Email":
                return Comparator.comparing(Employe::getEmail, String.CASE_INSENSITIVE_ORDER);
            case "Poste":
                return Comparator.comparing(e -> e.getPoste() != null ? e.getPoste() : "", String.CASE_INSENSITIVE_ORDER);
            case "Actif":
                return Comparator.comparing(Employe::isActif).reversed();
            default:
                return Comparator.comparing(Employe::getNom);
        }
    }

    // ── Dialogue de création/modification ────────────────────────────────────
    private Dialog<Employe> createEmployeDialog(String titre, Employe employe) {
        Dialog<Employe> dialog = new Dialog<>();
        dialog.setTitle(titre);
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(620);
        dialog.getDialogPane().setStyle("-fx-background-color: linear-gradient(to bottom right, #E8F5E9, #C8E6C9);");

        // Boutons
        ButtonType btnOK = new ButtonType(
                employe == null ? "➕ Ajouter" : "✏️ Modifier",
                ButtonBar.ButtonData.OK_DONE
        );
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOK, btnCancel);

        // ── Conteneur principal avec sections ──────────────────────────────
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: transparent;");

        // SECTION 1: INFORMATIONS PRINCIPALES
        VBox section1 = new VBox(10);
        Label header1 = new Label("\uD83D\uDCCB INFORMATIONS PRINCIPALES");
        header1.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2E7D32;");

        GridPane grid1 = new GridPane();
        grid1.setHgap(15);
        grid1.setVgap(10);
        grid1.setPadding(new Insets(10, 0, 0, 0));

        // Champs
        TextField txtNom = new TextField(employe != null ? employe.getNom() : "");
        txtNom.setPromptText("Nom");
        txtNom.setStyle("-fx-background-radius: 8; -fx-padding: 10;");
        txtNom.setPrefWidth(400);

        TextField txtPrenom = new TextField(employe != null ? employe.getPrenom() : "");
        txtPrenom.setPromptText("Prénom");
        txtPrenom.setStyle("-fx-background-radius: 8; -fx-padding: 10;");
        txtPrenom.setPrefWidth(400);

        TextField txtEmail = new TextField(employe != null ? employe.getEmail() : "");
        txtEmail.setPromptText("email@exemple.com");
        txtEmail.setStyle("-fx-background-radius: 8; -fx-padding: 10;");
        txtEmail.setPrefWidth(400);

        TextField txtPoste = new TextField(employe != null ? employe.getPoste() : "");
        txtPoste.setPromptText("Poste");
        txtPoste.setStyle("-fx-background-radius: 8; -fx-padding: 10;");
        txtPoste.setPrefWidth(400);

        // Labels d'erreur
        Label errNom = createErrorLabel();
        Label errPrenom = createErrorLabel();
        Label errEmail = createErrorLabel();
        Label errPoste = createErrorLabel();

        // Ajout au grid
        int row = 0;
        grid1.add(createFieldLabel("Nom *:"), 0, row);
        grid1.add(txtNom, 0, row + 1);
        grid1.add(errNom, 0, row + 2);
        row += 3;

        grid1.add(createFieldLabel("Prénom *:"), 0, row);
        grid1.add(txtPrenom, 0, row + 1);
        grid1.add(errPrenom, 0, row + 2);
        row += 3;

        grid1.add(createFieldLabel("Email *:"), 0, row);
        grid1.add(txtEmail, 0, row + 1);
        grid1.add(errEmail, 0, row + 2);
        row += 3;

        grid1.add(createFieldLabel("Poste *:"), 0, row);
        grid1.add(txtPoste, 0, row + 1);
        grid1.add(errPoste, 0, row + 2);

        section1.getChildren().addAll(header1, grid1);

        // SECTION 2: CONTACT
        VBox section2 = new VBox(10);
        Label header2 = new Label("\uD83D\uDCDE CONTACT");
        header2.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2E7D32;");

        GridPane grid2 = new GridPane();
        grid2.setHgap(15);
        grid2.setVgap(10);
        grid2.setPadding(new Insets(10, 0, 0, 0));

        TextField txtTelephone = new TextField(employe != null ? employe.getTelephone() : "");
        txtTelephone.setPromptText("Ex: 22345678 (8 chiffres)");
        txtTelephone.setStyle("-fx-background-radius: 8; -fx-padding: 10;");
        txtTelephone.setPrefWidth(400);

        Label errTelephone = createErrorLabel();

        // Bloquer la saisie de caractères non numériques
        txtTelephone.textProperty().addListener((obs, oldVal, newVal) -> {
            // N'autoriser que les chiffres
            if (!newVal.matches("\\d*")) {
                txtTelephone.setText(newVal.replaceAll("[^\\d]", ""));
                return;
            }
            // Limiter à 8 chiffres
            if (newVal.length() > 8) {
                txtTelephone.setText(newVal.substring(0, 8));
                return;
            }
            // Validation en temps réel
            if (newVal.isEmpty()) {
                errTelephone.setText("");
                txtTelephone.setStyle("-fx-background-radius: 8; -fx-padding: 10;");
            } else if (!PHONE_PATTERN.matcher(newVal).matches()) {
                errTelephone.setText("⚠ Numéro invalide (exactement 8 chiffres requis)");
                txtTelephone.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-background-radius: 8; -fx-padding: 8;");
            } else {
                errTelephone.setText("");
                txtTelephone.setStyle("-fx-border-color: #27ae60; -fx-border-width: 2; -fx-background-radius: 8; -fx-padding: 8;");
            }
        });

        grid2.add(createFieldLabel("Téléphone (8 chiffres):"), 0, 0);
        grid2.add(txtTelephone, 0, 1);
        grid2.add(errTelephone, 0, 2);

        section2.getChildren().addAll(header2, grid2);

        // SECTION 3: STATUT
        VBox section3 = new VBox(10);
        Label header3 = new Label("✓ STATUT");
        header3.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2E7D32;");

        CheckBox chkActif = new CheckBox("Employé actif");
        chkActif.setSelected(employe == null || employe.isActif());
        chkActif.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        section3.getChildren().addAll(header3, chkActif);

        // ── SECTION PHOTO ────────────────────────────────────────────────
        final String[] photoHolder = { employe != null ? employe.getPhotoPath() : null };

        VBox sectionPhoto = new VBox(10);
        sectionPhoto.setStyle("-fx-background-color:#f8fdf9; -fx-border-color:#c8e6c9; -fx-border-width:1; -fx-border-radius:10; -fx-background-radius:10; -fx-padding:12;");
        Label headerPhoto = new Label("\uD83D\uDCF8 Photo");
        headerPhoto.setStyle("-fx-font-weight:bold; -fx-font-size:11px; -fx-text-fill:#2E7D32;");

        // Preview compact 70px
        ImageView preview = creerPreviewCirc(photoHolder[0], 70);
        StackPane previewCont = new StackPane(preview);
        previewCont.setPrefSize(76,76);
        previewCont.setMinSize(76,76);
        previewCont.setMaxSize(76,76);
        previewCont.setStyle("-fx-background-color:#e8f5e9; -fx-background-radius:38; -fx-border-color:#66bb6a; -fx-border-width:2; -fx-border-radius:38;");
        previewCont.setAlignment(javafx.geometry.Pos.CENTER);

        Label lblPhotoInfo = new Label(photoHolder[0]!=null ? "\u2713 OK" : "Aucune");
        lblPhotoInfo.setStyle("-fx-font-size:10px; -fx-font-style:italic; -fx-text-fill:"+(photoHolder[0]!=null?"#27ae60":"#999")+"; -fx-wrap-text:true;");
        lblPhotoInfo.setMaxWidth(120);

        // Boutons verticaux pour economiser la largeur
        Button btnFichier = new Button("\uD83D\uDCC2 Fichier");
        btnFichier.setStyle("-fx-background-color:#1565c0; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:6; -fx-padding:6 8; -fx-cursor:hand; -fx-font-size:11px;");
        btnFichier.setMaxWidth(Double.MAX_VALUE);
        btnFichier.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une photo");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images","*.jpg","*.jpeg","*.png","*.gif","*.bmp"));
            File f = fc.showOpenDialog(btnFichier.getScene().getWindow());
            if (f != null) {
                String saved = photoSave(f, employe!=null?employe.getId():0);
                if (saved!=null) { photoHolder[0]=saved; photoRefresh(preview, saved, previewCont); lblPhotoInfo.setText("\u2713 "+f.getName()); lblPhotoInfo.setStyle("-fx-font-size:10px; -fx-text-fill:#27ae60; -fx-font-weight:bold; -fx-wrap-text:true;"); }
            }
        });

        Button btnCam = new Button("\uD83D\uDCF7 Camera");
        btnCam.setStyle("-fx-background-color:#e65100; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:6; -fx-padding:6 8; -fx-cursor:hand; -fx-font-size:11px;");
        btnCam.setMaxWidth(Double.MAX_VALUE);
        btnCam.setOnAction(ev -> {
            String saved = photoCamera(employe!=null?employe.getId():0, btnCam.getScene().getWindow());
            if (saved!=null) { photoHolder[0]=saved; photoRefresh(preview, saved, previewCont); lblPhotoInfo.setText("\u2713 Capturee"); lblPhotoInfo.setStyle("-fx-font-size:10px; -fx-text-fill:#27ae60; -fx-font-weight:bold;"); }
        });

        Button btnSuppr = new Button("\u2716 Suppr.");
        btnSuppr.setStyle("-fx-background-color:#c62828; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:6; -fx-padding:6 8; -fx-cursor:hand; -fx-font-size:11px;");
        btnSuppr.setMaxWidth(Double.MAX_VALUE);
        btnSuppr.setOnAction(ev -> { photoHolder[0]=null; preview.setImage(null); previewCont.setStyle("-fx-background-color:#f5f5f5; -fx-background-radius:38; -fx-border-color:#bbb; -fx-border-width:2; -fx-border-radius:38;"); lblPhotoInfo.setText("Aucune"); lblPhotoInfo.setStyle("-fx-font-size:10px; -fx-font-style:italic; -fx-text-fill:#999;"); });

        VBox photoButtons = new VBox(6, btnFichier, btnCam, btnSuppr);
        photoButtons.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        // Photo centree + boutons dessous
        VBox photoContent = new VBox(8, previewCont, lblPhotoInfo, photoButtons);
        photoContent.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        sectionPhoto.getChildren().addAll(headerPhoto, photoContent);

        final String[] finalPhoto = photoHolder;

        // ── Mise en page compacte : photo a droite des champs ─────────────
        // Colonne gauche : champs du formulaire
        VBox leftCol = new VBox(15, section1, section2, section3);
        leftCol.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(leftCol, javafx.scene.layout.Priority.ALWAYS);

        // Colonne droite : photo compacte
        sectionPhoto.setPrefWidth(160);
        sectionPhoto.setMinWidth(160);
        sectionPhoto.setMaxWidth(160);

        // Mise en page horizontale : champs | photo
        HBox twoColumns = new HBox(16, leftCol, sectionPhoto);
        twoColumns.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        mainContainer.getChildren().add(twoColumns);

        // ScrollPane pour garantir visibilite des boutons Valider/Annuler
        ScrollPane scroll = new ScrollPane(mainContainer);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxHeight(javafx.stage.Screen.getPrimary().getBounds().getHeight() * 0.65);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        dialog.getDialogPane().setContent(scroll);

        // Bouton OK styling
        Button okButton = (Button) dialog.getDialogPane().lookupButton(btnOK);
        okButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        okButton.setEffect(new DropShadow(10, Color.rgb(39, 174, 96, 0.3)));

        // Validation en temps réel
        txtNom.textProperty().addListener((obs, oldVal, newVal) ->
                validateField(txtNom, errNom, "Nom requis (min 2 caractères)", newVal.trim().length() >= 2, okButton, txtNom, txtPrenom, txtEmail, txtPoste));
        txtPrenom.textProperty().addListener((obs, oldVal, newVal) ->
                validateField(txtPrenom, errPrenom, "Prénom requis (min 2 caractères)", newVal.trim().length() >= 2, okButton, txtNom, txtPrenom, txtEmail, txtPoste));
        txtEmail.textProperty().addListener((obs, oldVal, newVal) ->
                validateEmail(txtEmail, errEmail, newVal, employe != null ? employe.getId() : 0, okButton, txtNom, txtPrenom, txtEmail, txtPoste));
        txtPoste.textProperty().addListener((obs, oldVal, newVal) ->
                validateField(txtPoste, errPoste, "Poste requis", !newVal.trim().isEmpty(), okButton, txtNom, txtPrenom, txtEmail, txtPoste));

        // Validation initiale
        okButton.setDisable(true);
        if (employe != null) {
            updateOkButtonState(okButton, txtNom, txtPrenom, txtEmail, txtPoste);
        }

        // Conversion du résultat
        dialog.setResultConverter(btn -> {
            if (btn != btnOK) return null;

            if (!validateFinal(txtNom, txtPrenom, txtEmail, txtPoste, errNom, errPrenom, errEmail, errPoste)) {
                return null;
            }

            Employe resultat = employe != null ? employe : new Employe();
            resultat.setNom(txtNom.getText().trim());
            resultat.setPrenom(txtPrenom.getText().trim());
            resultat.setEmail(txtEmail.getText().trim());
            resultat.setPoste(txtPoste.getText().trim());
            resultat.setTelephone(txtTelephone.getText().trim());
            resultat.setActif(chkActif.isSelected());
            resultat.setPhotoPath(finalPhoto[0]);

            return resultat;
        });

        return dialog;
    }

    private void validateField(TextField field, Label errLabel, String errorMsg, boolean isValid, Button okButton,
                               TextField nom, TextField prenom, TextField email, TextField poste) {
        if (!isValid) {
            errLabel.setText("⚠ " + errorMsg);
            field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-background-radius: 8; -fx-padding: 8;");
        } else {
            errLabel.setText("");
            field.setStyle("-fx-border-color: #27ae60; -fx-border-width: 2; -fx-background-radius: 8; -fx-padding: 8;");
        }
        updateOkButtonState(okButton, nom, prenom, email, poste);
    }

    private void validateEmail(TextField field, Label errLabel, String email, int excludeId, Button okButton,
                               TextField nom, TextField prenom, TextField emailField, TextField poste) {
        if (email.trim().isEmpty()) {
            errLabel.setText("⚠ Email requis");
            field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-background-radius: 8; -fx-padding: 8;");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errLabel.setText("⚠ Format email invalide");
            field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-background-radius: 8; -fx-padding: 8;");
        } else if (employeService.emailExists(email, excludeId)) {
            errLabel.setText("⚠ Cet email existe déjà");
            field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-background-radius: 8; -fx-padding: 8;");
        } else {
            errLabel.setText("");
            field.setStyle("-fx-border-color: #27ae60; -fx-border-width: 2; -fx-background-radius: 8; -fx-padding: 8;");
        }
        updateOkButtonState(okButton, nom, prenom, emailField, poste);
    }

    private void updateOkButtonState(Button okButton, TextField nom, TextField prenom, TextField email, TextField poste) {
        boolean nomValid = nom.getText().trim().length() >= 2;
        boolean prenomValid = prenom.getText().trim().length() >= 2;
        boolean emailValid = !email.getText().trim().isEmpty() && EMAIL_PATTERN.matcher(email.getText()).matches();
        boolean posteValid = !poste.getText().trim().isEmpty();

        okButton.setDisable(!(nomValid && prenomValid && emailValid && posteValid));
    }

    private boolean validateFinal(TextField nom, TextField prenom, TextField email, TextField poste,
                                  Label errNom, Label errPrenom, Label errEmail, Label errPoste) {
        boolean valid = true;

        if (nom.getText().trim().length() < 2) {
            errNom.setText("⚠ Nom requis (min 2 caractères)");
            valid = false;
        }

        if (prenom.getText().trim().length() < 2) {
            errPrenom.setText("⚠ Prénom requis (min 2 caractères)");
            valid = false;
        }

        if (email.getText().trim().isEmpty()) {
            errEmail.setText("⚠ Email requis");
            valid = false;
        } else if (!EMAIL_PATTERN.matcher(email.getText()).matches()) {
            errEmail.setText("⚠ Format email invalide");
            valid = false;
        }

        if (poste.getText().trim().isEmpty()) {
            errPoste.setText("⚠ Poste requis");
            valid = false;
        }

        return valid;
    }

    // ── Export PDF ───────────────────────────────────────────────────────────
    @FXML
    private void handleExportPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter la liste des employés en PDF");
        fc.setInitialFileName("employes_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        File file = fc.showSaveDialog(tableEmployees.getScene().getWindow());
        if (file == null) return;

        if (progressIndicator != null) progressIndicator.setVisible(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    Document doc = new Document(PageSize.A4.rotate());
                    PdfWriter.getInstance(doc, new FileOutputStream(file));
                    doc.open();

                    Font fTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
                    Paragraph title = new Paragraph("LISTE DES EMPLOYÉS", fTitle);
                    title.setAlignment(Element.ALIGN_CENTER);
                    title.setSpacingAfter(20);
                    doc.add(title);

                    Font fSub = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.GRAY);
                    Paragraph info = new Paragraph(
                            "Généré le : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                                    " | Total : " + sortedList.size() + " employé(s)",
                            fSub
                    );
                    info.setAlignment(Element.ALIGN_CENTER);
                    info.setSpacingAfter(20);
                    doc.add(info);

                    PdfPTable table = new PdfPTable(7);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[]{1f, 2.5f, 2.5f, 3.5f, 2f, 2f, 1.5f});

                    Font fHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);
                    String[] headers = {"ID", "Nom", "Prénom", "Email", "Poste", "Téléphone", "Actif"};
                    for (String header : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(header, fHeader));
                        cell.setBackgroundColor(new BaseColor(52, 73, 94));
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        cell.setPadding(8);
                        table.addCell(cell);
                    }

                    Font fData = FontFactory.getFont(FontFactory.HELVETICA, 10);
                    for (Employe emp : sortedList) {
                        table.addCell(new Phrase(String.valueOf(emp.getId()), fData));
                        table.addCell(new Phrase(emp.getNom(), fData));
                        table.addCell(new Phrase(emp.getPrenom(), fData));
                        table.addCell(new Phrase(emp.getEmail(), fData));
                        table.addCell(new Phrase(emp.getPoste() != null ? emp.getPoste() : "", fData));
                        table.addCell(new Phrase(emp.getTelephone() != null ? emp.getTelephone() : "", fData));

                        PdfPCell actifCell = new PdfPCell(new Phrase(emp.isActif() ? "Oui" : "Non", fData));
                        actifCell.setBackgroundColor(emp.isActif() ? BaseColor.GREEN : BaseColor.RED);
                        table.addCell(actifCell);
                    }

                    doc.add(table);
                    doc.close();
                    return true;

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            if (task.getValue()) {
                NotificationToast.showNotification("✅ PDF exporté avec succès !", NotificationToast.SUCCESS, 3000);
            } else {
                NotificationToast.showNotification("❌ Erreur d'export PDF", NotificationToast.ERROR, 3000);
            }
        });

        new Thread(task).start();
    }

    // ── Statistiques ─────────────────────────────────────────────────────────
    @FXML
    private void handleShowStatistics() {
        Stage statsStage = new Stage();
        statsStage.setTitle("\uD83D\uDCCA Statistiques des Employés");
        statsStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #ecf0f1;");

        long actifs = masterList.stream().filter(Employe::isActif).count();
        long inactifs = masterList.size() - actifs;

        PieChart pieChart = new PieChart();
        pieChart.setTitle("Employés Actifs vs Inactifs");
        pieChart.getData().addAll(
                new PieChart.Data("Actifs (" + actifs + ")", actifs),
                new PieChart.Data("Inactifs (" + inactifs + ")", inactifs)
        );

        Map<String, Long> parPoste = new HashMap<>();
        for (Employe emp : masterList) {
            String poste = emp.getPoste() != null ? emp.getPoste() : "Non défini";
            parPoste.put(poste, parPoste.getOrDefault(poste, 0L) + 1);
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Répartition par Poste");
        xAxis.setLabel("Poste");
        yAxis.setLabel("Nombre d'employés");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Employés");
        parPoste.forEach((poste, count) -> series.getData().add(new XYChart.Data<>(poste, count)));
        barChart.getData().add(series);

        root.getChildren().addAll(pieChart, barChart);

        Scene scene = new Scene(root, 800, 600);
        statsStage.setScene(scene);
        statsStage.show();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555;");
        return label;
    }

    private Label createErrorLabel() {
        Label label = new Label();
        label.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
        label.setWrapText(true);
        label.setMaxWidth(400);
        return label;
    }

    // ── Retour intelligent avec condition par role ──────────────────────────
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Navigation vers Gestion Tâches ──────────────────────────────────────
    @FXML
    private void goToTaches() {
        try {
            ResourceBundle bundle = LanguageManager.getInstance().getBundle();
            URL tacheFxml = getClass().getResource("/fxml/gestionemploye/tache_list.fxml");
            if (tacheFxml == null) {
                showAlert("Erreur", "Fichier tache_list.fxml introuvable.\nVérifiez que le fichier existe dans src/main/resources/fxml/gestionemploye/");
                return;
            }
            FXMLLoader loader = new FXMLLoader(tacheFxml, bundle);
            Parent root = loader.load();
            Stage currentStage = (Stage) (btnRetour != null ? btnRetour.getScene().getWindow()
                    : tableEmployees.getScene().getWindow());
            Stage tacheStage = new Stage();
            tacheStage.setTitle("Ardhi - Gestion des Tâches");
            tacheStage.setScene(new Scene(root));
            tacheStage.setMaximized(currentStage.isMaximized());
            tacheStage.show();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur chargement Tâches", e.getMessage());
        }
    }

    // ── Contrôle d'accès ────────────────────────────────────────────────────
    private void showAccessDeniedAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-font-size: 14px; -fx-padding: 20px;");
        alert.showAndWait();
        if (tableEmployees != null && tableEmployees.getScene() != null) {
            Stage stage = (Stage) tableEmployees.getScene().getWindow();
            stage.close();
        }
    }

    // ── Affichage Performance ───────────────────────────────────────────────
    private void afficherPerformance(Employe employe) {
        PerformanceService.PerformanceData perf = performanceService.calculatePerformance(employe.getId());

        Stage stage = new Stage();
        stage.setTitle("\uD83D\uDCCA Performance - " + employe.getPrenom() + " " + employe.getNom());
        stage.initModality(Modality.APPLICATION_MODAL);

        // ── Root container ──
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f0f4f8;");

        // ── HEADER avec score ──
        VBox header = new VBox(8);
        header.setPadding(new Insets(25, 30, 20, 30));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #1a5276, #2980b9);"
        );

        Label nomLabel = new Label(employe.getPrenom() + " " + employe.getNom());
        nomLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox scoreBox = new HBox(15);
        scoreBox.setAlignment(Pos.CENTER_LEFT);

        // Cercle score
        StackPane scoreCircle = new StackPane();
        javafx.scene.shape.Circle cercle = new javafx.scene.shape.Circle(38);
        cercle.setFill(Color.web(perf.getCouleur()));
        cercle.setStroke(Color.WHITE);
        cercle.setStrokeWidth(3);
        Label scoreNum = new Label(String.format("%.0f", perf.score));
        scoreNum.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        scoreCircle.getChildren().addAll(cercle, scoreNum);

        VBox scoreInfo = new VBox(4);
        Label appLabel = new Label(perf.getEmoji() + "  " + perf.getAppreciation());
        appLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label tauxLabel = new Label("Taux de réussite : " + String.format("%.1f%%", perf.tauxReussite));
        tauxLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #d6eaf8;");
        scoreInfo.getChildren().addAll(appLabel, tauxLabel);

        scoreBox.getChildren().addAll(scoreCircle, scoreInfo);
        header.getChildren().addAll(nomLabel, scoreBox);

        // ── BODY ──
        VBox body = new VBox(15);
        body.setPadding(new Insets(20, 30, 10, 30));

        // Section tâches
        body.getChildren().add(makeSectionTitle("\uD83D\uDCCB Statistiques des Tâches"));

        GridPane gridTaches = new GridPane();
        gridTaches.setHgap(20);
        gridTaches.setVgap(10);
        gridTaches.setPadding(new Insets(10, 0, 0, 0));

        addStatRow(gridTaches, 0, "\uD83D\uDCCA Total", perf.totalTaches + " tâche(s)", "#2c3e50");
        addStatRow(gridTaches, 1, "✅ Terminées", perf.tachesTerminees + " tâche(s)", "#27ae60");
        addStatRow(gridTaches, 2, "\uD83D\uDD04 En cours", perf.tachesEnCours + " tâche(s)", "#2980b9");
        addStatRow(gridTaches, 3, "⏰ En retard", perf.tachesEnRetard + " tâche(s)",
                perf.tachesEnRetard > 0 ? "#e74c3c" : "#27ae60");
        addStatRow(gridTaches, 4, "❌ Annulées", perf.tachesAnnulees + " tâche(s)", "#95a5a6");
        body.getChildren().add(gridTaches);

        // Barre de progression
        body.getChildren().add(makeSectionTitle("\uD83D\uDCC8 Score de Performance"));
        ProgressBar progBar = new ProgressBar(perf.score / 100.0);
        progBar.setPrefWidth(Double.MAX_VALUE);
        progBar.setPrefHeight(20);
        progBar.setStyle("-fx-accent: " + perf.getCouleur() + "; -fx-control-inner-background: #dde;");
        body.getChildren().add(progBar);

        // Temps moyen
        body.getChildren().add(makeSectionTitle("⏱️ Temps de Réalisation"));
        Label tempsLabel = new Label(String.format("Temps moyen : %.1f jour(s)", perf.tempsRealisationMoyen));
        tempsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        body.getChildren().add(tempsLabel);

        // Recommandations
        body.getChildren().add(makeSectionTitle("\uD83D\uDCA1 Recommandations"));
        Label recoLabel = new Label(getRecommandations(perf));
        recoLabel.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #34495e;" +
                        "-fx-background-color: white; -fx-background-radius: 8;" +
                        "-fx-padding: 12; -fx-border-color: #bdc3c7; -fx-border-radius: 8;"
        );
        recoLabel.setWrapText(true);
        recoLabel.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().add(recoLabel);

        // ── FOOTER ──
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 30, 20, 30));
        Button btnOk = new Button("Fermer");
        btnOk.setStyle(
                "-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-padding: 9 30; -fx-background-radius: 20px; -fx-cursor: hand; -fx-font-size: 14px;"
        );
        btnOk.setOnAction(e -> stage.close());
        footer.getChildren().add(btnOk);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, scroll, footer);

        Scene scene = new Scene(root, 560, 580);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    private Label makeSectionTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a5276;" +
                        "-fx-border-color: #aed6f1; -fx-border-width: 0 0 1 0; -fx-padding: 8 0 4 0;"
        );
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private void addStatRow(GridPane grid, int row, String label, String value, String valueColor) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #555; -fx-font-weight: bold;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 13px; -fx-text-fill: " + valueColor + "; -fx-font-weight: bold;");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private String getRecommandations(PerformanceService.PerformanceData perf) {
        if (perf.score >= 90) {
            return "\uD83C\uDFC6 Performance excellente !\n   → Continuez sur cette lancée\n   → Envisagez un rôle de mentor";
        } else if (perf.score >= 75) {
            return "⭐ Très bonne performance !\n   → Performance stable et fiable\n   → Peut prendre plus de responsabilités";
        } else if (perf.score >= 60) {
            return "\uD83D\uDC4D Bonne performance !\n   → Performance satisfaisante\n   → Quelques améliorations possibles";
        } else if (perf.score >= 50) {
            return "\uD83D\uDC4C Performance moyenne\n   → Attention aux retards\n   → Suivi recommandé";
        } else if (perf.tachesEnRetard > 3) {
            return "⚠️ Performance faible - Retards fréquents\n   → " + perf.tachesEnRetard + " tâche(s) en retard\n   → Revoir la charge de travail\n   → Formation recommandée";
        } else if (perf.totalTaches == 0) {
            return "ℹ️ Aucune tâche assignée\n   → Commencer par des tâches simples\n   → Établir un historique de performance";
        } else {
            return "⚠️ Performance faible\n   → Accompagnement nécessaire\n   → Revoir les objectifs\n   → Suivi hebdomadaire recommandé";
        }
    }

    // METHODES PHOTO EMPLOYE
    // ═══════════════════════════════════════════════════════

    /** Dossier local Java (lecture rapide en JavaFX) */
    private static final String PHOTOS_DIR =
            System.getProperty("user.home") + "/Ardhi/photos_employes/";

    /**
     * Dossier public du projet Symfony/ArdhiWEB.
     * Les photos déposées ici sont accessibles via http://127.0.0.1:8000/uploads/employes/
     * IMPORTANT : ce chemin DOIT correspondre à l'emplacement réel d'ArdhiWEB sur cette machine.
     */
    private static final String WEB_UPLOADS_DIR =
            System.getProperty("user.home") + "/ArdhiWEB/public/uploads/employes/";

    /** Préfixe URL stocké en base (identique au format Symfony) */
    private static final String WEB_PHOTO_PREFIX = "/uploads/employes/";

    // ── Mini serveur HTTP embarque pour fiches mobile ─────────────────────
    private static HttpServer ficheServeur = null;
    private static int       fichePort    = 8787;
    /** Map id_employe → Employe pour les requetes HTTP */
    private static final ConcurrentHashMap<Integer, Employe> ficheCache = new ConcurrentHashMap<>();

    /** Preview circulaire pour le formulaire */
    private ImageView creerPreviewCirc(String path, double sz) {
        ImageView iv = new ImageView();
        iv.setFitWidth(sz); iv.setFitHeight(sz); iv.setPreserveRatio(false);
        photoLoad(iv, path, sz);
        iv.setClip(new javafx.scene.shape.Circle(sz/2, sz/2, sz/2));
        return iv;
    }

    /** Avatar circulaire pour la colonne tableau */
    private javafx.scene.Node creerAvatar(Employe emp, double sz) {
        StackPane sp = new StackPane();
        sp.setPrefSize(sz, sz);
        if (emp.hasPhoto()) {
            ImageView iv = new ImageView();
            iv.setFitWidth(sz); iv.setFitHeight(sz); iv.setPreserveRatio(false);
            photoLoad(iv, emp.getPhotoPath(), sz);
            iv.setClip(new javafx.scene.shape.Circle(sz/2, sz/2, sz/2));
            sp.setStyle("-fx-background-radius:"+(sz/2)+"; -fx-border-color:#27ae60; -fx-border-width:1.5; -fx-border-radius:"+(sz/2)+";");
            sp.getChildren().add(iv);
        } else {
            String ini = (!emp.getPrenom().isEmpty() ? String.valueOf(emp.getPrenom().charAt(0)) : "?") +
                    (!emp.getNom().isEmpty()    ? String.valueOf(emp.getNom().charAt(0))    : "?");
            String[] cols = {"#1abc9c","#3498db","#9b59b6","#e67e22","#e74c3c","#27ae60","#f39c12"};
            sp.setStyle("-fx-background-color:"+cols[Math.abs(emp.getId()%cols.length)]+"; -fx-background-radius:"+(sz/2)+";");
            Label l = new Label(ini.toUpperCase());
            l.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:"+(sz*0.38)+"px;");
            sp.getChildren().add(l);
        }
        return sp;
    }

    /** Charge une image dans un ImageView — gère chemin relatif web ET chemin absolu */
    private void photoLoad(ImageView iv, String path, double sz) {
        if (path == null || path.isBlank()) { iv.setImage(null); return; }
        try {
            File f;
            if (path.startsWith("/uploads/")) {
                // Chemin relatif web : chercher d'abord dans le dossier web Symfony
                String basename = path.substring(path.lastIndexOf('/') + 1);
                f = new File(WEB_UPLOADS_DIR + basename);
                if (!f.exists()) f = new File(PHOTOS_DIR + basename); // fallback local
            } else {
                f = new File(path);
                if (!f.isAbsolute()) f = new File(PHOTOS_DIR + path);
            }
            if (f.exists()) iv.setImage(new Image(f.toURI().toString(), sz, sz, false, true));
        } catch (Exception e) { System.err.println("[Photo] " + e.getMessage()); }
    }

    /** Rafraichit le preview apres selection */
    private void photoRefresh(ImageView iv, String path, StackPane cont) {
        photoLoad(iv, path, iv.getFitWidth());
        cont.setStyle("-fx-background-color:#e8f5e9; -fx-background-radius:55; -fx-border-color:#27ae60; -fx-border-width:2; -fx-border-radius:55;");
    }

    /**
     * Copie le fichier dans PHOTOS_DIR (local Java) ET dans WEB_UPLOADS_DIR (Symfony).
     * Retourne le chemin RELATIF web (/uploads/employes/EMP_X.ext) pour la BDD.
     * Ce format est lu par les deux applications.
     */
    private String photoSave(File src, int idEmp) {
        try {
            String ext = src.getName().contains(".")
                    ? src.getName().substring(src.getName().lastIndexOf('.'))
                    : ".jpg";
            String filename = "EMP_" + (idEmp > 0 ? idEmp : System.currentTimeMillis()) + ext;

            // 1️⃣  Copie dans le dossier local Java (pour affichage rapide en JavaFX)
            new File(PHOTOS_DIR).mkdirs();
            Path localDest = Paths.get(PHOTOS_DIR + filename);
            Files.copy(src.toPath(), localDest, StandardCopyOption.REPLACE_EXISTING);

            // 2️⃣  Copie dans le dossier public Symfony (pour le web)
            new File(WEB_UPLOADS_DIR).mkdirs();
            Path webDest = Paths.get(WEB_UPLOADS_DIR + filename);
            Files.copy(src.toPath(), webDest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[✅ Photo] Sauvegardie : " + webDest);

            // 3️⃣  Retourne le chemin RELATIF web (compatible BDD partagee)
            return WEB_PHOTO_PREFIX + filename;

        } catch (IOException e) {
            Alert al = new Alert(Alert.AlertType.ERROR);
            al.setTitle("Erreur photo"); al.setContentText(e.getMessage()); al.showAndWait();
            return null;
        }
    }

    /** Ouvre la fenetre camera pour capturer une photo de profil */
    private String photoCamera(int idEmp, javafx.stage.Window parent) {
        javafx.stage.Stage cam = new javafx.stage.Stage();
        cam.setTitle("Prendre une photo");
        cam.initModality(Modality.APPLICATION_MODAL);
        cam.initOwner(parent);

        ImageView live = new ImageView();
        live.setFitWidth(480); live.setFitHeight(360); live.setPreserveRatio(true);
        Label status = new Label("Demarrage camera...");
        status.setStyle("-fx-font-size:12px; -fx-text-fill:#aaa;");
        Button btnCap = new Button("Capturer");
        btnCap.setStyle("-fx-background-color:#27ae60; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 26; -fx-cursor:hand; -fx-font-size:14px;");
        btnCap.setDisable(true);
        Button btnAnn = new Button("Annuler");
        btnAnn.setStyle("-fx-background-color:#c62828; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:10 22; -fx-cursor:hand;");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(14, live, status, new javafx.scene.layout.HBox(16, btnCap, btnAnn) {{ setAlignment(javafx.geometry.Pos.CENTER); }});
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color:#111827;");
        cam.setScene(new Scene(root, 520, 460));

        final String[] res = {null};
        final javafx.scene.image.Image[] lastFrame = {null};

        tn.neuron.ardhi.services.gestionemployeservice.QRScannerService scanner =
                new tn.neuron.ardhi.services.gestionemployeservice.QRScannerService();
        scanner.setOnFrameReady(img -> { live.setImage(img); lastFrame[0]=img; if(btnCap.isDisabled()){btnCap.setDisable(false); status.setText("Pret — cliquez pour capturer"); status.setStyle("-fx-font-size:12px; -fx-text-fill:#3aff7a;"); } });
        scanner.setOnError(msg -> { status.setText("Erreur: "+msg); status.setStyle("-fx-font-size:12px; -fx-text-fill:#e74c3c;"); });

        btnCap.setOnAction(ev -> {
            if (lastFrame[0]==null) return;
            try {
                String camFilename = "EMP_" + (idEmp > 0 ? idEmp : System.currentTimeMillis()) + ".png";
                // 1️⃣ Sauvegarde locale Java
                new File(PHOTOS_DIR).mkdirs();
                File out = new File(PHOTOS_DIR + camFilename);
                // Conversion JavaFX Image → BufferedImage sans SwingFXUtils
                int w=(int)lastFrame[0].getWidth(), h=(int)lastFrame[0].getHeight();
                BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                javafx.scene.image.PixelReader pr = lastFrame[0].getPixelReader();
                for(int y=0;y<h;y++) for(int x=0;x<w;x++) buf.setRGB(x,y,pr.getArgb(x,y));
                ImageIO.write(buf,"png",out);
                // 2️⃣ Copie dans le dossier web Symfony
                try {
                    new File(WEB_UPLOADS_DIR).mkdirs();
                    Files.copy(out.toPath(), Paths.get(WEB_UPLOADS_DIR + camFilename), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException webEx) { System.err.println("[Photo Web] " + webEx.getMessage()); }
                // 3️⃣ Chemin RELATIF web pour la BDD
                res[0] = WEB_PHOTO_PREFIX + camFilename;
                status.setText("Capture sauvegardee !");
                status.setStyle("-fx-font-size:12px; -fx-text-fill:#27ae60; -fx-font-weight:bold;");
                scanner.arreter();
                PauseTransition pause = new PauseTransition(Duration.millis(700));
                pause.setOnFinished(e -> cam.close());
                pause.play();
            } catch(IOException e){ status.setText("Erreur: "+e.getMessage()); }
        });
        btnAnn.setOnAction(ev->{ scanner.arreter(); cam.close(); });
        cam.setOnCloseRequest(ev->scanner.arreter());
        scanner.demarrer();
        cam.showAndWait();
        return res[0];
    }

    // ═══════════════════════════════════════════════════════════════════
    // QR CODE PAR EMPLOYE - GENERATION + SCAN → FICHE PDF
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Genere le QR code d'un employe sous forme de javafx.scene.image.Image.
     * Le QR encode un bloc JSON compact avec tous les details.
     */
    private javafx.scene.image.Image genererQRImage(Employe emp, int taille) {
        try {
            String contenu = buildQRContent(emp);

            java.util.Map<EncodeHintType, Object> hints = new java.util.EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(contenu, BarcodeFormat.QR_CODE, taille, taille, hints);

            // BitMatrix → BufferedImage → JavaFX Image
            BufferedImage bi = new BufferedImage(taille, taille, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < taille; x++) {
                for (int y = 0; y < taille; y++) {
                    bi.setRGB(x, y, matrix.get(x, y) ? 0xFF2C3E50 : 0xFFFFFFFF);
                }
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            return new javafx.scene.image.Image(
                    new java.io.ByteArrayInputStream(baos.toByteArray()));

        } catch (Exception e) {
            System.err.println("[QR] Generation: " + e.getMessage());
            return null;
        }
    }

    /**
     * Contenu JSON encode dans le QR code
     */
    /**
     * Retourne l'URL HTTP locale que le QR code encode.
     * Ex: http://192.168.1.15:8787/fiche?id=30
     * Le telephone, quand il scanne, ouvre cette URL dans son navigateur.
     */
    private String buildQRContent(Employe emp) {
        demarrerServeurFiche(emp);
        String ip = obtenirIPLocale();
        return "http://" + ip + ":" + fichePort + "/fiche?id=" + emp.getId();
    }

    /**
     * Detecte l'IP locale du PC sur le reseau WiFi/LAN
     * (la meme que celle du telephone doit etre sur le meme reseau)
     */
    private String obtenirIPLocale() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    // Prendre IPv4 non-loopback
                    if (ip.contains(".") && !ip.startsWith("127")) return ip;
                }
            }
        } catch (Exception e) { System.err.println("[HTTP] IP: " + e.getMessage()); }
        return "127.0.0.1";
    }

    /**
     * Demarre le mini serveur HTTP sur le port fichePort.
     * Appele lors de la generation du QR — ne se redemarre pas si deja actif.
     */
    private void demarrerServeurFiche(Employe emp) {
        // Enregistrer l'employe dans le cache
        ficheCache.put(emp.getId(), emp);
        // Demarrer le serveur une seule fois
        if (ficheServeur != null) return;
        try {
            ficheServeur = HttpServer.create(new InetSocketAddress(fichePort), 0);
            ficheServeur.createContext("/fiche", new FicheHandler());
            ficheServeur.createContext("/photo", new PhotoHandler());
            ficheServeur.setExecutor(Executors.newCachedThreadPool());
            ficheServeur.start();
            System.out.println("[HTTP] Serveur fiche demarre sur port " + fichePort);
            // Arreter proprement a la fermeture de l'app
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                ficheServeur.stop(0);
                System.out.println("[HTTP] Serveur fiche arrete");
            }));
        } catch (Exception e) {
            System.err.println("[HTTP] Erreur demarrage serveur: " + e.getMessage());
        }
    }

    // ── Handler HTTP : /fiche?id=XX → HTML mobile ─────────────────────────
    static class FicheHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws java.io.IOException {
            String query = ex.getRequestURI().getQuery();
            int id = -1;
            if (query != null && query.startsWith("id=")) {
                try { id = Integer.parseInt(query.substring(3).split("&")[0]); } catch (Exception ignored) {}
            }
            Employe emp = ficheCache.get(id);
            String html = emp != null ? genererHtmlFiche(emp) : "<h1>Employé introuvable</h1>";
            byte[] bytes = html.getBytes("UTF-8");
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.getResponseBody().close();
        }

        static String genererHtmlFiche(Employe emp) {
            String photoTag;
            File photoFile = emp.hasPhoto() ? new File(emp.getPhotoPath()) : null;
            if (photoFile != null && photoFile.exists()) {
                photoTag = "<img src='/photo?id=" + emp.getId() + "' class='photo' alt='photo'/>";
            } else {
                // Avatar initiales
                String ini = (emp.getPrenom().isEmpty()?"?":String.valueOf(emp.getPrenom().charAt(0)).toUpperCase())
                        + (emp.getNom().isEmpty()?"?":String.valueOf(emp.getNom().charAt(0)).toUpperCase());
                String[] couleurs = {"#1abc9c","#3498db","#9b59b6","#e67e22","#e74c3c","#27ae60"};
                String bg = couleurs[Math.abs(emp.getId() % couleurs.length)];
                photoTag = "<div class='avatar' style='background:" + bg + "'>" + ini + "</div>";
            }

            String statutCls = emp.isActif() ? "actif" : "inactif";
            String statutTxt = emp.isActif() ? "✓ Actif" : "✗ Inactif";

            return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'/>"
                    + "<meta name='viewport' content='width=device-width,initial-scale=1'/>"
                    + "<title>Fiche Employé</title>"
                    + "<style>"
                    + "* { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }"
                    + "body { background: #f0f4f8; color: #2d3748; min-height: 100vh; }"
                    + ".header { background: linear-gradient(135deg, #27ae60, #1a5c38); color: white; padding: 24px 20px 40px; text-align: center; position: relative; }"
                    + ".header h1 { font-size: 14px; letter-spacing: 3px; opacity: 0.85; text-transform: uppercase; margin-bottom: 4px; }"
                    + ".header .app { font-size: 22px; font-weight: 800; }"
                    + ".card { background: white; border-radius: 20px; margin: -24px 16px 16px; padding: 24px; box-shadow: 0 8px 32px rgba(0,0,0,0.12); }"
                    + ".photo { width: 120px; height: 120px; border-radius: 50%; object-fit: cover; border: 4px solid #27ae60; display: block; margin: 0 auto 16px; box-shadow: 0 4px 16px rgba(0,0,0,0.15); }"
                    + ".avatar { width: 120px; height: 120px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 42px; font-weight: 800; color: white; margin: 0 auto 16px; box-shadow: 0 4px 16px rgba(0,0,0,0.2); }"
                    + ".name { text-align: center; font-size: 26px; font-weight: 700; color: #1a202c; margin-bottom: 6px; }"
                    + ".poste { text-align: center; font-size: 15px; color: #718096; margin-bottom: 16px; }"
                    + ".badge { display: inline-block; padding: 6px 20px; border-radius: 50px; font-size: 13px; font-weight: 700; letter-spacing: 1px; text-transform: uppercase; }"
                    + ".actif { background: #d4edda; color: #155724; }"
                    + ".inactif { background: #f8d7da; color: #721c24; }"
                    + ".badge-wrap { text-align: center; margin-bottom: 8px; }"
                    + ".section { background: white; border-radius: 16px; margin: 0 16px 16px; padding: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }"
                    + ".section-title { font-size: 11px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; color: #27ae60; margin-bottom: 16px; border-bottom: 2px solid #e8f5e9; padding-bottom: 8px; }"
                    + ".field { display: flex; align-items: flex-start; padding: 10px 0; border-bottom: 1px solid #f7fafc; }"
                    + ".field:last-child { border-bottom: none; }"
                    + ".field-icon { width: 36px; height: 36px; border-radius: 10px; background: #e8f5e9; display: flex; align-items: center; justify-content: center; font-size: 16px; margin-right: 14px; flex-shrink: 0; }"
                    + ".field-label { font-size: 11px; color: #a0aec0; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 2px; }"
                    + ".field-value { font-size: 15px; font-weight: 600; color: #2d3748; }"
                    + "a.field-value { color: #3182ce; text-decoration: none; }"
                    + ".footer { text-align: center; padding: 20px; font-size: 11px; color: #a0aec0; }"
                    + ".id-badge { position: absolute; top: 20px; right: 20px; background: rgba(255,255,255,0.2); color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; }"
                    + "</style></head><body>"
                    + "<div class='header'>"
                    + "<div class='id-badge'>#" + emp.getId() + "</div>"
                    + "<div class='app'>🌿 Ardhi</div>"
                    + "<h1>Fiche Employé</h1>"
                    + "</div>"
                    + "<div class='card'>"
                    + photoTag
                    + "<div class='name'>" + esc(emp.getPrenom()) + " " + esc(emp.getNom()) + "</div>"
                    + "<div class='poste'>" + esc(emp.getPoste()!=null?emp.getPoste():"—") + "</div>"
                    + "<div class='badge-wrap'><span class='badge " + statutCls + "'>" + statutTxt + "</span></div>"
                    + "</div>"
                    + "<div class='section'>"
                    + "<div class='section-title'>👤 Identité</div>"
                    + champ("🪪", "Nom complet", emp.getPrenom() + " " + emp.getNom())
                    + champ("💼", "Poste", emp.getPoste()!=null?emp.getPoste():"—")
                    + champ("🔢", "ID Employé", "#" + emp.getId())
                    + "</div>"
                    + "<div class='section'>"
                    + "<div class='section-title'>📞 Contact</div>"
                    + champEmail("📧", "Email", emp.getEmail()!=null?emp.getEmail():"—")
                    + champTel("📱", "Téléphone", emp.getTelephone()!=null?emp.getTelephone():"—")
                    + "</div>"
                    + "<div class='footer'>Ardhi · Fiche générée le "
                    + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + "</div></body></html>";
        }

        private static String esc(String s) {
            return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        }
        private static String champ(String icon, String label, String val) {
            return "<div class='field'><div class='field-icon'>" + icon + "</div>"
                    + "<div><div class='field-label'>" + label + "</div>"
                    + "<div class='field-value'>" + esc(val) + "</div></div></div>";
        }
        private static String champEmail(String icon, String label, String val) {
            return "<div class='field'><div class='field-icon'>" + icon + "</div>"
                    + "<div><div class='field-label'>" + label + "</div>"
                    + "<a class='field-value' href='mailto:" + esc(val) + "'>" + esc(val) + "</a></div></div>";
        }
        private static String champTel(String icon, String label, String val) {
            return "<div class='field'><div class='field-icon'>" + icon + "</div>"
                    + "<div><div class='field-label'>" + label + "</div>"
                    + "<a class='field-value' href='tel:" + esc(val) + "'>" + esc(val) + "</a></div></div>";
        }
    }

    // ── Handler HTTP : /photo?id=XX → image JPEG ─────────────────────────
    static class PhotoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws java.io.IOException {
            String query = ex.getRequestURI().getQuery();
            int id = -1;
            if (query != null && query.startsWith("id=")) {
                try { id = Integer.parseInt(query.substring(3).split("&")[0]); } catch (Exception ignored) {}
            }
            Employe emp = ficheCache.get(id);
            if (emp != null && emp.hasPhoto()) {
                File f = new File(emp.getPhotoPath());
                if (f.exists()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                    String mime = f.getName().endsWith(".png") ? "image/png" : "image/jpeg";
                    ex.getResponseHeaders().set("Content-Type", mime);
                    ex.sendResponseHeaders(200, bytes.length);
                    ex.getResponseBody().write(bytes);
                    ex.getResponseBody().close();
                    return;
                }
            }
            // 404
            ex.sendResponseHeaders(404, -1);
            ex.getResponseBody().close();
        }
    }

    /**
     * Cree le noeud JavaFX pour la cellule QR dans le tableau.
     * Clic → ouvre la fiche PDF de l'employe.
     */
    private javafx.scene.Node creerQRNode(Employe emp) {
        javafx.scene.image.Image qrImg = genererQRImage(emp, 80);
        if (qrImg == null) return new Label("—");

        ImageView iv = new ImageView(qrImg);
        iv.setFitWidth(80); iv.setFitHeight(80);
        iv.setPreserveRatio(true);

        // Tooltip
        Tooltip tip = new Tooltip("Cliquer pour générer la fiche de " + emp.getPrenom() + " " + emp.getNom());
        tip.setStyle("-fx-font-size:11px;");
        Tooltip.install(iv, tip);

        // Effet hover
        iv.setOnMouseEntered(e -> iv.setOpacity(0.75));
        iv.setOnMouseExited(e  -> iv.setOpacity(1.0));

        // Clic → fiche PDF
        iv.setOnMouseClicked(e -> genererFicheEmployePDF(emp));
        iv.setStyle("-fx-cursor: hand;");

        StackPane sp = new StackPane(iv);
        sp.setAlignment(javafx.geometry.Pos.CENTER);
        return sp;
    }

    /**
     * Genere et ouvre une fiche PDF complete pour un employe.
     * Format A4 portrait avec photo, QR code, tous les details.
     */
    @FXML
    private void genererFicheEmployePDF(Employe emp) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer la fiche de " + emp.getNom() + " " + emp.getPrenom());
        fc.setInitialFileName("fiche_" + emp.getNom() + "_" + emp.getPrenom() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File fichier = fc.showSaveDialog(tableEmployees.getScene().getWindow());
        if (fichier == null) return;

        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() {
                return creerFichePDF(emp, fichier);
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                NotificationToast.showNotification(
                        "✅ Fiche PDF générée : " + emp.getPrenom() + " " + emp.getNom(),
                        NotificationToast.SUCCESS, 3500);
                // Ouvrir automatiquement
                try { java.awt.Desktop.getDesktop().open(fichier); } catch (Exception ex) {}
            } else {
                NotificationToast.showNotification("❌ Erreur génération PDF", NotificationToast.ERROR, 3000);
            }
        });
        task.setOnFailed(e ->
                NotificationToast.showNotification("❌ " + task.getException().getMessage(), NotificationToast.ERROR, 3000));
        new Thread(task).start();
    }

    /**
     * Construit le PDF fiche employe avec iText.
     * Inclut : en-tête entreprise, photo de profil, QR code, tableau des details,
     * section informations de contact, et pied de page.
     */
    private boolean creerFichePDF(Employe emp, File fichier) {
        try {
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(
                    com.itextpdf.text.PageSize.A4, 40, 40, 50, 40);
            PdfWriter pdfWriter = PdfWriter.getInstance(
                    doc, new FileOutputStream(fichier));
            doc.open();

            // ── Couleurs & Polices ────────────────────────────────────────
            BaseColor VERT_ARDHI   = new BaseColor(39, 174, 96);
            BaseColor VERT_FONCE   = new BaseColor(27, 94, 32);
            BaseColor GRIS_CLAIR   = new BaseColor(245, 247, 250);
            BaseColor GRIS_TEXTE   = new BaseColor(80, 80, 80);
            BaseColor BLEU_ACCENT  = new BaseColor(41, 128, 185);

            com.itextpdf.text.Font fTitre    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, VERT_FONCE);
            com.itextpdf.text.Font fSousTitre= FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, VERT_ARDHI);
            com.itextpdf.text.Font fLabel    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GRIS_TEXTE);
            com.itextpdf.text.Font fValeur   = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
            com.itextpdf.text.Font fPied     = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.GRAY);
            com.itextpdf.text.Font fBadge    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);

            // ── EN-TETE : Logo Ardhi + titre ──────────────────────────────
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{3f, 1f});
            header.setSpacingAfter(20);

            // Titre + sous-titre dans cellule gauche
            PdfPCell cellTitre = new PdfPCell();
            cellTitre.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            cellTitre.setBackgroundColor(VERT_ARDHI);
            cellTitre.setPadding(16);

            com.itextpdf.text.Paragraph pTitre = new com.itextpdf.text.Paragraph();
            pTitre.add(new com.itextpdf.text.Chunk("🌿 ARDHI\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, BaseColor.WHITE)));
            pTitre.add(new com.itextpdf.text.Chunk("FICHE EMPLOYÉ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new BaseColor(200,255,200))));
            cellTitre.addElement(pTitre);
            header.addCell(cellTitre);

            // Date + ID dans cellule droite
            PdfPCell cellDate = new PdfPCell();
            cellDate.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            cellDate.setBackgroundColor(VERT_FONCE);
            cellDate.setPadding(16);
            cellDate.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
            com.itextpdf.text.Paragraph pDate = new com.itextpdf.text.Paragraph();
            pDate.add(new com.itextpdf.text.Chunk("ID : #" + emp.getId() + "\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.WHITE)));
            pDate.add(new com.itextpdf.text.Chunk(
                    java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, new BaseColor(180,220,180))));
            cellDate.addElement(pDate);
            header.addCell(cellDate);
            doc.add(header);

            // ── CORPS : Photo + QR Code ────────────────────────────────────
            PdfPTable photoQR = new PdfPTable(2);
            photoQR.setWidthPercentage(100);
            photoQR.setWidths(new float[]{1f, 1f});
            photoQR.setSpacingAfter(20);

            // Photo de profil
            PdfPCell cellPhoto = new PdfPCell();
            cellPhoto.setBorder(com.itextpdf.text.Rectangle.BOX);
            cellPhoto.setBorderColor(VERT_ARDHI);
            cellPhoto.setPadding(10);
            cellPhoto.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            cellPhoto.setBackgroundColor(GRIS_CLAIR);

            if (emp.hasPhoto()) {
                try {
                    java.io.File photoFile = new java.io.File(emp.getPhotoPath());
                    if (photoFile.exists()) {
                        com.itextpdf.text.Image photoImg =
                                com.itextpdf.text.Image.getInstance(photoFile.getAbsolutePath());
                        photoImg.scaleToFit(150, 150);
                        photoImg.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER);
                        cellPhoto.addElement(photoImg);
                    } else {
                        cellPhoto.addElement(new com.itextpdf.text.Paragraph("Pas de photo",
                                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, BaseColor.GRAY)));
                    }
                } catch (Exception ex) {
                    cellPhoto.addElement(new com.itextpdf.text.Paragraph("Photo indisponible",
                            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, BaseColor.GRAY)));
                }
            } else {
                // Avatar initiales en texte
                com.itextpdf.text.Paragraph pInitiales = new com.itextpdf.text.Paragraph(
                        (emp.getPrenom().isEmpty()?"?":String.valueOf(emp.getPrenom().charAt(0)).toUpperCase()) +
                                (emp.getNom().isEmpty()  ?"?":String.valueOf(emp.getNom().charAt(0)).toUpperCase()),
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 48, VERT_ARDHI));
                pInitiales.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                cellPhoto.addElement(pInitiales);
            }

            com.itextpdf.text.Paragraph lblPhoto = new com.itextpdf.text.Paragraph("PHOTO", fLabel);
            lblPhoto.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            cellPhoto.addElement(lblPhoto);
            photoQR.addCell(cellPhoto);

            // QR Code
            PdfPCell cellQR = new PdfPCell();
            cellQR.setBorder(com.itextpdf.text.Rectangle.BOX);
            cellQR.setBorderColor(VERT_ARDHI);
            cellQR.setPadding(10);
            cellQR.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            cellQR.setBackgroundColor(GRIS_CLAIR);
            try {
                // Generer QR code 200x200 en memoire
                String qrContent = buildQRContent(emp);
                java.util.Map<EncodeHintType, Object> hints = new java.util.EnumMap<>(EncodeHintType.class);
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                hints.put(EncodeHintType.MARGIN, 1);
                BitMatrix matrix = new QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, 200, 200, hints);
                BufferedImage qrBuf = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
                for (int x=0;x<200;x++) for (int y=0;y<200;y++)
                    qrBuf.setRGB(x, y, matrix.get(x,y) ? 0xFF1B5E20 : 0xFFFFFFFF);
                java.io.ByteArrayOutputStream qrBaos = new java.io.ByteArrayOutputStream();
                ImageIO.write(qrBuf, "png", qrBaos);
                com.itextpdf.text.Image qrImg =
                        com.itextpdf.text.Image.getInstance(qrBaos.toByteArray());
                qrImg.scaleToFit(150, 150);
                qrImg.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER);
                cellQR.addElement(qrImg);
            } catch (Exception ex) {
                cellQR.addElement(new com.itextpdf.text.Paragraph("QR indisponible", fLabel));
            }
            com.itextpdf.text.Paragraph lblQR = new com.itextpdf.text.Paragraph(
                    "Scannez pour voir les détails", fPied);
            lblQR.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            cellQR.addElement(lblQR);
            photoQR.addCell(cellQR);
            doc.add(photoQR);

            // ── SECTION : Identité ─────────────────────────────────────────
            doc.add(creerSectionTitre("👤 IDENTITÉ", VERT_ARDHI, VERT_FONCE));
            PdfPTable tIdentite = new PdfPTable(4);
            tIdentite.setWidthPercentage(100);
            tIdentite.setWidths(new float[]{1.2f, 2f, 1.2f, 2f});
            tIdentite.setSpacingAfter(14);
            ajouterLigneDetail(tIdentite, "Nom", emp.getNom(), "Prénom", emp.getPrenom(), fLabel, fValeur, GRIS_CLAIR);
            ajouterLigneDetail(tIdentite, "Poste", emp.getPoste()!=null?emp.getPoste():"—",
                    "Statut", emp.isActif()?"✓ Actif":"✗ Inactif", fLabel, fValeur, BaseColor.WHITE);
            doc.add(tIdentite);

            // ── SECTION : Contact ──────────────────────────────────────────
            doc.add(creerSectionTitre("📞 CONTACT", BLEU_ACCENT, new BaseColor(13,71,161)));
            PdfPTable tContact = new PdfPTable(4);
            tContact.setWidthPercentage(100);
            tContact.setWidths(new float[]{1.2f, 2f, 1.2f, 2f});
            tContact.setSpacingAfter(14);
            ajouterLigneDetail(tContact, "Email",
                    emp.getEmail()!=null?emp.getEmail():"—",
                    "Téléphone",
                    emp.getTelephone()!=null?emp.getTelephone():"—",
                    fLabel, fValeur, GRIS_CLAIR);
            doc.add(tContact);

            // ── Badge statut ──────────────────────────────────────────────
            PdfPTable tBadge = new PdfPTable(1);
            tBadge.setWidthPercentage(40);
            tBadge.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            tBadge.setSpacingAfter(20);
            PdfPCell badge = new PdfPCell(new com.itextpdf.text.Phrase(
                    emp.isActif() ? "  ✓  EMPLOYÉ ACTIF  " : "  ✗  EMPLOYÉ INACTIF  ", fBadge));
            badge.setBackgroundColor(emp.isActif() ? VERT_ARDHI : new BaseColor(192,57,43));
            badge.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            badge.setPadding(8);
            badge.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
            tBadge.addCell(badge);
            doc.add(tBadge);

            // ── PIED DE PAGE ──────────────────────────────────────────────
            com.itextpdf.text.Paragraph pied = new com.itextpdf.text.Paragraph(
                    "Document généré automatiquement par Ardhi · " +
                            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) +
                            " · Confidentiel", fPied);
            pied.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            pied.setSpacingBefore(20);
            doc.add(pied);

            doc.close();
            return true;

        } catch (Exception e) {
            System.err.println("[PDF Fiche] " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /** Cree un titre de section pour le PDF avec bande coloree */
    private PdfPTable creerSectionTitre(String texte, BaseColor bgClair, BaseColor bgFonce) {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(10);
        t.setSpacingAfter(8);
        PdfPCell c = new PdfPCell(new com.itextpdf.text.Phrase(
                texte, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE)));
        c.setBackgroundColor(bgFonce);
        c.setPadding(8);
        c.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        t.addCell(c);
        return t;
    }

    // ── ✉️ ATTESTATION DE PRÉSENCE ─────────────────────────────────────────

    @FXML
    private void handleEnvoyerAttestation() {
        Employe selected = tableEmployees.getSelectionModel().getSelectedItem();

        if (selected == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Veuillez sélectionner un employé avant d'envoyer l'attestation.",
                    ButtonType.OK).showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Envoi de l'attestation");
        confirm.setHeaderText("Envoyer l'attestation de présence à :");
        confirm.setContentText(selected.getPrenom() + " " + selected.getNom()
                + "\n" + selected.getEmail());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                attestationMailService.envoyerAttestation(selected);
                return null;
            }
        };

        task.setOnSucceeded(e ->
                NotificationToast.showNotification(
                        "✅ Attestation envoyée à " + selected.getEmail(),
                        NotificationToast.SUCCESS,
                        3000
                )
        );

        task.setOnFailed(e -> {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur d'envoi");
            err.setHeaderText("Impossible d'envoyer l'attestation");
            err.setContentText(task.getException().getMessage());
            err.showAndWait();
        });

        new Thread(task, "mail-attestation-thread").start();
    }

    /** Ajoute une ligne label/valeur / label/valeur dans un tableau PDF a 4 colonnes */
    private void ajouterLigneDetail(PdfPTable t,
                                    String l1, String v1, String l2, String v2,
                                    com.itextpdf.text.Font fL, com.itextpdf.text.Font fV, BaseColor bg) {
        PdfPCell c;

        c = new PdfPCell(new com.itextpdf.text.Phrase(l1, fL));
        c.setBackgroundColor(bg); c.setPadding(7); c.setBorderColor(BaseColor.LIGHT_GRAY);
        t.addCell(c);

        c = new PdfPCell(new com.itextpdf.text.Phrase(v1, fV));
        c.setBackgroundColor(BaseColor.WHITE); c.setPadding(7); c.setBorderColor(BaseColor.LIGHT_GRAY);
        t.addCell(c);

        c = new PdfPCell(new com.itextpdf.text.Phrase(l2, fL));
        c.setBackgroundColor(bg); c.setPadding(7); c.setBorderColor(BaseColor.LIGHT_GRAY);
        t.addCell(c);

        c = new PdfPCell(new com.itextpdf.text.Phrase(v2, fV));
        c.setBackgroundColor(BaseColor.WHITE); c.setPadding(7); c.setBorderColor(BaseColor.LIGHT_GRAY);
        t.addCell(c);
    }

}