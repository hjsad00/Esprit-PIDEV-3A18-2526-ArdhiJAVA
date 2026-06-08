package tn.neuron.ardhi.utils.UserAndDiag;

import javafx.stage.Stage;

/**
 * Utilitaire centralisé pour configurer la taille des fenêtres.
 * Utiliser cette classe pour garantir une taille homogène sur toutes les vues.
 * (Refresh)
 */
public final class WindowUtils {

    // Taille standard de l'application (à ajuster si besoin)
    // Largeur fixe confortable, hauteur un peu réduite pour éviter
    // que le bas de la fenêtre soit coupé sur les écrans 768px.
    public static final double APP_WIDTH = 1060;
    public static final double APP_HEIGHT = 650;

    private WindowUtils() {
        // Utilitaire : pas d'instanciation
    }

    // Force re-index

    /**
     * Applique la taille standard à la fenêtre passée.
     * - Définit une taille minimale commune
     * - N'écrase PAS une fenêtre déjà agrandie
     */
    public static void applyStandardSize(Stage stage) {
        if (stage == null) {
            return;
        }
        stage.setMinWidth(APP_WIDTH);

        // Ne forcer la taille qu'en cas de fenêtre plus petite que le minimum
        if (stage.getWidth() < APP_WIDTH) {
            stage.setWidth(APP_WIDTH);
        }
        if (stage.getHeight() < APP_HEIGHT) {
            stage.setHeight(APP_HEIGHT);
        }
    }

    /**
     * Configure la table pour désélectionner la ligne si on clique :
     * 1. Sur une zone vide de la table (pas sur une ligne remplie)
     * 2. Sur le conteneur parent (si fourni)
     */
    public static <T> void setupTableDeselection(javafx.scene.control.TableView<T> table, javafx.scene.Node container) {
        if (table == null)
            return;

        // 1. Clic dans la table mais pas sur une ligne
        table.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
            javafx.scene.Node clickedNode = (javafx.scene.Node) event.getTarget();
            boolean estUneLigneDonnee = false;
            while (clickedNode != null && clickedNode != table) {
                if (clickedNode instanceof javafx.scene.control.TableRow
                        && !((javafx.scene.control.TableRow<?>) clickedNode).isEmpty()) {
                    estUneLigneDonnee = true;
                    break;
                }
                clickedNode = clickedNode.getParent();
            }
            if (!estUneLigneDonnee) {
                table.getSelectionModel().clearSelection();
            }
        });

        // 2. Clic sur le conteneur (background)
        if (container != null) {
            container.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
                if (event.getTarget() == container) {
                    table.getSelectionModel().clearSelection();
                }
            });
        }
    }

    // Navigation History
    private static java.util.Stack<HistoryEntry> history = new java.util.Stack<>();
    private static String currentFxml = null;
    private static String currentTitle = null;

    private static class HistoryEntry {
        String fxml;
        String title;

        public HistoryEntry(String fxml, String title) {
            this.fxml = fxml;
            this.title = title;
        }
    }

    /**
     * Définit la vue initiale (pour MainFX) sans l'ajouter à l'historique.
     */
    public static void setInitialView(String fxmlPath, String title) {
        currentFxml = fxmlPath;
        currentTitle = title;
        history.clear();
    }

    /**
     * Retourne à la page précédente dans la pile d'historique.
     * Si l'historique est vide, redirige vers la page d'accueil par défaut.
     */
    public static void goBack(javafx.event.Event event) {
        if (history.isEmpty()) {
            // Fallback strategy depending on session
            UserSession session = UserSession.getInstance();
            if (session != null && session.getUser() != null) {
                // Logged in -> Go to Landing Page (Nexus)
                loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil", null, false);
            } else {
                // Guest -> Go to Landing Page
                loadScene(event, "/fxml/LandingPage.fxml", "Ardhi - Accueil", null, false);
            }
            return;
        }

        HistoryEntry previous = history.pop();
        // Load previous view without pushing current one to history
        loadScene(event, previous.fxml, previous.title, null, false);
    }

    /**
     * Charge une nouvelle scène FXML dans la fenêtre actuelle.
     * Applique automatiquement la taille standard et le centrage.
     */
    public static void loadScene(javafx.event.Event event, String fxmlPath, String title) {
        loadScene(event, fxmlPath, title, null, true);
    }

    /**
     * Charge une nouvelle scène FXML dans la fenêtre actuelle avec passage de
     * données.
     *
     * @param event          L'événement déclencheur (pour récupérer le Stage)
     * @param fxmlPath       Chemin vers le fichier FXML
     * @param title          Titre de la fenêtre (peut être null)
     * @param controllerInit Consumer pour initialiser le contrôleur (peut être
     *                       null)
     * @param <T>            Type du contrôleur
     */
    public static <T> void loadScene(javafx.event.Event event, String fxmlPath, String title,
            java.util.function.Consumer<T> controllerInit) {
        loadScene(event, fxmlPath, title, controllerInit, true);
    }

    private static <T> void loadScene(javafx.event.Event event, String fxmlPath, String title,
            java.util.function.Consumer<T> controllerInit, boolean pushToHistory) {
        try {
            // Push current view to history before navigating away
            if (pushToHistory && currentFxml != null) {
                history.push(new HistoryEntry(currentFxml, currentTitle));
            }

            if ("/fxml/UserAndDiag/ClientDashboard.fxml".equals(fxmlPath)) {
                UserSession session = UserSession.getInstance();
                if (session != null && session.getUser() != null
                        && session.getUser().getRole() == tn.neuron.ardhi.models.UserAndDiag.Role.AGRONOME) {
                    fxmlPath = "/fxml/UserAndDiag/ExpertDashboard.fxml";
                    if (title != null) {
                        title = "Ardhi - Espace Expert";
                    }
                }
            }

            // Update current view tracking
            currentFxml = fxmlPath;
            if (title != null) {
                currentTitle = title;
            }

            // ✅ FIX: Vérifier et charger l'URL correctement
            java.net.URL fxmlUrl = WindowUtils.class.getResource(fxmlPath);

            // Si pas trouvé avec WindowUtils.class, essayer avec le ClassLoader
            if (fxmlUrl == null) {
                String pathWithoutSlash = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
                fxmlUrl = Thread.currentThread().getContextClassLoader().getResource(pathWithoutSlash);
            }

            if (fxmlUrl == null) {
                System.err.println("❌ Fichier FXML introuvable : " + fxmlPath);
                showAlert(javafx.scene.control.Alert.AlertType.ERROR,
                        "Erreur de navigation",
                        "Impossible de trouver le fichier : " + fxmlPath);
                return;
            }

            System.out.println("✅ Chargement de : " + fxmlUrl);

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            try {
                loader.setResources(
                        tn.neuron.ardhi.utils.gestionemployeutils.LanguageManager.getInstance().getBundle());
            } catch (Exception e) {
                System.err.println("Warning: Could not set resources for " + fxmlPath);
            }
            javafx.scene.Parent root = loader.load();

            if (controllerInit != null) {
                T controller = loader.getController();
                controllerInit.accept(controller);
            }

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root, APP_WIDTH, APP_HEIGHT));
            if (title != null) {
                stage.setTitle(title);
            }
            stage.setResizable(true);
            applyStandardSize(stage);

            // Animating the transition
            applyFadeTransition(root);

            stage.show();

            // Force scroll to top
            scrollToTop(root);
        } catch (Exception e) {
            e.printStackTrace();
            Throwable cause = e;
            while (cause.getCause() != null
                    && (cause.getMessage() == null || cause instanceof java.lang.reflect.InvocationTargetException)) {
                cause = cause.getCause();
            }
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur de Navigation",
                    "Impossible de charger la vue : " + fxmlPath + "\n\nDetails: " + cause.getMessage());
        }
    }

    /**
     * Ouvre une nouvelle fenêtre (Popup).
     *
     * @param fxmlPath       Chemin vers le fichier FXML
     * @param title          Titre de la fenêtre
     * @param controllerInit Consumer pour initialiser le contrôleur (peut être
     *                       null)
     * @param <T>            Type du contrôleur
     */
    public static <T> void loadPopup(String fxmlPath, String title, java.util.function.Consumer<T> controllerInit) {
        try {
            // ✅ FIX: Même logique
            java.net.URL fxmlUrl = WindowUtils.class.getResource(fxmlPath);

            if (fxmlUrl == null) {
                String pathWithoutSlash = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
                fxmlUrl = Thread.currentThread().getContextClassLoader().getResource(pathWithoutSlash);
            }

            if (fxmlUrl == null) {
                System.err.println("❌ Fichier FXML introuvable : " + fxmlPath);
                showAlert(javafx.scene.control.Alert.AlertType.ERROR,
                        "Erreur",
                        "Impossible de trouver le fichier : " + fxmlPath);
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            try {
                loader.setResources(
                        tn.neuron.ardhi.utils.gestionemployeutils.LanguageManager.getInstance().getBundle());
            } catch (Exception e) {
                System.err.println("Warning: Could not set resources for popup " + fxmlPath);
            }
            javafx.scene.Parent root = loader.load();

            if (controllerInit != null) {
                T controller = loader.getController();
                controllerInit.accept(controller);
            }

            Stage stage = new Stage();
            stage.setScene(new javafx.scene.Scene(root));
            if (title != null) {
                stage.setTitle(title);
            }
            stage.show();

            // Force scroll to top after show
            scrollToTop(root);

        } catch (java.io.IOException e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la fenêtre : " + fxmlPath + "\n\nDétails: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Force le scroll vers le haut pour éviter que le focus sur un bouton en bas ne
     * fasse défiler la page.
     */
    private static void scrollToTop(javafx.scene.Parent root) {
        javafx.application.Platform.runLater(() -> {
            // 1. Give focus to root to avoid button focus
            root.requestFocus();

            // 2. Find ScrollPane and reset Vvalue
            javafx.scene.control.ScrollPane sp = findScrollPane(root);
            if (sp != null) {
                // Formatting hack: run later again to ensure layout is fully computed
                javafx.application.Platform.runLater(() -> sp.setVvalue(0.0));
            }
        });
    }

    private static javafx.scene.control.ScrollPane findScrollPane(javafx.scene.Parent parent) {
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof javafx.scene.control.ScrollPane) {
                return (javafx.scene.control.ScrollPane) node;
            }
            if (node instanceof javafx.scene.Parent) {
                javafx.scene.control.ScrollPane found = findScrollPane((javafx.scene.Parent) node);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Affiche une alerte standard.
     */
    public static void showAlert(javafx.scene.control.Alert.AlertType type, String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }

    /**
     * Affiche une alerte d'information par défaut.
     */
    public static void showAlert(String title, String content) {
        showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, title, content);
    }

    /**
     * Affiche une boîte de dialogue de confirmation standard.
     *
     * @return true si l'utilisateur a cliqué sur OK, false sinon.
     */
    public static boolean showConfirmation(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK;
    }

    public static void updateInfoLabel(javafx.scene.control.Label label, int count, String noun) {
        if (label != null) {
            label.setText(count + " " + noun + (count != 1 ? "s" : ""));
        }
    }

    /**
     * Ferme la fenêtre actuelle associée à l'événement.
     */
    public static void closeWindow(javafx.event.ActionEvent event) {
        javafx.scene.Node source = (javafx.scene.Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private static void applyFadeTransition(javafx.scene.Parent root) {
        // Start transparent
        root.setOpacity(0);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350),
                root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * Centralized prompt to choose between USB Webcam and IP Camera.
     * Handles connection and error display for IP cameras.
     */
    public static void showCameraSourcePrompt(
            tn.neuron.ardhi.services.UserAndDiag.CameraService service,
            java.lang.Runnable onSuccess,
            java.lang.Runnable onCancel) {

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Mode Caméra");
        alert.setHeaderText("Choisir la source vidéo");
        javafx.scene.control.ButtonType btnWebcam = new javafx.scene.control.ButtonType("Webcam USB");
        javafx.scene.control.ButtonType btnIp = new javafx.scene.control.ButtonType("Caméra IP / Phone");
        javafx.scene.control.ButtonType btnCancel = new javafx.scene.control.ButtonType("Annuler",
                javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnWebcam, btnIp, btnCancel);

        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnWebcam) {
                java.util.List<String> cams = service.getAvailableCameras();
                if (!cams.isEmpty()) {
                    service.startCamera(cams.get(0));
                    if (onSuccess != null)
                        onSuccess.run();
                } else {
                    showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur", "Aucune webcam USB détectée.");
                    if (onCancel != null)
                        onCancel.run();
                }
            } else if (result.get() == btnIp) {
                javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(
                        "http://localhost:8081/video");
                dialog.setTitle("Caméra IP");
                dialog.setHeaderText("Entrez l'URL de votre caméra IP (Android IP Webcam)");
                dialog.setContentText("USB: http://localhost:8081/video\nWiFi: http://192.168.1.XX:8080/video\nURL:");

                java.util.Optional<String> urlResult = dialog.showAndWait();
                if (urlResult.isPresent() && !urlResult.get().isEmpty()) {
                    String url = urlResult.get();
                    try {
                        boolean connected = service.connectIpCamera("IP Cam", url);
                        if (connected) {
                            if (onSuccess != null)
                                onSuccess.run();
                        } else {
                            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur Connexion",
                                    "Impossible de se connecter à : " + url + "\n\n" +
                                            "1. Vérifiez que le serveur IP Webcam est lancé.\n" +
                                            "2. Mode USB : vérifiez 'adb forward tcp:8081 tcp:8080'.\n" +
                                            "3. Mode WiFi : vérifiez que téléphone et PC sont sur le même réseau.");
                            if (onCancel != null)
                                onCancel.run();
                        }
                    } catch (Exception e) {
                        showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur",
                                "URL invalide ou erreur de connexion : " + e.getMessage());
                        if (onCancel != null)
                            onCancel.run();
                    }
                } else {
                    if (onCancel != null)
                        onCancel.run();
                }
            } else {
                if (onCancel != null)
                    onCancel.run();
            }
        } else {
            if (onCancel != null)
                onCancel.run();
        }
    }
}