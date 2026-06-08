package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.neuron.ardhi.models.UserAndDiag.*;
import tn.neuron.ardhi.services.UserAndDiag.*;

import java.util.Comparator;
import java.util.List;

/**
 * Controller for prevention plan details.
 * Provides: HBox task-card design, chatbot, expert review (no AI).
 */
public class PreventionPlanDetailsController {

    // ─── FXML Bindings ───
    @FXML
    private Label planTitle;
    @FXML
    private Label startDateLabel;
    @FXML
    private VBox tasksContainer;
    @FXML
    private ScrollPane scrollPane;

    // ─── Services ───
    private PreventionPlanService planService;
    private PreventionTaskService taskService;
    private ReviewService reviewService;

    // ─── State ───
    private PreventionPlan plan;
    private int scanId;

    public PreventionPlanDetailsController() {
        try {
            planService = new PreventionPlanService();
            taskService = new PreventionTaskService();
            reviewService = new ReviewService();
        } catch (Exception e) {
            System.err.println("PreventionPlanDetailsController: service init error: " + e.getMessage());
        }
    }

    // ─── Init ───

    /**
     * Called after FXMLLoader.load() to populate the plan details.
     */
    public void initData(PreventionPlan plan, int scanId) {
        this.plan = plan;
        this.scanId = scanId;

        // Deep fetch to get tasks
        PreventionPlan full = planService.getById(plan.getId());
        if (full != null)
            this.plan = full;

        planTitle.setText("🛡 " + this.plan.getTitle());
        if (this.plan.getStartDate() != null) {
            startDateLabel.setText("Débuté le: " + this.plan.getStartDate().toString());
        } else if (this.plan.getCreatedAt() != null) {
            startDateLabel.setText("Débuté le: " + this.plan.getCreatedAt().toString().substring(0, 10));
        }

        renderTasks();
        checkAndShowExpertFeedback();
    }

    // ═══════════════════════════════════════════════════════════════
    // RENDER TASKS — identical HBox row layout as Treatment
    // ═══════════════════════════════════════════════════════════════

    private void renderTasks() {
        tasksContainer.getChildren().clear();
        List<PreventionTask> tasks = plan.getTasks();
        if (tasks == null)
            return;
        tasks.sort(Comparator.comparingInt(PreventionTask::getDayOffset));

        for (PreventionTask task : tasks) {
            // ── Main Row ──
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: rgba(255, 255, 255, 0.35); " +
                    "-fx-padding: 10 15; " +
                    "-fx-background-radius: 12; " +
                    "-fx-border-color: rgba(255, 255, 255, 0.25); " +
                    "-fx-border-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");

            // ── LEFT: Day & Type ──
            VBox leftBox = new VBox(4);
            leftBox.setAlignment(Pos.CENTER);
            leftBox.setMinWidth(70);
            leftBox.setMaxWidth(70);

            Label dayLabel = new Label("J-" + task.getDayOffset());
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px;");

            Label typeTag = new Label();
            String typeStyle = "-fx-padding: 2 8; -fx-background-radius: 6; -fx-font-size: 9px; -fx-font-weight: bold; ";
            if (task.getDayOffset() <= 2) {
                typeTag.setText("URGENT");
                typeTag.setStyle(typeStyle + "-fx-background-color: rgba(231,76,60,0.3); -fx-text-fill: #e74c3c;");
            } else if (task.getDayOffset() <= 7) {
                typeTag.setText("PRÉVENTION");
                typeTag.setStyle(typeStyle + "-fx-background-color: rgba(52,152,219,0.3); -fx-text-fill: #3498db;");
            } else {
                typeTag.setText("SUIVI");
                typeTag.setStyle(typeStyle + "-fx-background-color: rgba(46,204,113,0.3); -fx-text-fill: #2ecc71;");
            }
            leftBox.getChildren().addAll(dayLabel, typeTag);

            // ── CENTER: Description ──
            Label descLabel = new Label(task.getTaskDescription());
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(descLabel, javafx.scene.layout.Priority.ALWAYS);
            if (task.getStatus() == TaskStatus.COMPLETED) {
                descLabel.setStyle(
                        "-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 13px; -fx-strikethrough: true;");
            } else {
                descLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
            }

            // ── RIGHT: Chat + Status + Checkbox ──
            HBox rightBox = new HBox(10);
            rightBox.setAlignment(Pos.CENTER_RIGHT);
            rightBox.setMinWidth(180);

            // Chat button
            Button chatBtn = new Button("💬");
            chatBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); " +
                    "-fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;");
            chatBtn.setOnAction(e -> openChatPopup(task));

            // Status tag
            Label statusTag = new Label();
            String statusStyle = "-fx-padding: 5 10; -fx-background-radius: 8; -fx-font-size: 10px; -fx-font-weight: bold; ";
            if (task.getStatus() == TaskStatus.COMPLETED) {
                statusTag.setText("ACCOMPLI");
                statusTag.setStyle(statusStyle
                        + "-fx-background-color: rgba(46, 204, 113, 0.2); -fx-text-fill: #2ecc71; -fx-border-color: #2ecc71; -fx-border-radius: 8;");
            } else if (task.getStatus() == TaskStatus.MISSED) {
                statusTag.setText("MANQUÉ");
                statusTag.setStyle(statusStyle
                        + "-fx-background-color: rgba(231, 76, 60, 0.2); -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 8;");
            } else {
                statusTag.setText("EN ATTENTE");
                statusTag.setStyle(statusStyle
                        + "-fx-background-color: rgba(241, 196, 15, 0.2); -fx-text-fill: #f1c40f; -fx-border-color: #f1c40f; -fx-border-radius: 8;");
            }

            // Checkbox
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(task.getStatus() == TaskStatus.COMPLETED);
            checkBox.setDisable(task.getStatus() == TaskStatus.COMPLETED);
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal)
                    completeTask(task, checkBox);
            });

            rightBox.getChildren().addAll(chatBtn, statusTag, checkBox);
            row.getChildren().addAll(leftBox, descLabel, rightBox);
            tasksContainer.getChildren().add(row);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPLETE TASK — sequential enforcement + in-place UI update
    // ═══════════════════════════════════════════════════════════════

    private void completeTask(PreventionTask task, CheckBox box) {
        // Sequential enforcement
        List<PreventionTask> sorted = new java.util.ArrayList<>(plan.getTasks());
        sorted.sort(Comparator.comparingInt(PreventionTask::getDayOffset));
        for (PreventionTask prev : sorted) {
            if (prev.getDayOffset() < task.getDayOffset() && prev.getStatus() != TaskStatus.COMPLETED) {
                box.setSelected(false);
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.WARNING,
                        "Ordre séquentiel",
                        "Vous devez d'abord terminer la tâche du Jour " + prev.getDayOffset()
                                + " avant de passer à celle-ci.");
                return;
            }
        }

        // Update DB
        task.setStatus(TaskStatus.COMPLETED);
        taskService.updateTaskStatus(task.getId(), TaskStatus.COMPLETED);

        // In-place UI update
        box.setDisable(true);
        HBox row = (HBox) box.getParent().getParent();
        if (row.getChildren().size() > 1 && row.getChildren().get(1) instanceof Label) {
            Label descLabel = (Label) row.getChildren().get(1);
            descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 13px; -fx-strikethrough: true;");
        }
        HBox rightBox = (HBox) box.getParent();
        for (javafx.scene.Node child : rightBox.getChildren()) {
            if (child instanceof Label) {
                Label statusTag = (Label) child;
                statusTag.setText("ACCOMPLI");
                statusTag.setStyle(
                        "-fx-padding: 5 10; -fx-background-radius: 8; -fx-font-size: 10px; -fx-font-weight: bold; " +
                                "-fx-background-color: rgba(46, 204, 113, 0.2); -fx-text-fill: #2ecc71; -fx-border-color: #2ecc71; -fx-border-radius: 8;");
                break;
            }
        }

        checkAndShowExpertFeedback();

        // Check if all tasks complete
        boolean allDone = plan.getTasks().stream().allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
        if (allDone) {
            planService.completePlan(plan.getId());
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION,
                    "Plan Terminé", "Toutes les tâches de prévention sont accomplies. Plan terminé !");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CHATBOT
    // ═══════════════════════════════════════════════════════════════

    private void openChatPopup(PreventionTask task) {
        String context = "Tu es un expert agronome en prévention agricole. " +
                "Contexte : Plan de prévention \"" + plan.getTitle() + "\", Tâche " + task.getTaskDescription()
                + ", Jour " + task.getDayOffset() + ". " +
                "Problème ciblé : " + plan.getProblemSummary() + ". " +
                "Réponds UNIQUEMENT sur cette tâche préventive. " +
                "Tes réponses doivent être COURTES (max 3 phrases) et PRATIQUES. Pas de politesses inutiles.";

        tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.<ChatbotTrTasksController>loadPopup(
                "/fxml/UserAndDiag/ChatbotTrTasks.fxml",
                "Assistant Prévention: " + task.getTaskDescription(),
                (controller) -> {
                    controller.setContext(context);
                });
    }

    // ═══════════════════════════════════════════════════════════════
    // EXPERT REVIEW (no photo required for prevention)
    // ═══════════════════════════════════════════════════════════════

    @FXML
    void requestExpertPlanReview() {
        if (plan == null)
            return;

        if (reviewService.hasPendingPreventionReview(plan.getId())) {
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert("Déjà soumis",
                    "Une demande d'avis expert sur le suivi est déjà en cours.");
            return;
        }

        // Build a summary of pending tasks for the agronome
        StringBuilder pendingSummary = new StringBuilder();
        if (plan.getTasks() != null) {
            for (PreventionTask t : plan.getTasks()) {
                if (t.getStatus() != TaskStatus.COMPLETED) {
                    pendingSummary.append("J-").append(t.getDayOffset()).append(": ").append(t.getTaskDescription())
                            .append("\n");
                }
            }
        }

        // Create review record directly (no photo needed for prevention)
        Review review = new Review(0, ReviewType.PREVENTION);
        review.setPreventionPlanId(plan.getId());
        review.setAiAnalysis("Tâches en attente:\n" + pendingSummary.toString());
        reviewService.createReview(review);

        tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION,
                "Demande envoyée",
                "Votre demande a été envoyée à l'agronome.\n\n" +
                        "L'expert examinera les tâches restantes et pourra les modifier, supprimer ou en ajouter.");
    }

    // ═══════════════════════════════════════════════════════════════
    // EXPERT FEEDBACK BANNER
    // ═══════════════════════════════════════════════════════════════

    private void checkAndShowExpertFeedback() {
        if (plan == null)
            return;
        Review review = reviewService.getCompletedReviewForPreventionPlan(plan.getId());
        if (review == null)
            return;
        if (review.getFarmerResponse() != null)
            return;

        VBox banner = new VBox(10);
        banner.setStyle("-fx-background-color: rgba(41, 128, 185, 0.2); " +
                "-fx-border-color: #2980b9; -fx-border-radius: 12; " +
                "-fx-background-radius: 12; -fx-padding: 15 20;");

        Label title = new Label("🧑‍🌾 Avis de l'Agronome Expert");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

        ExpertVerdict verdict = review.getExpertVerdict();
        Label verdictLabel = new Label();
        String verdictStyle = "-fx-padding: 4 14; -fx-background-radius: 10; -fx-font-size: 12px; -fx-font-weight: bold; ";
        if (verdict == ExpertVerdict.HEALED) {
            verdictLabel.setText("✅ RISQUE RÉSOLU");
            verdictLabel.setStyle(verdictStyle +
                    "-fx-background-color: rgba(46,204,113,0.3); -fx-text-fill: #2ecc71; -fx-border-color: #2ecc71; -fx-border-radius: 10;");
        } else if (verdict == ExpertVerdict.WORSENED) {
            verdictLabel.setText("⚠️ RISQUE AGGRAVÉ");
            verdictLabel.setStyle(verdictStyle +
                    "-fx-background-color: rgba(231,76,60,0.3); -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 10;");
        } else {
            verdictLabel.setText("🔄 CONTINUER LA PRÉVENTION");
            verdictLabel.setStyle(verdictStyle +
                    "-fx-background-color: rgba(241,196,15,0.3); -fx-text-fill: #f1c40f; -fx-border-color: #f1c40f; -fx-border-radius: 10;");
        }

        String notes = review.getExpertNotes();
        Label notesLabel = new Label(notes != null && !notes.isEmpty() ? notes : "Aucune note supplémentaire.");
        notesLabel.setWrapText(true);
        notesLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 13px;");

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (verdict == ExpertVerdict.HEALED) {
            Button acceptBtn = new Button("✅ Terminer le plan");
            acceptBtn.setStyle(
                    "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16;");
            acceptBtn.setOnAction(e -> {
                reviewService.updateFarmerResponse(review.getId(), FarmerResponse.ACCEPTED, null);
                planService.completePlan(plan.getId());
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION,
                        "Plan terminé", "Félicitations ! Le risque est résolu selon l'agronome.");
                goBack();
            });
            Button ignoreBtn = new Button("Continuer quand même");
            ignoreBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16;");
            ignoreBtn.setOnAction(e -> {
                reviewService.updateFarmerResponse(review.getId(), FarmerResponse.REJECTED, null);
                tasksContainer.getChildren().remove(banner);
            });
            actions.getChildren().addAll(acceptBtn, ignoreBtn);
        } else if (verdict == ExpertVerdict.WORSENED) {
            Label warningLabel = new Label("L'expert recommande de revoir votre approche.");
            warningLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 12px;");
            Button dismissBtn = new Button("Compris");
            dismissBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16;");
            dismissBtn.setOnAction(e -> {
                reviewService.updateFarmerResponse(review.getId(), FarmerResponse.ACKNOWLEDGED, null);
                tasksContainer.getChildren().remove(banner);
            });
            actions.getChildren().addAll(warningLabel, dismissBtn);
        } else {
            Button dismissBtn = new Button("OK, compris");
            dismissBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 16;");
            dismissBtn.setOnAction(e -> {
                reviewService.updateFarmerResponse(review.getId(), FarmerResponse.ACKNOWLEDGED, null);
                tasksContainer.getChildren().remove(banner);
            });
            actions.getChildren().add(dismissBtn);
        }

        banner.getChildren().addAll(title, verdictLabel, notesLabel, actions);
        tasksContainer.getChildren().add(0, banner);
    }

    // ═══════════════════════════════════════════════════════════════
    // ABANDON + NAVIGATION
    // ═══════════════════════════════════════════════════════════════

    @FXML
    void handleAbandon() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Abandonner le plan");
        confirm.setHeaderText("Voulez-vous vraiment abandonner ce plan de prévention ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                planService.abandonPlan(plan.getId());
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION,
                        "Plan Abandonné", "Le plan de prévention a été abandonné.");
                goBack();
            }
        });
    }

    @FXML
    void handleWhatsAppReminder(javafx.event.ActionEvent event) {
        if (plan == null || plan.getTasks() == null || plan.getTasks().isEmpty()) {
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Aucune tâche préventive à rappeler.");
            return;
        }

        tn.neuron.ardhi.models.UserAndDiag.User currentUser = tn.neuron.ardhi.utils.UserAndDiag.UserSession
                .getInstance().getUser();
        if (currentUser == null) {
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.WARNING, "Non connecté",
                    "Veuillez vous connecter pour utiliser cette fonctionnalité.");
            return;
        }

        String phone = currentUser.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.WARNING, "Numéro manquant",
                    "Veuillez ajouter un numéro de téléphone à votre profil pour recevoir des rappels WhatsApp.");
            return;
        }

        Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("Envoi en cours");
        progress.setHeaderText("Envoi du rappel WhatsApp...");
        progress.setContentText("Veuillez patienter pendant l'envoi du message via Twilio.");
        progress.show();

        javafx.concurrent.Task<Boolean> task = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                tn.neuron.ardhi.services.UserAndDiag.PreventionReminderService reminderService = new tn.neuron.ardhi.services.UserAndDiag.PreventionReminderService();
                return reminderService.sendReminder(plan, plan.getTasks(), phone);
            }
        };

        task.setOnSucceeded(e -> {
            progress.close();
            if (task.getValue()) {
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Rappel WhatsApp envoyé avec succès au numéro " + phone);
            } else {
                tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                        "L'envoi a échoué. Assurez-vous d'avoir rejoint la Sandbox Twilio WhatsApp (voir console pour détails).");
            }
        });

        task.setOnFailed(e -> {
            progress.close();
            tn.neuron.ardhi.utils.UserAndDiag.WindowUtils.showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Une erreur s'est produite lors de l'envoi : " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    void goBack() {
        try {
            if (scanId == -1) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserAndDiag/TreatmentPlanList.fxml"));
                Parent root = loader.load();
                planTitle.getScene().setRoot(root);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserAndDiag/ReportVulnerabilities.fxml"));
            Parent root = loader.load();
            ReportVulnerabilitiesController controller = loader.getController();
            controller.initData(scanId);
            planTitle.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
