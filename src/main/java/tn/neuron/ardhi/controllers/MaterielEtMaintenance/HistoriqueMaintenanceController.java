package tn.neuron.ardhi.controllers.MaterielEtMaintenance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.neuron.ardhi.models.MaterielEtMaintenance.Maintenance;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.MaterielEtMaintenance.GoogleCalendarService;
import tn.neuron.ardhi.services.MaterielEtMaintenance.MaintenanceService;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class HistoriqueMaintenanceController implements Initializable {

    // ── Tableau ───────────────────────────────────────────────────
    @FXML private TableView<Maintenance>              tableMaintenances;
    @FXML private TableColumn<Maintenance, Integer>   colId;
    @FXML private TableColumn<Maintenance, String>    colMateriel;
    @FXML private TableColumn<Maintenance, String>    colType;
    @FXML private TableColumn<Maintenance, String>    colStatut;
    @FXML private TableColumn<Maintenance, LocalDate> colDatePlanifiee;
    @FXML private TableColumn<Maintenance, LocalDate> colDateRealisee;
    @FXML private TableColumn<Maintenance, Double>    colCout;
    @FXML private TableColumn<Maintenance, String>    colDescription;

    // ── Stats ─────────────────────────────────────────────────────
    @FXML private Label       lblTotalMaintenances;
    @FXML private Label       lblMaintenancesPlanifiees;
    @FXML private Label       lblMaintenancesEnCours;
    @FXML private Label       lblMaintenancesTerminees;
    @FXML private Label       lblMaintenancesEnRetard;
    @FXML private Label       lblMTBF;
    @FXML private Label       lblCoutTotal;
    @FXML private Label       lblCoutMoyenAnnuel;
    @FXML private ProgressBar progressCompletion;
    @FXML private Label       lblTauxCompletion;
    @FXML private Label       lblStatutResume;
    @FXML private Label       lblNbResultats;

    // ── Filtres ───────────────────────────────────────────────────
    @FXML private ComboBox<String> cmbFiltreStatut;
    @FXML private ComboBox<String> cmbFiltreType;
    @FXML private ComboBox<String> cmbFiltreMateriel;
    @FXML private DatePicker       dateDebut;
    @FXML private DatePicker       dateFin;
    @FXML private TextField        txtRecherche;
    @FXML private TextField        txtCoutMin;
    @FXML private TextField        txtCoutMax;

    // ── Pagination ────────────────────────────────────────────────
    @FXML private Label  lblPage;
    @FXML private Label  lblTotalPages;
    @FXML private Button btnPrecedent;
    @FXML private Button btnSuivant;

    // ── Actions ───────────────────────────────────────────────────
    @FXML private Button btnDetails;
    @FXML private Button btnTerminer;
    @FXML private Button btnAnnuler;
    @FXML private Button btnSupprimer;

    // ── État ──────────────────────────────────────────────────────
    private MaintenanceService    maintenanceService;
    private GoogleCalendarService googleCalendarService;
    private User                  utilisateurConnecte;

    private List<Maintenance>           toutesMaintenances   = new ArrayList<>();
    private List<Maintenance>           maintenancesFiltrees = new ArrayList<>();
    private ObservableList<Maintenance> pageCourante         = FXCollections.observableArrayList();

    /**
     * Cache : materiel_id → date_prochaine_maintenance du matériel
     * Sert à détecter les retards même quand date_planifiee est null dans maintenance
     */
    private Map<Integer, LocalDate> dateProchMaintenanceParMateriel = new HashMap<>();

    private static final int PAGE_SIZE = 20;
    private int pageIndex = 0;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            utilisateurConnecte = UserSession.getInstance().getUser();
        } catch (Exception e) {
            showError("Erreur", "Aucun utilisateur connecté.");
            return;
        }
        maintenanceService    = new MaintenanceService();
        googleCalendarService = new GoogleCalendarService(utilisateurConnecte.getEmail());

        configurerTableau();
        configurerFiltres();
        configurerSelection();
        chargerToutesMaintenances();
    }

    // ════════════════════════════════════════════════════════════
    //  CHARGEMENT DES DATES MATÉRIELS (pour détecter retards)
    // ════════════════════════════════════════════════════════════

    /**
     * Charge depuis la BDD la date_prochaine_maintenance de chaque matériel.
     * On s'en sert pour marquer "en retard" même quand date_planifiee est null
     * dans la table maintenance.
     */
    private void chargerDatesMateriels(int userId) {
        dateProchMaintenanceParMateriel.clear();
        try {
            Connection conn = MyDatabase.getInstance().getCnx();
            // Jointure : maintenance → materiel pour récupérer date_prochaine_maintenance
            String sql = "SELECT m.id_materiel, m.date_prochaine_maintenance " +
                    "FROM materiel m " +
                    "WHERE m.user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id_materiel");
                java.sql.Date d = rs.getDate("date_prochaine_maintenance");
                if (d != null) {
                    dateProchMaintenanceParMateriel.put(id, d.toLocalDate());
                }
            }
            ps.close();
        } catch (Exception e) {
            System.err.println("[HistoriqueMaintenance] chargerDatesMateriels : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  DÉTECTION RETARD — logique corrigée
    // ════════════════════════════════════════════════════════════

    /**
     * Une maintenance est "en retard" si elle n'est pas terminée/annulée ET que :
     *   1. Sa date_planifiee ou date_maintenance est passée, OU
     *   2. La date_prochaine_maintenance du matériel associé est passée
     *
     * Ceci couvre le cas où date_planifiee est null dans la table maintenance
     * mais le matériel a sa date_prochaine_maintenance dépassée.
     */
    private boolean estEnRetardParDate(Maintenance m) {
        if (m == null) return false;

        // Si déjà terminée ou annulée → pas en retard
        String statut = m.getStatut_maintenance();
        if (statut != null &&
                (statut.equalsIgnoreCase("terminee") || statut.equalsIgnoreCase("annulee"))) {
            return false;
        }

        LocalDate today = LocalDate.now();

        // 1. Vérifier date_planifiee de la maintenance elle-même
        LocalDate datePlan = m.getDate_planifiee();
        if (datePlan != null && datePlan.isBefore(today)) return true;

        // 2. Vérifier date_maintenance (date de réalisation prévue)
        LocalDate dateMaint = m.getDate_maintenance();
        if (dateMaint != null && dateMaint.isBefore(today)) return true;

        // 3. Vérifier date_prochaine_maintenance du matériel associé (cas principal)
        int materielId = m.getMateriel_id(); // getter sur l'id du matériel
        if (materielId > 0) {
            LocalDate dateMatériel = dateProchMaintenanceParMateriel.get(materielId);
            if (dateMatériel != null && dateMatériel.isBefore(today)) return true;
        }

        return false;
    }

    /**
     * Retourne la date pertinente à afficher pour une maintenance :
     * date_planifiee → date_maintenance → date_prochaine_maintenance du matériel
     */
    private LocalDate getDateEffective(Maintenance m) {
        if (m.getDate_planifiee() != null) return m.getDate_planifiee();
        if (m.getDate_maintenance() != null) return m.getDate_maintenance();
        if (m.getMateriel_id() > 0)
            return dateProchMaintenanceParMateriel.get(m.getMateriel_id());
        return null;
    }

    // ════════════════════════════════════════════════════════════
    //  TABLEAU
    // ════════════════════════════════════════════════════════════

    private void configurerTableau() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id_maintenance"));
        colMateriel.setCellValueFactory(new PropertyValueFactory<>("materiel_nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type_maintenance"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut_maintenance"));
        colDatePlanifiee.setCellValueFactory(new PropertyValueFactory<>("date_planifiee"));
        colDateRealisee.setCellValueFactory(new PropertyValueFactory<>("date_realisee"));
        if (colCout != null) colCout.setCellValueFactory(new PropertyValueFactory<>("cout"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Type coloré
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                switch (item.toLowerCase()) {
                    case "preventive" -> { setText("🛡️  Préventive"); setStyle("-fx-text-fill:#566734;-fx-font-weight:bold;"); }
                    case "corrective" -> { setText("🔧  Corrective"); setStyle("-fx-text-fill:#7A9148;-fx-font-weight:bold;"); }
                    case "urgente"    -> { setText("⚠️  Urgente");    setStyle("-fx-text-fill:#c0392b;-fx-font-weight:bold;"); }
                    case "revision"   -> { setText("🔍  Révision");   setStyle("-fx-text-fill:#4a6428;-fx-font-weight:bold;"); }
                    default           -> { setText(item);              setStyle(""); }
                }
            }
        });

        // Statut coloré — utilise estEnRetardParDate corrigé
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }

                boolean enRetard = false;
                try {
                    Maintenance m = getTableView().getItems().get(getIndex());
                    enRetard = estEnRetardParDate(m);
                } catch (Exception ignored) {}

                if (enRetard) {
                    setText("⚠️  En retard");
                    setStyle("-fx-background-color:#fde8e8;-fx-text-fill:#c0392b;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:3 8;");
                } else switch (item.toLowerCase()) {
                    case "planifiee" -> { setText("📅  Planifiée"); setStyle("-fx-background-color:#fff8e6;-fx-text-fill:#8a6200;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:3 8;"); }
                    case "en_cours"  -> { setText("🔧  En cours");  setStyle("-fx-background-color:#eef4e0;-fx-text-fill:#566734;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:3 8;"); }
                    case "terminee"  -> { setText("✅  Terminée");  setStyle("-fx-background-color:#e8f5e9;-fx-text-fill:#2e7d32;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:3 8;"); }
                    case "annulee"   -> { setText("❌  Annulée");   setStyle("-fx-background-color:#fde8e8;-fx-text-fill:#c0392b;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:3 8;"); }
                    default          -> { setText(item);             setStyle(""); }
                }
            }
        });

        // Date planifiée — utilise getDateEffective pour afficher même si date_planifiee est null
        colDatePlanifiee.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setStyle(""); return; }

                Maintenance m = null;
                try { m = getTableView().getItems().get(getIndex()); } catch (Exception ignored) {}

                // Utiliser date effective si date_planifiee est null
                LocalDate dateAffichee = (item != null) ? item : (m != null ? getDateEffective(m) : null);

                if (dateAffichee == null) { setText("—"); setStyle("-fx-text-fill:#bbb;"); return; }

                setText(FMT.format(dateAffichee));
                try {
                    if (m != null && estEnRetardParDate(m))
                        setStyle("-fx-text-fill:#c0392b;-fx-font-weight:bold;");
                    else if (dateAffichee.isBefore(LocalDate.now().plusDays(7)))
                        setStyle("-fx-text-fill:#e67e22;-fx-font-weight:bold;");
                    else
                        setStyle("-fx-text-fill:#566734;");
                } catch (Exception ignored) { setStyle(""); }
            }
        });

        // Date réalisée
        colDateRealisee.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText("—"); setStyle("-fx-text-fill:#bbb;"); return; }
                setText(FMT.format(item));
                setStyle("-fx-text-fill:#6ab87a;-fx-font-weight:bold;");
            }
        });

        tableMaintenances.setSortPolicy(table -> {
            FXCollections.sort(tableMaintenances.getItems(), tableMaintenances.getComparator());
            return true;
        });
        tableMaintenances.setItems(pageCourante);
    }

    // ════════════════════════════════════════════════════════════
    //  FILTRES
    // ════════════════════════════════════════════════════════════

    private void configurerFiltres() {
        cmbFiltreStatut.getItems().addAll("Tous", "Planifiées", "En cours", "Terminées", "En retard", "Annulées");
        cmbFiltreStatut.setValue("Tous");
        cmbFiltreType.getItems().addAll("Tous", "Préventive", "Corrective", "Urgente", "Révision");
        cmbFiltreType.setValue("Tous");
        cmbFiltreMateriel.setValue("Tous");

        cmbFiltreStatut.setOnAction(e -> appliquerFiltres());
        cmbFiltreType.setOnAction(e -> appliquerFiltres());
        cmbFiltreMateriel.setOnAction(e -> appliquerFiltres());
        dateDebut.setOnAction(e -> appliquerFiltres());
        dateFin.setOnAction(e -> appliquerFiltres());
        txtRecherche.textProperty().addListener((o, a, b) -> appliquerFiltres());
        if (txtCoutMin != null) txtCoutMin.textProperty().addListener((o, a, b) -> appliquerFiltres());
        if (txtCoutMax != null) txtCoutMax.textProperty().addListener((o, a, b) -> appliquerFiltres());
    }

    private void configurerSelection() {
        tableMaintenances.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            boolean s = sel != null;
            btnDetails.setDisable(!s);
            btnSupprimer.setDisable(!s);
            btnTerminer.setDisable(!s || !sel.isPlanifiee());
            btnAnnuler.setDisable(!s || !sel.isPlanifiee());
        });
    }

    // ════════════════════════════════════════════════════════════
    //  CHARGEMENT
    // ════════════════════════════════════════════════════════════

    private void chargerToutesMaintenances() {
        // 1. Charger les dates des matériels en PREMIER pour le calcul de retard
        chargerDatesMateriels(utilisateurConnecte.getId());

        // 2. Charger les maintenances
        toutesMaintenances = maintenanceService.getMaintenancesByUserId(utilisateurConnecte.getId());

        // 3. Alimenter le filtre matériel
        List<String> noms = toutesMaintenances.stream()
                .map(Maintenance::getMateriel_nom).filter(Objects::nonNull)
                .distinct().sorted().collect(Collectors.toList());
        cmbFiltreMateriel.getItems().clear();
        cmbFiltreMateriel.getItems().add("Tous");
        cmbFiltreMateriel.getItems().addAll(noms);
        cmbFiltreMateriel.setValue("Tous");

        appliquerFiltres();
    }

    // ════════════════════════════════════════════════════════════
    //  FILTRAGE
    // ════════════════════════════════════════════════════════════

    private void appliquerFiltres() {
        String statut    = cmbFiltreStatut.getValue();
        String type      = cmbFiltreType.getValue();
        String materiel  = cmbFiltreMateriel.getValue();
        String recherche = txtRecherche.getText().toLowerCase().trim();
        LocalDate debut  = dateDebut.getValue();
        LocalDate fin    = dateFin.getValue();
        double coutMin   = parseDouble(txtCoutMin != null ? txtCoutMin.getText() : "", 0);
        double coutMax   = parseDouble(txtCoutMax != null ? txtCoutMax.getText() : "", Double.MAX_VALUE);

        maintenancesFiltrees = toutesMaintenances.stream()
                .filter(m -> {
                    if ("Tous".equals(statut)) return true;
                    return switch (statut) {
                        case "Planifiées" -> "planifiee".equalsIgnoreCase(m.getStatut_maintenance());
                        case "En cours"   -> "en_cours".equalsIgnoreCase(m.getStatut_maintenance());
                        case "Terminées"  -> "terminee".equalsIgnoreCase(m.getStatut_maintenance());
                        case "Annulées"   -> "annulee".equalsIgnoreCase(m.getStatut_maintenance());
                        case "En retard"  -> estEnRetardParDate(m); // utilise la nouvelle logique
                        default -> true;
                    };
                })
                .filter(m -> {
                    if ("Tous".equals(type)) return true;
                    String t = m.getType_maintenance();
                    if (t == null) return false;
                    return switch (type) {
                        case "Préventive" -> "preventive".equalsIgnoreCase(t);
                        case "Corrective" -> "corrective".equalsIgnoreCase(t);
                        case "Urgente"    -> "urgente".equalsIgnoreCase(t);
                        case "Révision"   -> "revision".equalsIgnoreCase(t);
                        default -> true;
                    };
                })
                .filter(m -> "Tous".equals(materiel) || materiel.equals(m.getMateriel_nom()))
                .filter(m -> {
                    // Utiliser date effective pour le filtre de date aussi
                    LocalDate d = getDateEffective(m);
                    if (d == null) return true;
                    if (debut != null && d.isBefore(debut)) return false;
                    if (fin   != null && d.isAfter(fin))    return false;
                    return true;
                })
                .filter(m -> m.getCout() >= coutMin && m.getCout() <= coutMax)
                .filter(m -> {
                    if (recherche.isEmpty()) return true;
                    String nom  = m.getMateriel_nom()     != null ? m.getMateriel_nom().toLowerCase()     : "";
                    String desc = m.getDescription()      != null ? m.getDescription().toLowerCase()      : "";
                    String t    = m.getType_maintenance() != null ? m.getType_maintenance().toLowerCase() : "";
                    return nom.contains(recherche) || desc.contains(recherche) || t.contains(recherche);
                })
                .collect(Collectors.toList());

        pageIndex = 0;
        afficherPage();
        mettreAJourStatistiques();
    }

    // ════════════════════════════════════════════════════════════
    //  PAGINATION
    // ════════════════════════════════════════════════════════════

    private void afficherPage() {
        int total      = maintenancesFiltrees.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        pageIndex      = Math.min(pageIndex, totalPages - 1);
        int from = pageIndex * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        pageCourante.setAll(maintenancesFiltrees.subList(from, to));
        lblPage.setText("Page " + (pageIndex + 1));
        lblTotalPages.setText("/ " + totalPages + "  (" + total + " entrée" + (total > 1 ? "s" : "") + ")");
        btnPrecedent.setDisable(pageIndex == 0);
        btnSuivant.setDisable(pageIndex >= totalPages - 1);
        if (lblNbResultats != null)
            lblNbResultats.setText(total + " résultat" + (total > 1 ? "s" : "") + " trouvé" + (total > 1 ? "s" : ""));
    }

    @FXML private void handlePagePrecedente() { pageIndex--; afficherPage(); }
    @FXML private void handlePageSuivante()   { pageIndex++; afficherPage(); }

    // ════════════════════════════════════════════════════════════
    //  STATISTIQUES
    // ════════════════════════════════════════════════════════════

    private void mettreAJourStatistiques() {
        List<Maintenance> liste = toutesMaintenances;

        long total     = liste.size();
        long planif    = liste.stream().filter(Maintenance::isPlanifiee).count();
        long enCours   = liste.stream().filter(m -> "en_cours".equalsIgnoreCase(m.getStatut_maintenance())).count();
        long terminees = liste.stream().filter(Maintenance::isTerminee).count();
        long annulees  = liste.stream().filter(m -> "annulee".equalsIgnoreCase(m.getStatut_maintenance())).count();

        // En retard = calcul par date corrigé (inclut date_prochaine_maintenance du matériel)
        long retard = liste.stream().filter(this::estEnRetardParDate).count();

        if (lblTotalMaintenances    != null) lblTotalMaintenances.setText(String.valueOf(total));
        if (lblMaintenancesPlanifiees != null) lblMaintenancesPlanifiees.setText(String.valueOf(planif));
        if (lblMaintenancesEnCours  != null) lblMaintenancesEnCours.setText(String.valueOf(enCours));
        if (lblMaintenancesTerminees!= null) lblMaintenancesTerminees.setText(String.valueOf(terminees));
        if (lblMaintenancesEnRetard != null) lblMaintenancesEnRetard.setText(String.valueOf(retard));

        if (lblMTBF            != null) lblMTBF.setText("N/A");
        if (lblCoutTotal       != null) lblCoutTotal.setText("—");
        if (lblCoutMoyenAnnuel != null) lblCoutMoyenAnnuel.setText("—");

        if (total > 0) {
            double taux = (double) terminees / total;
            if (progressCompletion != null) progressCompletion.setProgress(taux);
            if (lblTauxCompletion  != null) lblTauxCompletion.setText(String.format("%.0f%%", taux * 100));
            if (lblStatutResume    != null)
                lblStatutResume.setText(
                        terminees + " terminée(s)  ·  " + planif + " planifiée(s)  ·  " +
                                enCours + " en cours  ·  " + retard + " en retard  ·  " + annulees + " annulée(s)"
                );
        }
    }

    // ════════════════════════════════════════════════════════════
    //  ACTIONS
    // ════════════════════════════════════════════════════════════

    @FXML
    private void handleTerminer() {
        Maintenance m = tableMaintenances.getSelectionModel().getSelectedItem();
        if (m == null) return;
        confirmer("Marquer comme terminée", "✅  " + m.getMateriel_nom(),
                "Confirmer la fin de cette maintenance ?")
                .ifPresent(ok -> {
                    if (maintenanceService.terminerMaintenance(m.getId_maintenance(), 0)) {
                        showSuccess("Terminée", "Maintenance marquée comme terminée.");
                        chargerToutesMaintenances();
                    } else showError("Erreur", "Impossible de terminer.");
                });
    }

    @FXML
    private void handleAnnuler() {
        Maintenance m = tableMaintenances.getSelectionModel().getSelectedItem();
        if (m == null) return;
        confirmer("Annuler", "Matériel : " + m.getMateriel_nom(), "Cette maintenance sera annulée.")
                .ifPresent(ok -> {
                    if (maintenanceService.annulerMaintenance(m.getId_maintenance())) {
                        if (m.getGoogle_calendar_event_id() != null)
                            googleCalendarService.annulerMaintenance(m.getGoogle_calendar_event_id());
                        showSuccess("Annulée", "Maintenance annulée.");
                        chargerToutesMaintenances();
                    } else showError("Erreur", "Impossible d'annuler.");
                });
    }

    @FXML
    private void handleSupprimer() {
        Maintenance m = tableMaintenances.getSelectionModel().getSelectedItem();
        if (m == null) return;
        confirmer("⚠️  Supprimer — irréversible", "Matériel : " + m.getMateriel_nom(),
                "Cette entrée sera définitivement supprimée.")
                .ifPresent(ok -> {
                    if (maintenanceService.supprimerMaintenance(m.getId_maintenance())) {
                        if (m.getGoogle_calendar_event_id() != null)
                            googleCalendarService.annulerMaintenance(m.getGoogle_calendar_event_id());
                        showSuccess("Supprimée", "Maintenance supprimée.");
                        chargerToutesMaintenances();
                    } else showError("Erreur", "Impossible de supprimer.");
                });
    }

    @FXML
    private void handleDetails() {
        Maintenance m = tableMaintenances.getSelectionModel().getSelectedItem();
        if (m == null) return;

        LocalDate dateEff = getDateEffective(m);
        String delai = "—";
        if (dateEff != null) {
            long j = ChronoUnit.DAYS.between(LocalDate.now(), dateEff);
            if      (j > 0)  delai = "Dans " + j + " jour(s)";
            else if (j == 0) delai = "Aujourd'hui";
            else             delai = "⚠️ En retard de " + Math.abs(j) + " jour(s)";
        }

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Détails — Maintenance #" + m.getId_maintenance());
        a.setHeaderText("🔧  " + m.getMateriel_nom());
        a.setContentText(
                "📋  Type       : " + str(m.getType_maintenance())   + "\n" +
                        "📊  Statut     : " + str(m.getStatut_maintenance())  + "\n" +
                        "📅  Planifiée  : " + date(m.getDate_planifiee())     + "\n" +
                        "✅  Réalisée   : " + date(m.getDate_realisee())       + "\n" +
                        "⏱️  Délai       : " + delai                           + "\n\n" +
                        "📝  Description :\n" + str(m.getDescription())
        );
        a.showAndWait();
    }

    @FXML private void handleActualiser()  { chargerToutesMaintenances(); showSuccess("Actualisé", "Données rechargées."); }
    @FXML private void handleGraphiques()  { /* compatibilité */ }

    @FXML
    private void handleResetFiltres() {
        cmbFiltreStatut.setValue("Tous");
        cmbFiltreType.setValue("Tous");
        cmbFiltreMateriel.setValue("Tous");
        dateDebut.setValue(null);
        dateFin.setValue(null);
        txtRecherche.clear();
        if (txtCoutMin != null) txtCoutMin.clear();
        if (txtCoutMax != null) txtCoutMax.clear();
        appliquerFiltres();
    }

    @FXML
    private void handleRetour() {
        Stage stage = (Stage) tableMaintenances.getScene().getWindow();
        stage.close();
    }

    // ════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════

    private Optional<ButtonType> confirmer(String titre, String header, String content) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(titre); a.setHeaderText(header); a.setContentText(content);
        return a.showAndWait().filter(b -> b == ButtonType.OK);
    }

    private double parseDouble(String txt, double defaut) {
        try { return txt != null && !txt.isBlank() ? Double.parseDouble(txt) : defaut; }
        catch (NumberFormatException e) { return defaut; }
    }

    private String date(LocalDate d) { return d != null ? FMT.format(d) : "—"; }
    private String str(String s)     { return s != null ? s : "—"; }

    private void showSuccess(String t, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showError(String t, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
