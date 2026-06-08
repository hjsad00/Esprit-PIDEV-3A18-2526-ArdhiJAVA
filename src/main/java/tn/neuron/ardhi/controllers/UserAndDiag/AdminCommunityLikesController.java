package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.CommunityLike;
import tn.neuron.ardhi.models.UserAndDiag.VoteType;
import tn.neuron.ardhi.services.UserAndDiag.CommunityLikeService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminCommunityLikesController implements Initializable {

    @FXML
    private TableView<CommunityLike> tableLikes;
    @FXML
    private TableColumn<CommunityLike, Integer> colId;
    @FXML
    private TableColumn<CommunityLike, Integer> colUserId;
    @FXML
    private TableColumn<CommunityLike, String> colTarget;
    @FXML
    private TableColumn<CommunityLike, String> colVoteType;
    @FXML
    private TableColumn<CommunityLike, String> colDate;

    @FXML
    private TextField tfRecherche;
    @FXML
    private ComboBox<String> cbFilterVoteType;
    @FXML
    private Label lblInfo;

    @FXML
    private TextField tfUserId;
    @FXML
    private TextField tfPostId;
    @FXML
    private TextField tfCommentId;
    @FXML
    private ComboBox<VoteType> cbVoteType;

    @FXML
    private VBox mainContainer;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private CommunityLikeService likeService = new CommunityLikeService();
    private List<CommunityLike> allLikes = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
            colTarget.setCellValueFactory(cellData -> {
                return new SimpleStringProperty(cellData.getValue().getTargetDescription());
            });
            colVoteType.setCellValueFactory(cellData -> {
                VoteType vt = cellData.getValue().getVoteType();
                return new SimpleStringProperty(vt != null ? vt.name() : "N/A");
            });
            colDate.setCellValueFactory(cellData -> {
                if (cellData.getValue().getCreatedAt() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    return new SimpleStringProperty(dateFormat.format(cellData.getValue().getCreatedAt()));
                }
                return new SimpleStringProperty("");
            });

            // Init Filter
            cbFilterVoteType.setItems(FXCollections.observableArrayList("Tous", "LIKE", "DISLIKE"));
            cbFilterVoteType.getSelectionModel().selectFirst();
            cbFilterVoteType.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

            // Init VoteType ComboBox for form
            cbVoteType.setItems(FXCollections.observableArrayList(VoteType.values()));
            cbVoteType.getSelectionModel().selectFirst();

            chargerLikes();
            updateButtonState(false);

            tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());

            tableLikes.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    afficherDetails(newSelection);
                    updateButtonState(true);
                } else {
                    viderChamps();
                    updateButtonState(false);
                }
            });

            WindowUtils.setupTableDeselection(tableLikes, mainContainer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void chargerLikes() {
        try {
            allLikes = likeService.recuperer();
            applyFilters();
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les votes : " + e.getMessage());
        }
    }

    private void applyFilters() {
        String keyword = tfRecherche.getText().toLowerCase().trim();
        String voteTypeFilter = cbFilterVoteType.getValue();

        List<CommunityLike> filteredList = new ArrayList<>();

        for (CommunityLike l : allLikes) {
            boolean matchesKeyword = keyword.isEmpty() ||
                    (l.getUserName() != null && l.getUserName().toLowerCase().contains(keyword)) ||
                    String.valueOf(l.getUserId()).contains(keyword) ||
                    (l.getPostId() != null && String.valueOf(l.getPostId()).contains(keyword)) ||
                    (l.getCommentId() != null && String.valueOf(l.getCommentId()).contains(keyword));

            boolean matchesVoteType = true;
            if ("LIKE".equals(voteTypeFilter)) {
                matchesVoteType = l.getVoteType() == VoteType.LIKE;
            } else if ("DISLIKE".equals(voteTypeFilter)) {
                matchesVoteType = l.getVoteType() == VoteType.DISLIKE;
            }

            if (matchesKeyword && matchesVoteType) {
                filteredList.add(l);
            }
        }

        tableLikes.setItems(FXCollections.observableArrayList(filteredList));
        WindowUtils.updateInfoLabel(lblInfo, filteredList.size(), "vote");
    }

    private void afficherDetails(CommunityLike l) {
        tfUserId.setText(String.valueOf(l.getUserId()));
        tfPostId.setText(l.getPostId() != null ? String.valueOf(l.getPostId()) : "");
        tfCommentId.setText(l.getCommentId() != null ? String.valueOf(l.getCommentId()) : "");
        cbVoteType.setValue(l.getVoteType());
    }

    @FXML
    void ajouterLike(ActionEvent event) {
        try {
            String userIdText = tfUserId.getText().trim();
            String postIdText = tfPostId.getText().trim();
            String commentIdText = tfCommentId.getText().trim();
            VoteType voteType = cbVoteType.getValue();

            if (userIdText.isEmpty()) {
                WindowUtils.showAlert("Erreur", "User ID requis.");
                return;
            }

            if (postIdText.isEmpty() && commentIdText.isEmpty()) {
                WindowUtils.showAlert("Erreur", "Post ID ou Comment ID requis.");
                return;
            }

            if (!postIdText.isEmpty() && !commentIdText.isEmpty()) {
                WindowUtils.showAlert("Erreur", "Spécifiez uniquement Post ID OU Comment ID, pas les deux.");
                return;
            }

            int userId = Integer.parseInt(userIdText);
            Integer postId = postIdText.isEmpty() ? null : Integer.parseInt(postIdText);
            Integer commentId = commentIdText.isEmpty() ? null : Integer.parseInt(commentIdText);

            CommunityLike like = new CommunityLike(userId, postId, commentId, voteType);
            likeService.ajouter(like);

            WindowUtils.showAlert("Succès", "Vote ajouté !");
            chargerLikes();
            viderChamps();
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID invalide.");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key")) {
                WindowUtils.showAlert("Erreur",
                        "L'utilisateur, post ou commentaire avec l'ID spécifié n'existe pas.");
            } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                WindowUtils.showAlert("Erreur", "Ce vote existe déjà.");
            } else {
                WindowUtils.showAlert("Erreur", "Erreur lors de l'ajout : " + e.getMessage());
            }
        }
    }

    @FXML
    void modifierLike(ActionEvent event) {
        CommunityLike selected = tableLikes.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        try {
            String userIdText = tfUserId.getText().trim();
            String postIdText = tfPostId.getText().trim();
            String commentIdText = tfCommentId.getText().trim();

            if (userIdText.isEmpty()) {
                WindowUtils.showAlert("Erreur", "User ID requis.");
                return;
            }

            if (postIdText.isEmpty() && commentIdText.isEmpty()) {
                WindowUtils.showAlert("Erreur", "Post ID ou Comment ID requis.");
                return;
            }

            selected.setUserId(Integer.parseInt(userIdText));
            selected.setPostId(postIdText.isEmpty() ? null : Integer.parseInt(postIdText));
            selected.setCommentId(commentIdText.isEmpty() ? null : Integer.parseInt(commentIdText));
            selected.setVoteType(cbVoteType.getValue());

            likeService.modifier(selected);
            WindowUtils.showAlert("Succès", "Vote modifié !");
            chargerLikes();
            tableLikes.getSelectionModel().clearSelection();
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "ID invalide.");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key")) {
                WindowUtils.showAlert("Erreur",
                        "L'utilisateur, post ou commentaire avec l'ID spécifié n'existe pas.");
            } else {
                WindowUtils.showAlert("Erreur", "Erreur lors de la modification : " + e.getMessage());
            }
        }
    }

    @FXML
    void supprimerLike(ActionEvent event) {
        CommunityLike selected = tableLikes.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        if (WindowUtils.showConfirmation("Supprimer ce vote ?", "Cette action est irréversible.")) {
            try {
                likeService.supprimer(selected.getId());
                WindowUtils.showAlert("Succès", "Vote supprimé !");
                chargerLikes();
                tableLikes.getSelectionModel().clearSelection();
            } catch (SQLException e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        cbFilterVoteType.getSelectionModel().selectFirst();
        chargerLikes();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
    }

    private void viderChamps() {
        tfUserId.clear();
        tfPostId.clear();
        tfCommentId.clear();
        cbVoteType.getSelectionModel().selectFirst();
        tableLikes.getSelectionModel().clearSelection();
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
