package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.CommunityComment;
import tn.neuron.ardhi.services.UserAndDiag.CommunityCommentService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminCommunityCommentsController implements Initializable {

    @FXML
    private TableView<CommunityComment> tableComments;
    @FXML
    private TableColumn<CommunityComment, Integer> colId;
    @FXML
    private TableColumn<CommunityComment, Integer> colPostId;
    @FXML
    private TableColumn<CommunityComment, Integer> colParentId;
    @FXML
    private TableColumn<CommunityComment, String> colContent;
    @FXML
    private TableColumn<CommunityComment, Integer> colUser; // Changed to Integer
    @FXML
    private TableColumn<CommunityComment, String> colDate;
    @FXML
    private TableColumn<CommunityComment, Boolean> colSolution;

    @FXML
    private TextField tfRecherche;
    @FXML
    private ComboBox<String> cbFilterStatus;
    @FXML
    private Label lblInfo;

    @FXML
    private TextField tfUserId; // New field
    @FXML
    private TextArea taContent;
    @FXML
    private CheckBox cbSolution;
    @FXML
    private TextField tfPostId;
    @FXML
    private TextField tfParentId;

    @FXML
    private VBox mainContainer;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private CommunityCommentService commentService = new CommunityCommentService();
    private List<CommunityComment> allComments = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colPostId.setCellValueFactory(new PropertyValueFactory<>("postId"));
            colParentId.setCellValueFactory(new PropertyValueFactory<>("parentCommentId"));
            colContent.setCellValueFactory(new PropertyValueFactory<>("content"));
            colUser.setCellValueFactory(new PropertyValueFactory<>("userId")); // Bind to userId
            colDate.setCellValueFactory(cellData -> {
                if (cellData.getValue().getCreatedAt() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    return new SimpleStringProperty(dateFormat.format(cellData.getValue().getCreatedAt()));
                }
                return new SimpleStringProperty("");
            });
            colSolution.setCellValueFactory(new PropertyValueFactory<>("solution"));
            colSolution.setCellFactory(col -> new TableCell<CommunityComment, Boolean>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item ? "Vrai" : "Faux");
                    }
                }
            });

            // Init Filter
            cbFilterStatus.setItems(FXCollections.observableArrayList("Tous", "Solution", "Non Solution"));
            cbFilterStatus.getSelectionModel().selectFirst();
            cbFilterStatus.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

            chargerComments();
            updateButtonState(false);

            tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());

            tableComments.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    afficherDetails(newSelection);
                    updateButtonState(true);
                } else {
                    viderChamps();
                    updateButtonState(false);
                }
            });

            WindowUtils.setupTableDeselection(tableComments, mainContainer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void chargerComments() {
        try {
            allComments = commentService.recuperer();
            applyFilters();
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les commentaires : " + e.getMessage());
        }
    }

    private void applyFilters() {
        String keyword = tfRecherche.getText().toLowerCase().trim();
        String status = cbFilterStatus.getValue();

        List<CommunityComment> filteredList = new ArrayList<>();

        for (CommunityComment c : allComments) {
            boolean matchesKeyword = keyword.isEmpty() ||
                    (c.getContent() != null && c.getContent().toLowerCase().contains(keyword)) ||
                    (c.getUserName() != null && c.getUserName().toLowerCase().contains(keyword));

            boolean matchesStatus = true;
            if ("Solution".equals(status)) {
                matchesStatus = c.isSolution();
            } else if ("Non Solution".equals(status)) {
                matchesStatus = !c.isSolution();
            }

            if (matchesKeyword && matchesStatus) {
                filteredList.add(c);
            }
        }

        tableComments.setItems(FXCollections.observableArrayList(filteredList));
        WindowUtils.updateInfoLabel(lblInfo, filteredList.size(), "commentaire");
    }

    private void afficherDetails(CommunityComment c) {
        tfUserId.setText(String.valueOf(c.getUserId()));
        taContent.setText(c.getContent());
        cbSolution.setSelected(c.isSolution());
        tfPostId.setText(String.valueOf(c.getPostId()));

        if (c.getParentCommentId() != null) {
            tfParentId.setText(String.valueOf(c.getParentCommentId()));
        } else {
            tfParentId.clear();
        }
    }

    @FXML
    void ajouterComment(ActionEvent event) {
        try {
            int postId = Integer.parseInt(tfPostId.getText().trim());
            String userIdText = tfUserId.getText().trim();
            String content = taContent.getText();

            if (content.isEmpty() || userIdText.isEmpty()) {
                WindowUtils.showAlert("Erreur", "Contenu et User ID requis.");
                return;
            }

            int currentUserId = Integer.parseInt(userIdText);

            CommunityComment c = new CommunityComment(0, postId, currentUserId, "", content, null, 0, 0,
                    cbSolution.isSelected(), null);
            commentService.ajouter(c);

            WindowUtils.showAlert("Succès", "Commentaire ajouté !");
            chargerComments();
            viderChamps();
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID Post ou User ID invalide.");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key")
                    && e.getMessage().toLowerCase().contains("user_id")) {
                WindowUtils.showAlert("Erreur",
                        "L'utilisateur avec l'ID spécifié n'existe pas. Veuillez vérifier le User ID.");
            } else {
                WindowUtils.showAlert("Erreur", "Erreur lors de l'ajout : " + e.getMessage());
            }
        }
    }

    @FXML
    void modifierComment(ActionEvent event) {
        CommunityComment selected = tableComments.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        try {
            selected.setContent(taContent.getText());
            selected.setSolution(cbSolution.isSelected());
            selected.setPostId(Integer.parseInt(tfPostId.getText().trim()));
            selected.setUserId(Integer.parseInt(tfUserId.getText().trim()));

            String pIdText = tfParentId.getText().trim();
            if (pIdText.isEmpty()) {
                selected.setParentCommentId(null);
            } else {
                selected.setParentCommentId(Integer.parseInt(pIdText));
            }

            commentService.modifier(selected);
            WindowUtils.showAlert("Succès", "Commentaire modifié !");
            chargerComments();
            tableComments.getSelectionModel().clearSelection();
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID Post ou User ID invalide.");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key")
                    && e.getMessage().toLowerCase().contains("user_id")) {
                WindowUtils.showAlert("Erreur",
                        "L'utilisateur avec l'ID spécifié n'existe pas. Veuillez vérifier le User ID.");
            } else {
                WindowUtils.showAlert("Erreur", "Erreur lors de la modification : " + e.getMessage());
            }
        }
    }

    @FXML
    void supprimerComment(ActionEvent event) {
        CommunityComment selected = tableComments.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        if (WindowUtils.showConfirmation("Supprimer ce commentaire ?", "Cette action est irréversible.")) {
            try {
                commentService.supprimer(selected.getId());
                WindowUtils.showAlert("Succès", "Commentaire supprimé !");
                chargerComments();
                tableComments.getSelectionModel().clearSelection();
            } catch (SQLException e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        cbFilterStatus.getSelectionModel().selectFirst();
        chargerComments();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
    }

    private void viderChamps() {
        tfUserId.clear();
        taContent.clear();
        cbSolution.setSelected(false);
        tfPostId.clear();
        tfParentId.clear();
        tableComments.getSelectionModel().clearSelection();
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
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Admin Dashboard");
    }
}
