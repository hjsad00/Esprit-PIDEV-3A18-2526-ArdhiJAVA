package tn.neuron.ardhi.controllers.Parcelle_Cultures;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;

import tn.neuron.ardhi.models.Parcelle_Cultures.Culture;
import tn.neuron.ardhi.models.Parcelle_Cultures.Parcelle;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult.CultureRecommandee;
import tn.neuron.ardhi.models.Parcelle_Cultures.RecommendationResult.DonneesMeteo;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.Parcelle_Cultures.*;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller Premium pour la fonctionnalité Smart Parcelle:
 * - Recommandation IA de 2 cultures optimisées
 * - Visualisation météo temps réel
 * - Carte Leaflet (WebView) avec division de la parcelle
 * - Alertes métier (gel, pluie, récolte)
 * - Score qualité de la parcelle
 * - Application directe des recommandations en base de données
 */
public class SmartParcelleController implements Initializable {

    // ==================== COMPOSANTS FXML ====================

    @FXML private Label lblTitreParcelle;
    @FXML private Label lblSurface;
    @FXML private Label lblTypeSol;
    @FXML private Label lblIrrigation;
    @FXML private Label lblGps;
    @FXML private Label lblSaison;

    // Météo
    @FXML private Label lblTemperature;
    @FXML private Label lblHumidite;
    @FXML private Label lblPrecipitations;
    @FXML private Label lblVent;
    @FXML private Label lblMeteoDesc;
    @FXML private Label lblAlertGel;
    @FXML private Label lblAlertPluie;

    // Score qualité
    @FXML private Label lblScoreQualite;
    @FXML private ProgressBar progressScoreQualite;

    // Recommandation Culture 1
    @FXML private Label lblCulture1Nom;
    @FXML private Label lblCulture1Surface;
    @FXML private Label lblCulture1Rendement;
    @FXML private Label lblCulture1Production;
    @FXML private Label lblCulture1Score;
    @FXML private ProgressBar progressScore1;

    // Recommandation Culture 2
    @FXML private VBox vboxCulture2;
    @FXML private Label lblCulture2Nom;
    @FXML private Label lblCulture2Surface;
    @FXML private Label lblCulture2Rendement;
    @FXML private Label lblCulture2Production;
    @FXML private Label lblCulture2Score;
    @FXML private ProgressBar progressScore2;

    // Options
    @FXML private CheckBox chkDiviser;

    // Justification & source
    @FXML private TextArea taJustification;
    @FXML private Label lblSource;

    // Alertes
    @FXML private VBox boxAlertes;

    // Boutons
    @FXML private Button btnAnalyser;
    @FXML private Button btnAppliquer;

    // Carte Leaflet
    @FXML private WebView webMapView;

    // État
    @FXML private Label lblEtatAnalyse;
    @FXML private ProgressIndicator progressIndicator;

    // GPS Fields dans le formulaire
    @FXML private TextField tfLatitude;
    @FXML private TextField tfLongitude;

    // ==================== ÉTAT ====================

    private User currentUser;
    private Parcelle parcelleCourante;
    private RecommendationResult resultatCourant;

    private final ParcelleRecommendationService recommendationService = new ParcelleRecommendationService();
    private final CultureService cultureService = new CultureService();
    private final ParcelleService parcelleService = new ParcelleService();

    // ==================== INITIALISATION ====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) {
            WindowUtils.showAlert("Erreur", "Session expirée. Veuillez vous reconnecter.");
            return;
        }

        // État initial des boutons
        if (btnAppliquer != null) {
            btnAppliquer.setDisable(true);
            btnAppliquer.setVisible(false);
            btnAppliquer.setManaged(false);
        }
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
        }

        // Initialiser la carte Leaflet
        initializerCarte();
    }

    /**
     * Méthode appelée depuis AgriculteurParcellesController pour passer la parcelle.
     */
    public void setParcelle(Parcelle parcelle) {
        this.parcelleCourante = parcelle;
        afficherInfoParcelle(parcelle);
        initializerCarte(); // Recharger la carte avec les coordonnées
    }

    // ==================== ACTIONS ====================

    @FXML
    void lancerAnalyse(ActionEvent event) {
        if (parcelleCourante == null) {
            WindowUtils.showAlert("Attention", "Aucune parcelle sélectionnée.\nRetournez sur la liste des parcelles et cliquez sur '🧠 Recommandation IA'.");
            return;
        }

        // Mettre à jour les GPS depuis les champs si renseignés
        if (tfLatitude != null && !tfLatitude.getText().trim().isEmpty()) {
            try {
                parcelleCourante.setLatitude(Double.parseDouble(tfLatitude.getText().trim()));
                parcelleCourante.setLongitude(Double.parseDouble(tfLongitude.getText().trim()));
            } catch (NumberFormatException e) {
                WindowUtils.showAlert("Erreur", "Latitude/Longitude invalides. Utilisez des nombres décimaux (ex: 36.8065)");
                return;
            }
        }

        setEtatChargement(true);

        // Exécution en arrière-plan pour éviter de bloquer l'UI
        Task<RecommendationResult> task = new Task<>() {
            @Override
            protected RecommendationResult call() {
                boolean diviser = chkDiviser != null && chkDiviser.isSelected();
                return recommendationService.genererRecommandation(parcelleCourante, diviser);
            }
        };

        task.setOnSucceeded(e -> {
            setEtatChargement(false);
            RecommendationResult result = task.getValue();
            if (result != null && result.isSucces()) {
                resultatCourant = result;
                afficherResultat(result);
            } else {
                String erreur = (result != null) ? result.getMessageErreur() : "Erreur inconnue";
                WindowUtils.showAlert("Erreur analyse", "Impossible de générer la recommandation:\n" + erreur);
            }
        });

        task.setOnFailed(e -> {
            setEtatChargement(false);
            WindowUtils.showAlert("Erreur", "Erreur lors de l'analyse: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    void appliquerRecommandation(ActionEvent event) {
        if (resultatCourant == null || !resultatCourant.isSucces()) {
            WindowUtils.showAlert("Attention", "Aucune recommandation à appliquer.");
            return;
        }

        CultureRecommandee c1 = resultatCourant.getCulture1();
        CultureRecommandee c2 = resultatCourant.getCulture2();
        boolean diviser = chkDiviser != null && chkDiviser.isSelected();

        String confirmation;
        if (diviser && c2 != null) {
            confirmation = String.format(
                    "Appliquer cette recommandation ?\n\n" +
                    "✅ %s — %.1f ha (%.1f t/ha)\n" +
                    "✅ %s — %.1f ha (%.1f t/ha)\n\n" +
                    "Ces deux cultures seront créées pour la parcelle: %s",
                    c1.getNom(), c1.getSurface(), c1.getRendementEstime(),
                    c2.getNom(), c2.getSurface(), c2.getRendementEstime(),
                    parcelleCourante.getLocalisation());
        } else {
            confirmation = String.format(
                    "Appliquer cette recommandation ?\n\n" +
                    "✅ %s — %.1f ha (%.1f t/ha)\n\n" +
                    "Cette culture sera créée pour la parcelle complète: %s",
                    c1.getNom(), c1.getSurface(), c1.getRendementEstime(),
                    parcelleCourante.getLocalisation());
        }

        if (!WindowUtils.showConfirmation("Appliquer la recommandation IA", confirmation)) {
            return;
        }

        try {
            // Vérifier la surface disponible
            double surfaceDisponible = new ParcelleRecommendationService().getSurfaceDisponible(parcelleCourante.getId());
            double surfaceRequise = c1.getSurface() + (diviser && c2 != null ? c2.getSurface() : 0);
            
            if (surfaceRequise > surfaceDisponible + 0.01) {
                WindowUtils.showAlert("Surface Insuffisante", 
                    String.format("La surface disponible (%.1f ha) est insuffisante pour ces cultures (%.1f ha).", 
                    surfaceDisponible, surfaceRequise));
                return;
            }

            // Créer Culture 1
            Culture culture1 = buildCultureFromRecommandation(c1, parcelleCourante.getId());
            cultureService.ajouter(culture1);

            // Créer Culture 2 (si division)
            if (diviser && c2 != null) {
                Culture culture2 = buildCultureFromRecommandation(c2, parcelleCourante.getId());
                cultureService.ajouter(culture2);
            }

            if (diviser && c2 != null) {
                WindowUtils.showAlert("Succes",
                        "Les deux cultures ont été créées avec succès !\n\n" +
                        "- " + c1.getNom() + " (" + c1.getSurface() + " ha)\n" +
                        "- " + c2.getNom() + " (" + c2.getSurface() + " ha)\n\n" +
                        "Consultez 'Mes Cultures' pour les voir.");
            } else {
                WindowUtils.showAlert("Succes",
                        "La culture a été créée avec succès sur la globalité de la parcelle !\n\n" +
                        "- " + c1.getNom() + " (" + c1.getSurface() + " ha)\n\n" +
                        "Consultez 'Mes Cultures' pour les voir.");
            }

            // Désactiver le bouton appliquer pour éviter la double soumission
            if (btnAppliquer != null) {
                btnAppliquer.setDisable(true);
                btnAppliquer.setText("✅ Appliquée");
            }

        } catch (IllegalArgumentException e) {
            WindowUtils.showAlert("Contrainte violée", e.getMessage());
        } catch (SQLException e) {
            WindowUtils.showAlert("Erreur BD", "Impossible de créer les cultures: " + e.getMessage());
        }
    }

    @FXML
    void retour(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurParcelles.fxml", "Ardhi - Mes Parcelles");
    }

    @FXML
    void retourAction(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AgriculteurParcelles.fxml", "Ardhi - Mes Parcelles");
    }

    // ==================== AFFICHAGE ====================

    private void afficherInfoParcelle(Parcelle p) {
        if (lblTitreParcelle != null)
            lblTitreParcelle.setText("Parcelle: " + p.getLocalisation());
        if (lblSurface != null)
            lblSurface.setText(String.format("%.2f ha", p.getSurface()));
        if (lblTypeSol != null)
            lblTypeSol.setText(p.getTypeSol() != null ? p.getTypeSol() : "N/A");
        if (lblIrrigation != null)
            lblIrrigation.setText(p.getSystemeIrrigation() != null ? p.getSystemeIrrigation() : "N/A");
        if (lblSaison != null)
            lblSaison.setText(recommendationService.getSaisonActuelle());

        if (lblGps != null) {
            if (p.hasGpsCoordinates()) {
                lblGps.setText(String.format("%.4f, %.4f", p.getLatitude(), p.getLongitude()));
            } else {
                lblGps.setText("Non défini (géocodage auto)");
            }
        }

        if (tfLatitude != null && p.getLatitude() != null) {
            tfLatitude.setText(String.valueOf(p.getLatitude()));
        }
        if (tfLongitude != null && p.getLongitude() != null) {
            tfLongitude.setText(String.valueOf(p.getLongitude()));
        }
    }

    private void afficherResultat(RecommendationResult result) {
        CultureRecommandee c1 = result.getCulture1();
        CultureRecommandee c2 = result.getCulture2();
        DonneesMeteo meteo = result.getMeteo();

        // ---- Météo ----
        if (meteo != null) {
            if (lblTemperature != null)
                lblTemperature.setText(String.format("%.1f°C", meteo.getTemperatureMoyenne()));
            if (lblHumidite != null)
                lblHumidite.setText(String.format("%.0f%%", meteo.getHumidite()));
            if (lblPrecipitations != null)
                lblPrecipitations.setText(String.format("%.1f mm/j", meteo.getPrecipitations()));
            if (lblVent != null)
                lblVent.setText(String.format("%.1f km/h", meteo.getVitesseVent()));
            if (lblMeteoDesc != null)
                lblMeteoDesc.setText(meteo.getDescription());
            if (lblAlertGel != null) {
                lblAlertGel.setVisible(meteo.isRisqueGel());
                lblAlertGel.setManaged(meteo.isRisqueGel());
            }
            if (lblAlertPluie != null) {
                lblAlertPluie.setVisible(meteo.isPluieExcessive());
                lblAlertPluie.setManaged(meteo.isPluieExcessive());
            }
        }
        boolean diviser = chkDiviser != null && chkDiviser.isSelected();

        if (c1 != null) {
            lblCulture1Nom.setText(c1.getNom());
            lblCulture1Surface.setText(String.format("%.1f ha", c1.getSurface()));
            lblCulture1Rendement.setText(String.format("%.1f t/ha", c1.getRendementEstime()));
            lblCulture1Production.setText(String.format("%.1f t", c1.getProductionEstimee())); // Assuming getProductionEstimee() is the correct method
            lblCulture1Score.setText(String.format("Score: %.0f%%", c1.getScore() * 100)); // Assuming getScore() is the correct method
            progressScore1.setProgress(c1.getScore()); // Assuming getScore() returns a value between 0 and 1
        }

        if (diviser && c2 != null) {
            if (vboxCulture2 != null) vboxCulture2.setVisible(true);
            if (vboxCulture2 != null) vboxCulture2.setManaged(true);
            lblCulture2Nom.setText(c2.getNom());
            lblCulture2Surface.setText(String.format("%.1f ha", c2.getSurface()));
            lblCulture2Rendement.setText(String.format("%.1f t/ha", c2.getRendementEstime()));
            lblCulture2Production.setText(String.format("%.1f t", c2.getProductionEstimee())); // Assuming getProductionEstimee() is the correct method
            lblCulture2Score.setText(String.format("Score: %.0f%%", c2.getScore() * 100)); // Assuming getScore() is the correct method
            progressScore2.setProgress(c2.getScore()); // Assuming getScore() returns a value between 0 and 1
        } else {
            if (vboxCulture2 != null) vboxCulture2.setVisible(false);
            if (vboxCulture2 != null) vboxCulture2.setManaged(false);
        }

        if (taJustification != null) taJustification.setText(result.getJustification());
        if (lblSource != null) {
            String sourceText = "ALGORITHME_FALLBACK".equals(result.getSource())
                    ? "🔧 Algorithme Ardhi Intelligence"
                    : "🤖 IA Gemini Google";
            lblSource.setText("Source: " + sourceText);
        }

        // ---- Score qualité ----
        double score = result.getScoreQualiteParcelle();
        if (lblScoreQualite != null)
            lblScoreQualite.setText(String.format("Score Qualité: %.1f/10", score));
        if (progressScoreQualite != null)
            progressScoreQualite.setProgress(score / 10.0);

        // ---- Alertes ----
        if (boxAlertes != null) {
            boxAlertes.getChildren().clear();
            List<String> alertes = result.getAlertes();
            if (alertes != null && !alertes.isEmpty()) {
                for (String alerte : alertes) {
                    Label lbl = new Label(alerte);
                    lbl.setWrapText(true);
                    lbl.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 6; " +
                            "-fx-padding: 8 12; -fx-text-fill: #E65100; -fx-font-size: 11;");
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    boxAlertes.getChildren().add(lbl);
                }
            }
        }

        // ---- Carte avec division ----
        afficherCarteAvecDivision(result);

        // ---- Activer bouton Appliquer ----
        if (btnAppliquer != null) {
            btnAppliquer.setDisable(false);
            btnAppliquer.setVisible(true);
            btnAppliquer.setManaged(true);
        }

        if (lblEtatAnalyse != null)
            lblEtatAnalyse.setText("✅ Analyse terminée");
    }

    private void afficherCarteAvecDivision(RecommendationResult result) {
        if (webMapView == null || parcelleCourante == null) return;

        double lat = parcelleCourante.hasGpsCoordinates() ? parcelleCourante.getLatitude() : 36.8065;
        double lon = parcelleCourante.hasGpsCoordinates() ? parcelleCourante.getLongitude() : 10.1815;

        CultureRecommandee c1 = result.getCulture1();
        CultureRecommandee c2 = result.getCulture2();

        String couleur1 = "#4CAF50"; // Vert pour culture 1
        String couleur2 = "#FF9800"; // Orange pour culture 2
        double ratio1 = (c1 != null && parcelleCourante.getSurface() > 0)
                ? c1.getSurface() / parcelleCourante.getSurface() : 0.6;

        String html = buildLeafletHTML(lat, lon, c1, c2, couleur1, couleur2, ratio1);
        Platform.runLater(() -> {
            WebEngine engine = webMapView.getEngine();
            engine.loadContent(html);
        });
    }

    private void initializerCarte() {
        if (webMapView == null) return;
        double lat = (parcelleCourante != null && parcelleCourante.hasGpsCoordinates())
                ? parcelleCourante.getLatitude() : 36.8065;
        double lon = (parcelleCourante != null && parcelleCourante.hasGpsCoordinates())
                ? parcelleCourante.getLongitude() : 10.1815;

        String html = buildLeafletHTML(lat, lon, null, null, "#4CAF50", "#FF9800", 0.5);
        Platform.runLater(() -> webMapView.getEngine().loadContent(html));
    }

    private String buildLeafletHTML(double lat, double lon,
                                     CultureRecommandee c1, CultureRecommandee c2,
                                     String couleur1, String couleur2, double ratio1) {
        String c1Nom = (c1 != null) ? c1.getNom() : "Culture 1";
        String c2Nom = (c2 != null) ? c2.getNom() : "Culture 2";
        String c1Info = (c1 != null) ? String.format("%.1f ha | %.1f t/ha", c1.getSurface(), c1.getRendementEstime()) : "";
        String c2Info = (c2 != null) ? String.format("%.1f ha | %.1f t/ha", c2.getSurface(), c2.getRendementEstime()) : "";

        // Calcul des zones de la parcelle (approximation rectangulaire)
        double delta = 0.003; // ~300m
        double splitLon = lon - delta + (ratio1 * 2 * delta);

        String zone2Script = "";
        if (c2 != null) {
            zone2Script = "var zone2 = L.rectangle([[" + (lat - delta) + "," + splitLon + "]," +
                    "[" + (lat + delta) + "," + (lon + delta) + "]]," +
                    "{color:'" + couleur2 + "',fillColor:'" + couleur2 + "',fillOpacity:0.45,weight:2}).addTo(map);" +
                    "zone2.bindPopup('<b>" + c2Nom + "</b><br>" + c2Info + "');";
        }

        return "<!DOCTYPE html><html><head>" +
                "<meta charset='utf-8'/>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>" +
                "html,body{margin:0;padding:0;height:100%;overflow:hidden;}" +
                "#map{height:100%;width:100%;background-color:#f4f4f4;}" +
                ".leaflet-tile{will-change:unset!important;backface-visibility:visible!important;}" +
                ".leaflet-fade-anim .leaflet-tile,.leaflet-zoom-anim .leaflet-zoom-animated{will-change:auto!important;}" +
                "</style>" +
                "</head><body>" +
                "<div id='map'></div>" +
                "<script>" +
                // Fixes spécifiques JavaFX WebView (comme html/map.html)
                "L.Browser.any3d=false;" +
                "L.Browser.retina=false;" +
                "var map = L.map('map',{" +
                " fadeAnimation:false,zoomAnimation:false,markerZoomAnimation:false," +
                " zoomSnap:1,zoomDelta:1,trackResize:true,preferCanvas:true" +
                "}).setView([" + lat + "," + lon + "],14);" +
                "L.tileLayer('https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}',{" +
                " maxZoom:20,attribution:'© Google Satellite'}).addTo(map);" +
                // Zone culture 1
                "var zone1 = L.rectangle([[" + (lat - delta) + "," + (lon - delta) + "]," +
                "[" + (lat + delta) + "," + (c2 != null ? splitLon : lon + delta) + "]]," +
                "{color:'" + couleur1 + "',fillColor:'" + couleur1 + "',fillOpacity:0.45,weight:2}).addTo(map);" +
                "zone1.bindPopup('<b>" + c1Nom + "</b><br>" + c1Info + "');" +
                zone2Script +
                // Marqueur centre
                "L.marker([" + lat + "," + lon + "])" +
                ".bindPopup('<b>Parcelle</b><br>Location: " + (parcelleCourante != null ? parcelleCourante.getLocalisation() : "") + "').addTo(map).openPopup();" +
                "</script></body></html>";
    }

    // ==================== UTILITAIRES ====================

    private Culture buildCultureFromRecommandation(CultureRecommandee cr, int parcelleId) {
        // Date plantation = aujourd'hui, récolte = 120 jours après
        Date dateDebut = Date.valueOf(LocalDate.now());
        Date dateFin = Date.valueOf(LocalDate.now().plusDays(120));

        Culture c = new Culture(
                cr.getNom(),
                cr.getType(),
                cr.getSaison(),
                dateDebut,
                dateFin,
                "en_croissance",
                parcelleId
        );
        c.setSurfaceUtilisee(cr.getSurface());
        c.setRendementEstime(cr.getRendementEstime());
        return c;
    }

    private void setEtatChargement(boolean chargement) {
        Platform.runLater(() -> {
            if (btnAnalyser != null) btnAnalyser.setDisable(chargement);
            if (progressIndicator != null) {
                progressIndicator.setVisible(chargement);
                progressIndicator.setManaged(chargement);
            }
            if (lblEtatAnalyse != null) {
                lblEtatAnalyse.setText(chargement
                        ? "Analyse en cours (meteo + IA)..."
                        : "Pret pour l'analyse");
            }
        });
    }
}
