package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import tn.neuron.ardhi.models.UserAndDiag.CommunityPost;
import tn.neuron.ardhi.services.UserAndDiag.CommunityPostService;
import tn.neuron.ardhi.services.UserAndDiag.SpeechToTextService;
import tn.neuron.ardhi.utils.UserAndDiag.ImgBBService;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;
import javafx.concurrent.Task;

import java.io.File;

public class CreatePostController {

    @FXML
    private TextField txtTitle;
    @FXML
    private TextArea txtDescription;
    @FXML
    private ImageView imgPreview;
    @FXML
    private javafx.scene.control.Label lblImageStatus;
    @FXML
    private Button btnMicTitle;
    @FXML
    private Button btnMicDesc;

    private File imageFile;
    private final CommunityPostService communityService = new CommunityPostService();
    private final SpeechToTextService sttService = new SpeechToTextService();

    public void initData(String suggestedTitle, String suggestedDesc, File image) {
        if (suggestedTitle != null)
            txtTitle.setText(suggestedTitle);
        if (suggestedDesc != null)
            txtDescription.setText(suggestedDesc);

        if (image != null) {
            this.imageFile = image;
            imgPreview.setImage(new Image(image.toURI().toString()));
        }
    }

    @FXML
    void chooseImage(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File selected = fileChooser.showOpenDialog(txtTitle.getScene().getWindow());
        if (selected != null) {
            this.imageFile = selected;
            imgPreview.setImage(new Image(selected.toURI().toString()));
            lblImageStatus.setText(selected.getName());
        }
    }

    @FXML
    void publish(ActionEvent event) {
        if (txtTitle.getText().isEmpty() || txtDescription.getText().isEmpty()) {
            WindowUtils.showAlert("Attention", "Veuillez remplir le titre et la description.");
            return;
        }

        // Disable UI to prevent double click
        // Ideally show a progress indicator

        Task<Boolean> publishTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                int userId = UserSession.getInstance().getUser().getId();
                String imgUrl = null;

                // 1. Upload Image to ImgBB
                if (imageFile != null) {
                    try {
                        imgUrl = ImgBBService.uploadImage(imageFile);
                        if (imgUrl == null) {
                            LogUtils.warn(getClass(), "ImgBB upload returned null, using local path.");
                            imgUrl = imageFile.getAbsolutePath();
                        }
                    } catch (Exception e) {
                        LogUtils.error(getClass(), "ImgBB Upload Error", e);
                        imgUrl = imageFile.getAbsolutePath();
                    }
                }

                // 2. Create Post
                CommunityPost post = new CommunityPost(userId, txtTitle.getText(), txtDescription.getText(), imgUrl);
                communityService.ajouter(post);
                return true;
            }
        };

        publishTask.setOnSucceeded(e -> {
            WindowUtils.showAlert("Succès", "Question publiée !");
            WindowUtils.goBack(event);
        });

        publishTask.setOnFailed(e -> {
            Throwable ex = publishTask.getException();
            ex.printStackTrace();
            WindowUtils.showAlert("Erreur", "Impossible de publier : " + ex.getMessage());
        });

        new Thread(publishTask).start();
    }

    @FXML
    void cancel(ActionEvent event) {
        WindowUtils.goBack(event);
    }

    // ── Voice Input Handlers ──

    @FXML
    void dictateTitle(ActionEvent event) {
        toggleDictation(btnMicTitle, text -> txtTitle.setText(text));
    }

    @FXML
    void dictateDescription(ActionEvent event) {
        toggleDictation(btnMicDesc, text -> {
            String existing = txtDescription.getText();
            txtDescription.setText(existing.isEmpty() ? text : existing + " " + text);
        });
    }

    private void toggleDictation(Button btn, java.util.function.Consumer<String> onResult) {
        if (sttService.isRecording()) {
            btn.setText("⏳");
            btn.setDisable(true);
            sttService.stopAndTranscribe(
                    text -> {
                        onResult.accept(text);
                        btn.setText("🎙️");
                        btn.setDisable(false);
                    },
                    error -> {
                        btn.setText("🎙️");
                        btn.setDisable(false);
                    });
        } else {
            sttService.startRecording();
            btn.setText("⏹️");
            btn.setStyle(
                    "-fx-background-color: #e74c3c; -fx-background-radius: 20; -fx-cursor: hand; -fx-min-width: 40; -fx-min-height: 40;");
        }
    }
}