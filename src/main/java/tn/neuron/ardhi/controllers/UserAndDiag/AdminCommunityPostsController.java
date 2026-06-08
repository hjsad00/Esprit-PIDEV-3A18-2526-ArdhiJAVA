package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.CommunityPost;
import tn.neuron.ardhi.services.UserAndDiag.CommunityPostService;
import tn.neuron.ardhi.utils.UserAndDiag.ImgBBService;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import javafx.concurrent.Task;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminCommunityPostsController implements Initializable {

    @FXML
    private TableView<CommunityPost> tablePosts;
    @FXML
    private TableColumn<CommunityPost, Integer> colId;
    @FXML
    private TableColumn<CommunityPost, String> colTitle;
    @FXML
    private TableColumn<CommunityPost, Integer> colUser; // Changed to Integer
    @FXML
    private TableColumn<CommunityPost, String> colDate;
    @FXML
    private TableColumn<CommunityPost, Boolean> colResolved;

    @FXML
    private TextField tfRecherche;
    @FXML
    private ComboBox<String> cbFilterStatus;
    @FXML
    private Label lblInfo;

    @FXML
    private TextField tfUserId; // New field
    @FXML
    private TextField tfTitle;
    @FXML
    private TextArea taDescription;
    @FXML
    private CheckBox cbResolved;
    @FXML
    private ImageView imgPreview;

    @FXML
    private VBox mainContainer;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private File selectedFile;
    private CommunityPostService postService = new CommunityPostService();
    private List<CommunityPost> allPosts = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
            colUser.setCellValueFactory(new PropertyValueFactory<>("userId")); // Bind to userId
            colDate.setCellValueFactory(cellData -> {
                if (cellData.getValue().getCreatedAt() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    return new SimpleStringProperty(dateFormat.format(cellData.getValue().getCreatedAt()));
                }
                return new SimpleStringProperty("");
            });
            colResolved.setCellValueFactory(new PropertyValueFactory<>("resolved"));
            colResolved.setCellFactory(col -> new TableCell<CommunityPost, Boolean>() {
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
            cbFilterStatus.setItems(FXCollections.observableArrayList("Tous", "Résolu", "Non Résolu"));
            cbFilterStatus.getSelectionModel().selectFirst();
            cbFilterStatus.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

            chargerPosts(); // Loads all posts initially
            updateButtonState(false);

            tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());

            tablePosts.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectedFile = null; // Reset selection
                    afficherDetails(newSelection);
                    updateButtonState(true);
                } else {
                    viderChamps();
                    updateButtonState(false);
                }
            });

            WindowUtils.setupTableDeselection(tablePosts, mainContainer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void chargerPosts() {
        try {
            allPosts = postService.getAllPosts(0);
            applyFilters();
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur", "Impossible de charger les posts : " + e.getMessage());
        }
    }

    private void applyFilters() {
        String keyword = tfRecherche.getText().toLowerCase().trim();
        String status = cbFilterStatus.getValue();

        List<CommunityPost> filteredList = new ArrayList<>();

        for (CommunityPost p : allPosts) {
            boolean matchesKeyword = keyword.isEmpty() ||
                    (p.getTitle() != null && p.getTitle().toLowerCase().contains(keyword)) ||
                    (p.getUserName() != null && p.getUserName().toLowerCase().contains(keyword));

            boolean matchesStatus = true;
            if ("Résolu".equals(status)) {
                matchesStatus = p.isResolved();
            } else if ("Non Résolu".equals(status)) {
                matchesStatus = !p.isResolved();
            }

            if (matchesKeyword && matchesStatus) {
                filteredList.add(p);
            }
        }

        tablePosts.setItems(FXCollections.observableArrayList(filteredList));
        WindowUtils.updateInfoLabel(lblInfo, filteredList.size(), "post");
    }

    private void afficherDetails(CommunityPost p) {
        tfUserId.setText(String.valueOf(p.getUserId()));
        tfTitle.setText(p.getTitle());
        taDescription.setText(p.getDescription());
        cbResolved.setSelected(p.isResolved());

        if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
            try {
                String url = p.getImageUrl();
                if (url.startsWith("http") || url.startsWith("file:")) {
                    imgPreview.setImage(new Image(url));
                } else {
                    imgPreview.setImage(new Image(new File(url).toURI().toString()));
                }
            } catch (Exception e) {
                imgPreview.setImage(null);
            }
        } else {
            imgPreview.setImage(null);
        }
    }

    @FXML
    void choisirImage(ActionEvent event) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Choisir une image pour le post");
        fc.getExtensionFilters()
                .add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        selectedFile = fc.showOpenDialog(null);

        if (selectedFile != null) {
            imgPreview.setImage(new javafx.scene.image.Image(selectedFile.toURI().toString()));
        }
    }

    @FXML
    void ajouterPost(ActionEvent event) {
        String userIdText = tfUserId.getText().trim();
        String title = tfTitle.getText();
        String description = taDescription.getText();

        if (title.isEmpty() || description.isEmpty() || userIdText.isEmpty()) {
            WindowUtils.showAlert("Erreur", "Titre, description et User ID requis.");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(userIdText);
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "User ID invalide.");
            return;
        }

        // Disable UI
        if (btnAjouter != null)
            btnAjouter.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String imgPath = "";
                if (selectedFile != null) {
                    try {
                        // Try ImgBB first
                        String uploadedUrl = ImgBBService.uploadImage(selectedFile);
                        if (uploadedUrl != null) {
                            imgPath = uploadedUrl;
                        } else {
                            // Fallback to local path without copying
                            LogUtils.warn(getClass(), "ImgBB upload failed, falling back to local path.");
                            imgPath = selectedFile.getAbsolutePath();
                        }
                    } catch (Exception e) {
                        LogUtils.error(getClass(), "Image processing error", e);
                        // Try one last time to just save locally if not already done
                        if (imgPath.isEmpty()) {
                            imgPath = selectedFile.getAbsolutePath();
                        }
                    }
                }

                CommunityPost p = new CommunityPost(userId, title, description, imgPath);
                postService.ajouter(p);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (btnAjouter != null)
                btnAjouter.setDisable(false);
            WindowUtils.showAlert("Succès", "Post ajouté !");
            chargerPosts();
            viderChamps();
        });

        task.setOnFailed(e -> {
            if (btnAjouter != null)
                btnAjouter.setDisable(false);
            Throwable ex = task.getException();
            if (ex instanceof SQLException) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("foreign key")
                        && ex.getMessage().toLowerCase().contains("user_id")) {
                    WindowUtils.showAlert("Erreur", "L'utilisateur avec l'ID spécifié n'existe pas.");
                } else {
                    WindowUtils.showAlert("Erreur", "Erreur SQL : " + ex.getMessage());
                }
            } else {
                WindowUtils.showAlert("Erreur", "Erreur : " + ex.getMessage());
            }
        });

        new Thread(task).start();
    }

    @FXML
    void modifierPost(ActionEvent event) {
        CommunityPost selected = tablePosts.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        int userId;
        try {
            userId = Integer.parseInt(tfUserId.getText().trim());
        } catch (NumberFormatException e) {
            WindowUtils.showAlert("Erreur", "User ID invalide.");
            return;
        }

        String title = tfTitle.getText();
        String description = taDescription.getText();
        boolean resolved = cbResolved.isSelected();

        // Disable UI
        if (btnModifier != null)
            btnModifier.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                selected.setUserId(userId);
                selected.setTitle(title);
                selected.setDescription(description);
                selected.setResolved(resolved);

                if (selectedFile != null) {
                    try {
                        String newPath;
                        // Try ImgBB first
                        String uploadedUrl = ImgBBService.uploadImage(selectedFile);
                        if (uploadedUrl != null) {
                            newPath = uploadedUrl;
                        } else {
                            // Fallback to local path without copying
                            LogUtils.warn(getClass(), "ImgBB upload failed, falling back to local path.");
                            newPath = selectedFile.getAbsolutePath();
                        }
                        selected.setImageUrl(newPath);
                    } catch (Exception e) {
                        LogUtils.error(getClass(), "Image update error", e);
                        selected.setImageUrl(selectedFile.getAbsolutePath());
                    }
                }

                postService.modifier(selected);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (btnModifier != null)
                btnModifier.setDisable(false);
            WindowUtils.showAlert("Succès", "Post modifié !");
            chargerPosts();
            tablePosts.getSelectionModel().clearSelection();
        });

        task.setOnFailed(e -> {
            if (btnModifier != null)
                btnModifier.setDisable(false);
            Throwable ex = task.getException();
            if (ex instanceof SQLException) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("foreign key")
                        && ex.getMessage().toLowerCase().contains("user_id")) {
                    WindowUtils.showAlert("Erreur", "L'utilisateur spécifié n'existe pas.");
                } else {
                    WindowUtils.showAlert("Erreur", "Erreur SQL : " + ex.getMessage());
                }
            } else {
                WindowUtils.showAlert("Erreur", "Erreur : " + ex.getMessage());
            }
        });

        new Thread(task).start();
    }

    @FXML
    void supprimerPost(ActionEvent event) {
        CommunityPost selected = tablePosts.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        if (WindowUtils.showConfirmation("Supprimer ce post ?", "Cette action est irréversible.")) {
            try {
                postService.supprimer(selected.getId());
                WindowUtils.showAlert("Succès", "Post supprimé !");
                chargerPosts();
                tablePosts.getSelectionModel().clearSelection();
            } catch (SQLException e) {
                WindowUtils.showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void rafraichir(ActionEvent event) {
        tfRecherche.clear();
        cbFilterStatus.getSelectionModel().selectFirst();
        chargerPosts();
    }

    @FXML
    void viderChamps(ActionEvent event) {
        viderChamps();
    }

    private void viderChamps() {
        tfUserId.clear();
        tfTitle.clear();
        taDescription.clear();
        cbResolved.setSelected(false);
        imgPreview.setImage(null);
        selectedFile = null;
        tablePosts.getSelectionModel().clearSelection();
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
