package tn.neuron.ardhi.controllers.UserAndDiag;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;
import tn.neuron.ardhi.utils.UserAndDiag.WindowUtils;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML
    private Label lblBienvenue;

    @FXML
    void goToDiagnosticStats(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/DiagnosticStatistiques.fxml", "Statistiques Diagnostics");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser != null) {
            lblBienvenue.setText("Bienvenue, " + currentUser.getPrenom() + " " + currentUser.getNom());
        }
    }

    @FXML
    void goToAdminUsers(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminUsers.fxml", "Gestion des Utilisateurs");
    }

    @FXML
    void goToAdminAbonnements(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminAbonnements.fxml", "Gestion des Abonnements");
    }

    @FXML
    void goToProfil(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/Profil.fxml", "Mon Profil");
    }

    @FXML
    void goToAIDiagPanel(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDiagnostics.fxml", "Gestion Diagnostics IA");
    }

    @FXML
    void goToMap(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/DiseaseMap.fxml", "Carte des Maladies");
    }

    @FXML
    void goToAdminMateriel(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/MaterielEtMaintenance/AdminMateriel.fxml", "Gestion du Matériel");
    }

    @FXML
    void goToAdminTraitements(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminTraitements.fxml", "Gestion des Traitements");
    }

    @FXML
    void goToAdminOffres(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminOffres.fxml", "Gestion des Offres");
    }

    @FXML
    void goToAdminBadges(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminBadges.fxml", "Gestion des Badges");
    }

    @FXML
    void goToAdminUserBadges(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminUserBadges.fxml", "Affectation des Badges");
    }

    @FXML
    void goToSettings(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminSettings.fxml", "Paramètres du système");
    }

    @FXML
    void goToAdminCommunityPosts(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminCommunityPosts.fxml", "Gestion Posts Communauté");
    }

    @FXML
    void goToAdminCommunityComments(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminCommunityComments.fxml", "Gestion Commentaires");
    }

    @FXML
    void goToAdminCommunityLikes(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminCommunityLikes.fxml", "Gestion Votes Communauté");
    }

    @FXML
    void goToAdminTreatmentPlans(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminTreatmentPlans.fxml", "Gestion Plans de Traitement");
    }

    @FXML
    void goToAdminTreatmentTasks(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminTreatmentTasks.fxml", "Gestion Tâches Traitement");
    }

    @FXML
    void goToAdminReviews(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminReviews.fxml", "Gestion Revues Expert");
    }

    @FXML
    void goToAdminPreventionPlans(MouseEvent event) {
        // Navigate to Prevention Plans management
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminPreventionPlans.fxml", "Gestion Plans de Prévention");
    }

    @FXML
    void goToAdminPreventionTasks(MouseEvent event) {
        // Navigate to Prevention Tasks management
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminPreventionTasks.fxml", "Gestion Tâches Prévention");
    }

    @FXML
    void goToAdminVulnerabilities(MouseEvent event) {
        // Navigate to Vulnerabilities management
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminVulnerabilities.fxml", "Gestion Vulnérabilités");
    }

    @FXML
    void goToAdminFarmHealthScans(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminFarmHealthScans.fxml", "Gestion Health Scans");
    }

    @FXML
    void goToAdminFarmHealthReports(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminFarmHealthReports.fxml", "Gestion Health Reports");
    }

     @FXML
    void goToAdminProduits(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/GestionProduits.fxml", "Gestion des Produits");
    }

    @FXML
    void goToAdminReclamations(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/AdminReclamations.fxml", "Gestion des Réclamations");
    }

    @FXML
    void goToAdminCommandes(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/AdminCommandes.fxml", "Gestion des Commandes");
    }

    @FXML
    void goToAdminCoupons(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/marketplace/AdminCoupons.fxml", "Gestion des Coupons");
    }

    @FXML
    void goToAdminEvenements(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/Evenement/Navigationevenements.fxml", "Gestion des Événements");
    }

    @FXML
    void goToAdminParcelles(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AdminParcelles.fxml", "Gestion des Parcelles");
    }

    @FXML
    void goToAdminCultures(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/Parcelle_Cultures/AdminCultures.fxml", "Gestion des Cultures");
    }

    @FXML
    void goToAdminAgriculteurs(MouseEvent event) {
        WindowUtils.loadScene(event, "/fxml/gestionemploye/admin_agriculteurs_list.fxml", " Gestion des employes");
    }

    @FXML
    void goToAdminHome(ActionEvent event) {
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/AdminDashboard.fxml", "Ardhi - Administration");
    }

    @FXML
    void logout(ActionEvent event) {
        UserSession.getInstance().cleanUserSession();
        WindowUtils.loadScene(event, "/fxml/UserAndDiag/Login.fxml", "Connexion");
    }

}