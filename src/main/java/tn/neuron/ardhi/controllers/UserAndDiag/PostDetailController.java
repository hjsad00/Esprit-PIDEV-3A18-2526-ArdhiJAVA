package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.CommunityComment;
import tn.neuron.ardhi.models.UserAndDiag.CommunityPost;
import tn.neuron.ardhi.services.UserAndDiag.CommunityPostService;
import tn.neuron.ardhi.services.UserAndDiag.CommunityCommentService;
import tn.neuron.ardhi.services.UserAndDiag.SpeechToTextService;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.io.File;
import java.util.List;

public class PostDetailController {

    @FXML
    private VBox contentContainer;

    @FXML
    private javafx.scene.control.ScrollPane scrollPane;

    @FXML
    private TextArea txtComment;
    @FXML
    private Button btnMicComment;

    private CommunityPost post;
    private final CommunityPostService postService = new CommunityPostService();
    private final CommunityCommentService commentService = new CommunityCommentService();
    private final SpeechToTextService sttService = new SpeechToTextService();

    public void setPost(CommunityPost post) {
        this.post = post;
        // Reset container to force full rebuild

        renderPost();
        loadComments(false);
    }

    @FXML
    void goBack(ActionEvent event) {
        WindowUtils.goBack(event);
    }

    @FXML
    private void renderPost() {
        contentContainer.getChildren().clear();
        contentContainer.setStyle("-fx-padding: 30; -fx-background-color: transparent;");

        VBox mainCard = new VBox(15);
        mainCard.getStyleClass().add("card-post");

        // 1. Header (Avatar placeholder + Name + Date + Resolved Status)
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Avatar
        Label avatar = new Label(post.getUserName().substring(0, 1).toUpperCase());
        int colorHash = Math.abs(post.getUserName().hashCode()) % 5;
        String[] colors = { "#E0F2F1", "#FCE4EC", "#E8EAF6", "#F3E5F5", "#FFF3E0" };
        String[] textColors = { "#00695C", "#AD1457", "#283593", "#6A1B9A", "#E65100" };

        avatar.setStyle(
                "-fx-background-color: " + colors[colorHash] + "; -fx-text-fill: " + textColors[colorHash]
                        + "; -fx-font-weight: bold; -fx-font-size: 16; -fx-min-width: 44; -fx-min-height: 44; -fx-alignment: center; -fx-background-radius: 22;");

        VBox metaBox = new VBox(2);
        Label authorName = new Label(post.getUserName());
        authorName.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #111827;");

        Label dateLabel = new Label(post.getCreatedAt().toString().substring(0, 16));
        dateLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12;");

        metaBox.getChildren().addAll(authorName, dateLabel);

        header.getChildren().addAll(avatar, metaBox);

        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().add(spacer);

        if (post.isResolved()) {
            Label resolvedBadge = new Label("RESOLU");
            resolvedBadge.setStyle(
                    "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-weight: bold; -fx-font-size: 10;");
            header.getChildren().add(resolvedBadge);
        }

        mainCard.getChildren().add(header);

        // 2. Title
        Label title = new Label(post.getTitle());
        title.setStyle(
                "-fx-font-size: 22; -fx-font-weight: 800; -fx-text-fill: #111827; -fx-font-family: 'Segoe UI', sans-serif;");
        title.setWrapText(true);
        mainCard.getChildren().add(title);

        // 3. Description
        Label desc = new Label(post.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 15; -fx-text-fill: #374151; -fx-line-spacing: 4;");
        mainCard.getChildren().add(desc);

        // 4. Image
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
                    img.setFitWidth(600); // dynamic max width?
                    img.setPreserveRatio(true);
                    javafx.scene.layout.StackPane imgContainer = new javafx.scene.layout.StackPane(img);
                    imgContainer.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8; -fx-padding: 10;");
                    mainCard.getChildren().add(imgContainer);
                }
            } catch (Exception e) {
            }
        }

        // Separator
        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        mainCard.getChildren().add(sep);

        // 5. Action Bar
        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(15);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.control.Button btnLike = new javafx.scene.control.Button("👍 " + post.getLikes());
        styleLikeButton(btnLike, post.getUserVote() == 1);

        javafx.scene.control.Button btnDislike = new javafx.scene.control.Button("👎 " + post.getDislikes());
        styleDislikeButton(btnDislike, post.getUserVote() == -1);

        btnLike.setOnAction(e -> handleVotePost(1, btnLike, btnDislike));
        btnDislike.setOnAction(e -> handleVotePost(-1, btnLike, btnDislike));

        actions.getChildren().addAll(btnLike, btnDislike);
        mainCard.getChildren().add(actions);

        contentContainer.getChildren().add(mainCard);

        // Comments Section Label
        Label commentsLabel = new Label("Commentaires");
        commentsLabel
                .setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #111827; -fx-padding: 20 0 10 5;");
        contentContainer.getChildren().add(commentsLabel);
    }

    private void styleLikeButton(javafx.scene.control.Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                    "-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-border-color: #2563EB; -fx-border-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;");
        } else {
            btn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #4B5563; -fx-border-color: #D1D5DB; -fx-border-radius: 5; -fx-cursor: hand;");
        }
    }

    private void styleDislikeButton(javafx.scene.control.Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                    "-fx-background-color: #FEF2F2; -fx-text-fill: #DC2626; -fx-border-color: #DC2626; -fx-border-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;");
        } else {
            btn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #4B5563; -fx-border-color: #D1D5DB; -fx-border-radius: 5; -fx-cursor: hand;");
        }
    }

    private void handleVotePost(int voteType, javafx.scene.control.Button btnLike,
            javafx.scene.control.Button btnDislike) {
        new Thread(() -> {
            try {
                int userId = UserSession.getInstance().getUser().getId();
                int newVoteState;
                if (voteType == 1) {
                    newVoteState = postService.toggleLikePost(userId, post.getId());
                } else {
                    newVoteState = postService.toggleDislikePost(userId, post.getId());
                }

                // Update Local Model Logic
                int oldVote = post.getUserVote();

                // Remove old stats
                if (oldVote == 1)
                    post.setLikes(post.getLikes() - 1);
                if (oldVote == -1)
                    post.setDislikes(post.getDislikes() - 1);

                // Add new stats
                if (newVoteState == 1)
                    post.setLikes(post.getLikes() + 1);
                if (newVoteState == -1)
                    post.setDislikes(post.getDislikes() + 1);

                post.setUserVote(newVoteState);

                // Update UI in-place (no scroll reset)
                javafx.application.Platform.runLater(() -> {
                    btnLike.setText("👍 " + post.getLikes());
                    styleLikeButton(btnLike, post.getUserVote() == 1);

                    btnDislike.setText("👎 " + post.getDislikes());
                    styleDislikeButton(btnDislike, post.getUserVote() == -1);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private CommunityComment replyingToComment = null; // State to track reply target
    @FXML
    private Label lblReplyingTo; // Optional UI to show who we are replying to (add to FXML or dynamic)

    private void loadComments(boolean scrollToBottom) {
        new Thread(() -> {
            try {
                int currentUserId = UserSession.getInstance().getUser().getId();
                List<CommunityComment> comments = commentService.getCommentsForPost(post.getId(), currentUserId);
                boolean isAuthor = (post.getUserId() == currentUserId);

                // Organize Into Hierarchy
                // 1. Map ID -> Comment
                java.util.Map<Integer, CommunityComment> commentMap = new java.util.HashMap<>();
                for (CommunityComment c : comments)
                    commentMap.put(c.getId(), c);

                // 2. Identify Roots and Children
                List<CommunityComment> rootComments = new java.util.ArrayList<>();
                java.util.Map<Integer, List<CommunityComment>> childrenMap = new java.util.HashMap<>();

                for (CommunityComment c : comments) {
                    if (c.getParentCommentId() == null) {
                        rootComments.add(c);
                    } else {
                        childrenMap.computeIfAbsent(c.getParentCommentId(), k -> new java.util.ArrayList<>()).add(c);
                    }
                }

                javafx.application.Platform.runLater(() -> {
                    renderPost();
                    Label header = new Label("Réponses (" + comments.size() + ")");
                    header.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 10 0; -fx-font-size: 16;");
                    contentContainer.getChildren().add(header);

                    // Add Roots recursively
                    for (CommunityComment root : rootComments) {
                        renderCommentTree(root, childrenMap, isAuthor, currentUserId, 0);
                    }

                    if (scrollToBottom && scrollPane != null) {
                        // Slight delay to ensure layout is updated before scrolling
                        javafx.application.Platform.runLater(() -> scrollPane.setVvalue(1.0));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void renderCommentTree(CommunityComment c, java.util.Map<Integer, List<CommunityComment>> childrenMap,
            boolean isPostAuthor, int currentUserId, int depth) {
        contentContainer.getChildren().add(createCommentBox(c, isPostAuthor, currentUserId, depth));
        if (childrenMap.containsKey(c.getId())) {
            for (CommunityComment child : childrenMap.get(c.getId())) {
                renderCommentTree(child, childrenMap, isPostAuthor, currentUserId, depth + 1);
            }
        }
    }

    private VBox createCommentBox(CommunityComment c, boolean isPostAuthor, int currentUserId, int depth) {
        VBox outerBox = new VBox();

        // Left margin for depth
        int marginLeft = Math.min(depth * 35, 200);
        VBox.setMargin(outerBox, new javafx.geometry.Insets(0, 0, 8, marginLeft));

        // The card itself
        VBox card = new VBox(8);
        card.getStyleClass().add("card-post");

        // Different styling for solution
        if (c.isSolution()) {
            card.setStyle("-fx-background-color: #F0FDF4; -fx-border-color: #BBF7D0;");
        }

        outerBox.getChildren().add(card);

        // Header Row
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Avatar Small
        Label avatar = new Label(c.getUserName().substring(0, 1).toUpperCase());
        int colorHash = Math.abs(c.getUserName().hashCode()) % 5;
        String[] colors = { "#E0F2F1", "#FCE4EC", "#E8EAF6", "#F3E5F5", "#FFF3E0" };
        String[] textColors = { "#00695C", "#AD1457", "#283593", "#6A1B9A", "#E65100" };

        avatar.setStyle(
                "-fx-background-color: " + colors[colorHash] + "; -fx-text-fill: " + textColors[colorHash]
                        + "; -fx-font-weight: bold; -fx-font-size: 11; -fx-min-width: 24; -fx-min-height: 24; -fx-alignment: center; -fx-background-radius: 12;");

        Label author = new Label(c.getUserName());
        author.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #1F2937;");
        header.getChildren().addAll(avatar, author);

        // OP Badge
        if (c.getUserId() == post.getUserId()) {
            Label opBadge = new Label("OP");
            // Enterprise style badge
            opBadge.setStyle(
                    "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-font-size: 9; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 10;");
            javafx.scene.control.Tooltip.install(opBadge, new javafx.scene.control.Tooltip("Auteur du post"));
            header.getChildren().add(opBadge);
        }

        if (c.isSolution()) {
            Label badge = new Label("SOLUTION");
            badge.setStyle(
                    "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-font-size: 9; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 10;");
            header.getChildren().add(badge);
        }

        card.getChildren().add(header);

        // Content
        Label content = new Label(c.getContent());
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: #4B5563; -fx-font-size: 13;");
        card.getChildren().add(content);

        // Actions Row
        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(12);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        actions.setPadding(new javafx.geometry.Insets(5, 0, 0, 0));

        // Vote Buttons
        // Vote Buttons
        javafx.scene.control.Button btnLike = new javafx.scene.control.Button("👍 " + c.getLikes());
        styleSmallButton(btnLike, c.getUserVote() == 1);
        btnLike.setOnAction(e -> handleVoteComment(c, 1, btnLike, actions));

        javafx.scene.control.Button btnDislike = new javafx.scene.control.Button("👎 " + c.getDislikes());
        styleSmallButton(btnDislike, c.getUserVote() == -1);
        btnDislike.setOnAction(e -> handleVoteComment(c, -1, btnDislike, actions));

        // Reply Button
        javafx.scene.control.Button btnReply = new javafx.scene.control.Button("Répondre");
        btnReply.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-font-size: 11; -fx-cursor: hand; -fx-font-weight: bold;");
        btnReply.setOnMouseEntered(e -> btnReply.setStyle(
                "-fx-background-color: #F3F4F6; -fx-text-fill: #374151; -fx-font-size: 11; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 4;"));
        btnReply.setOnMouseExited(e -> btnReply.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-font-size: 11; -fx-cursor: hand; -fx-font-weight: bold;"));
        btnReply.setOnAction(e -> initiateReply(c));

        actions.getChildren().addAll(btnLike, btnDislike, btnReply);

        if (isPostAuthor && !post.isResolved() && !c.isSolution()) {
            javafx.scene.control.Button btnSolve = new javafx.scene.control.Button("✓ Marquer comme solution");
            btnSolve.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #166534; -fx-font-size: 11; -fx-cursor: hand; -fx-font-weight: bold;");
            btnSolve.setOnAction(e -> handleMarkSolution(c));
            actions.getChildren().add(btnSolve);
        }

        card.getChildren().add(actions);
        return outerBox;
    }

    private void styleSmallButton(javafx.scene.control.Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                    "-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-background-radius: 5; -fx-font-size: 11; -fx-padding: 3 8;");
        } else {
            btn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-background-radius: 5; -fx-font-size: 11; -fx-padding: 3 8;");
        }
    }

    private void initiateReply(CommunityComment c) {
        this.replyingToComment = c;
        txtComment.setPromptText("Réponse à " + c.getUserName() + " (Échap pour annuler)...");
        txtComment.requestFocus();
    }

    // Add logic to cancel reply on ESC key if possible, or just click elsewhere?
    // We will just handle send logic.

    private void handleVoteComment(CommunityComment c, int voteType, javafx.scene.control.Button clickedBtn,
            javafx.scene.layout.HBox actions) {
        new Thread(() -> {
            try {
                int userId = UserSession.getInstance().getUser().getId();
                int newVoteState;
                if (voteType == 1) {
                    newVoteState = commentService.toggleLikeComment(userId, c.getId());
                } else {
                    newVoteState = commentService.toggleDislikeComment(userId, c.getId());
                }
                int oldVote = c.getUserVote();
                if (oldVote == 1)
                    c.setLikes(c.getLikes() - 1);
                if (oldVote == -1)
                    c.setDislikes(c.getDislikes() - 1);
                if (newVoteState == 1)
                    c.setLikes(c.getLikes() + 1);
                if (newVoteState == -1)
                    c.setDislikes(c.getDislikes() + 1);
                c.setUserVote(newVoteState);

                javafx.application.Platform.runLater(() -> {
                    if (actions.getChildren().size() >= 2) {
                        javafx.scene.control.Button bLike = (javafx.scene.control.Button) actions.getChildren().get(0);
                        javafx.scene.control.Button bDislike = (javafx.scene.control.Button) actions.getChildren()
                                .get(1);

                        bLike.setText("👍 " + c.getLikes());
                        styleLikeButton(bLike, c.getUserVote() == 1);

                        bDislike.setText("👎 " + c.getDislikes());
                        styleDislikeButton(bDislike, c.getUserVote() == -1);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleMarkSolution(CommunityComment c) {
        boolean confirm = WindowUtils.showConfirmation("Définir comme solution ?",
                "Cela marquera votre question comme résolue. Cette action est irréversible pour le moment.");
        if (!confirm)
            return;

        new Thread(() -> {
            try {
                postService.markPostAsResolved(post.getId(), c.getId());
                commentService.markCommentAsSolution(c.getId());
                post.setResolved(true);
                post.setSolutionCommentId(c.getId());
                javafx.application.Platform.runLater(() -> {
                    renderPost();
                    setPost(post);
                });
            } catch (Exception e) {
                javafx.application.Platform
                        .runLater(() -> WindowUtils.showAlert("Erreur", "Impossible de valider la solution."));
            }
        }).start();
    }

    @FXML
    void sendComment(ActionEvent event) {
        String content = txtComment.getText().trim();
        if (content.isEmpty())
            return;

        try {
            int userId = UserSession.getInstance().getUser().getId();
            // Check if reply
            Integer parentId = (replyingToComment != null) ? replyingToComment.getId() : null;

            CommunityComment c = new CommunityComment(post.getId(), userId, content, parentId);
            commentService.ajouter(c);

            // Fetch the created comment ID to ensure it has one (optional but good
            // practice)
            // For now, we manually set user name for display immediately
            c.setUserName(UserSession.getInstance().getUser().getPrenom() + " "
                    + UserSession.getInstance().getUser().getNom());
            c.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

            txtComment.clear();
            txtComment.setPromptText("Écrire un commentaire...");
            replyingToComment = null; // Reset reply state

            // Append new comment to bottom of container instead of reloading all
            // Calculate depth based on parent if replying

            // REFRESH ENTIRE COMMENT TREE TO MAINTAIN HIERARCHY
            // Since threading requires proper depth and ordering, it's safer to just reload
            // from DB
            loadComments(true);

            // Scroll to bottom to show new comment
            // Platform.runLater(() -> scrollPane.setVvalue(1.0)); // If we had reference to
            // scrollpane
        } catch (Exception e) {
            e.printStackTrace(); // PRINT STACKTRACE
            WindowUtils.showAlert("Erreur", "Impossible d'envoyer le commentaire.\n" + e.getMessage());
        }
    }

    // ── Voice Input ──

    @FXML
    void dictateComment(ActionEvent event) {
        if (sttService.isRecording()) {
            btnMicComment.setText("⏳");
            btnMicComment.setDisable(true);
            sttService.stopAndTranscribe(
                    text -> {
                        String existing = txtComment.getText();
                        txtComment.setText(existing.isEmpty() ? text : existing + " " + text);
                        btnMicComment.setText("🎙️");
                        btnMicComment.setDisable(false);
                    },
                    error -> {
                        btnMicComment.setText("🎙️");
                        btnMicComment.setDisable(false);
                    });
        } else {
            sttService.startRecording();
            btnMicComment.setText("⏹️");
            btnMicComment.setStyle(
                    "-fx-background-color: #e74c3c; -fx-background-radius: 20; -fx-cursor: hand; -fx-min-width: 45; -fx-min-height: 40;");
        }
    }
}