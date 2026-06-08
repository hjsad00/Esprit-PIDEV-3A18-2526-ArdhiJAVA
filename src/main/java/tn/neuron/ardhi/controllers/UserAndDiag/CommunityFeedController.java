package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.CommunityPost;
import tn.neuron.ardhi.services.UserAndDiag.CommunityPostService;
import tn.neuron.ardhi.services.UserAndDiag.SpeechToTextService;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CommunityFeedController implements Initializable {

    @FXML
    private VBox feedContainer;
    @FXML
    private TextField txtSearch;
    @FXML
    private Button btnMicSearch;

    private CommunityPostService communityService = new CommunityPostService();
    private final SpeechToTextService sttService = new SpeechToTextService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshFeed(null);

        // Add search listener
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                performSearch(newValue);
            });
        }
    }

    private void performSearch(String query) {
        feedContainer.getChildren().clear();
        new Thread(() -> {
            try {
                // Get query safely to avoid null
                String keyword = (query == null) ? "" : query.trim();
                List<CommunityPost> posts;
                if (keyword.isEmpty()) {
                    posts = communityService.recuperer();
                } else {
                    posts = communityService.rechercher(keyword);
                }

                javafx.application.Platform.runLater(() -> {
                    if (posts.isEmpty()) {
                        feedContainer.getChildren().add(new Label("Aucun post trouvé pour : " + keyword));
                    } else {
                        for (CommunityPost post : posts) {
                            feedContainer.getChildren().add(createPostCard(post));
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    void goBack(ActionEvent event) {
        WindowUtils.goBack(event);
    }

    @FXML
    void openCreatePost(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/CreatePost.fxml", "Poser une question");
    }

    @FXML
    void refreshFeed(ActionEvent event) {
        feedContainer.getChildren().clear();
        feedContainer.setStyle("-fx-padding: 30; -fx-background-color: transparent;");
        new Thread(() -> {
            try {
                // Get current user ID safely
                tn.neuron.ardhi.models.UserAndDiag.User user = tn.neuron.ardhi.utils.UserAndDiag.UserSession
                        .getInstance().getUser();
                int userId = (user != null) ? user.getId() : 0; // 0 or handle error

                List<CommunityPost> posts = communityService.getAllPosts(userId);
                javafx.application.Platform.runLater(() -> {
                    if (posts.isEmpty()) {
                        feedContainer.getChildren().add(new Label("Aucun post pour le moment. Soyez le premier !"));
                    } else {
                        for (CommunityPost post : posts) {
                            feedContainer.getChildren().add(createPostCard(post));
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private VBox createPostCard(CommunityPost post) {
        VBox card = new VBox(15);
        card.getStyleClass().add("card-post");
        card.setMaxWidth(800);

        // Header (Avatar + Name + Date + Status)
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Avatar
        Label avatar = new Label(post.getUserName().substring(0, 1).toUpperCase());
        int colorHash = Math.abs(post.getUserName().hashCode()) % 5;
        String[] colors = { "#E0F2F1", "#FCE4EC", "#E8EAF6", "#F3E5F5", "#FFF3E0" };
        String[] textColors = { "#00695C", "#AD1457", "#283593", "#6A1B9A", "#E65100" };

        avatar.setStyle(
                "-fx-background-color: " + colors[colorHash] + "; -fx-text-fill: " + textColors[colorHash]
                        + "; -fx-font-weight: bold; -fx-font-size: 14; -fx-min-width: 36; -fx-min-height: 36; -fx-alignment: center; -fx-background-radius: 18;");

        VBox meta = new VBox(2);
        Label author = new Label(post.getUserName());
        author.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827; -fx-font-size: 13;");

        Label date = new Label(post.getCreatedAt().toString().substring(0, 16));
        date.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11;");
        meta.getChildren().addAll(author, date);

        header.getChildren().addAll(avatar, meta);

        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().add(spacer);

        // Resolved Badge
        if (post.isResolved()) {
            Label resolvedBadge = new Label("RESOLU");
            resolvedBadge.setStyle(
                    "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-background-radius: 4; -fx-padding: 3 6; -fx-font-weight: bold; -fx-font-size: 10;");
            header.getChildren().add(resolvedBadge);
        }

        card.getChildren().add(header);

        // Content
        VBox contentBox = new VBox(8);
        Label title = new Label(post.getTitle());
        title.setWrapText(true);
        title.setStyle(
                "-fx-font-weight: 800; -fx-font-size: 18; -fx-text-fill: #111827; -fx-font-family: 'Segoe UI', sans-serif;");

        Label desc = new Label(post.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #4B5563; -fx-font-size: 14; -fx-line-spacing: 3;");
        // Limit description length for feed?
        if (desc.getText().length() > 200) {
            desc.setText(desc.getText().substring(0, 200) + "...");
        }

        contentBox.getChildren().addAll(title, desc);
        card.getChildren().add(contentBox);

        // Image Preview (Smaller)
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            try {
                ImageView img = new ImageView();
                boolean loaded = false;

                if (post.getImageUrl().startsWith("http")) {
                    img.setImage(new Image(post.getImageUrl(), true)); // Background loading
                    loaded = true;
                } else {
                    File file = new File(post.getImageUrl());
                    if (file.exists()) {
                        img.setImage(new Image(file.toURI().toString()));
                        loaded = true;
                    }
                }

                if (loaded) {
                    img.setFitHeight(180);
                    img.setPreserveRatio(true);
                    javafx.scene.layout.StackPane imgContainer = new javafx.scene.layout.StackPane(img);
                    imgContainer.setStyle("-fx-background-radius: 8; -fx-background-color: #F3F4F6;");
                    imgContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    card.getChildren().add(imgContainer);
                }
            } catch (Exception e) {
            }
        }

        // Footer (Counters)
        HBox footer = new HBox(20);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        footer.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));
        footer.setStyle("-fx-border-color: #F3F4F6; -fx-border-width: 1 0 0 0;");

        // Comments
        HBox comments = new HBox(6);
        comments.setAlignment(javafx.geometry.Pos.CENTER);
        Label txtComment = new Label("💬 " + post.getCommentCount());
        txtComment.setStyle("-fx-font-weight: bold; -fx-text-fill: #4B5563;");
        comments.getChildren().add(txtComment);

        // Likes
        HBox likes = new HBox(6);
        likes.setAlignment(javafx.geometry.Pos.CENTER);
        Label txtLike = new Label("👍 " + post.getLikes());
        if (post.isLikedByCurrentUser()) {
            txtLike.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563EB;");
        } else {
            txtLike.setStyle("-fx-font-weight: bold; -fx-text-fill: #4B5563;");
        }
        likes.getChildren().add(txtLike);

        // Dislikes
        HBox dislikes = new HBox(6);
        dislikes.setAlignment(javafx.geometry.Pos.CENTER);
        Label txtDislike = new Label("👎 " + post.getDislikes());
        if (post.isDislikedByCurrentUser()) {
            txtDislike.setStyle("-fx-font-weight: bold; -fx-text-fill: #DC2626;");
        } else {
            txtDislike.setStyle("-fx-font-weight: bold; -fx-text-fill: #4B5563;");
        }
        dislikes.getChildren().add(txtDislike);

        footer.getChildren().addAll(comments, likes, dislikes);
        card.getChildren().add(footer);

        // Click Action
        card.setOnMouseClicked(e -> {
            WindowUtils.loadScene(e, "/fxml/UserAndDiag/PostDetail.fxml", "Détail du Post",
                    (PostDetailController ctrl) -> ctrl.setPost(post));
        });

        return card;
    }

    // ── Voice Search ──

    @FXML
    void voiceSearch(ActionEvent event) {
        if (sttService.isRecording()) {
            btnMicSearch.setText("⏳");
            btnMicSearch.setDisable(true);
            sttService.stopAndTranscribe(
                    text -> {
                        txtSearch.setText(text);
                        btnMicSearch.setText("🎙️");
                        btnMicSearch.setDisable(false);
                    },
                    error -> {
                        btnMicSearch.setText("🎙️");
                        btnMicSearch.setDisable(false);
                    });
        } else {
            sttService.startRecording();
            btnMicSearch.setText("⏹️");
            btnMicSearch.setStyle(
                    "-fx-background-color: #e74c3c; -fx-background-radius: 20; -fx-cursor: hand; -fx-min-width: 38; -fx-min-height: 38;");
        }
    }
}