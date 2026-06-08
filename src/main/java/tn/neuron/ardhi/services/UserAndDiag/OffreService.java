package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.interfaces.IService;

import tn.neuron.ardhi.models.UserAndDiag.Offre;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour la gestion des offres d'abonnement.
 */
public class OffreService implements IService<Offre> {

    private Connection cnx;

    // Auto-create table if not exists

    public OffreService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    // 1. AJOUTER une offre
    public void ajouter(Offre o) throws SQLException {
        String sql = "INSERT INTO offre (nom, description, prix_mensuel, avantages, couleur_primaire, couleur_secondaire, est_active, est_recommandee, diagnostics_par_heure, acces_traitement, acces_plan_traitement) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, o.getNom());
        ps.setString(2, o.getDescription());
        ps.setFloat(3, o.getPrixMensuel());
        ps.setString(4, o.getAvantages());
        ps.setString(5, o.getCouleurPrimaire());
        ps.setString(6, o.getCouleurSecondaire());
        ps.setBoolean(7, o.isEstActive());
        ps.setBoolean(8, o.isEstRecommandee());
        ps.setInt(9, o.getDiagnosticsParHeure());
        ps.setBoolean(10, o.isAccesTraitement());
        ps.setBoolean(11, o.isAccesPlanTraitement());
        ps.executeUpdate();

        // Récupérer l'ID généré
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            o.setId(rs.getInt(1));
        }

    }

    // 2. MODIFIER une offre (auto-annule les abonnements si l'offre est désactivée)
    public void modifier(Offre o) throws SQLException {
        // Si l'offre est désactivée, annuler tous les abonnements actifs liés
        if (!o.isEstActive()) {
            AbonnementService abonnementService = new AbonnementService();
            abonnementService.annulerAbonnementsParOffre(o.getId());
        }

        String sql = "UPDATE offre SET nom=?, description=?, prix_mensuel=?, avantages=?, couleur_primaire=?, couleur_secondaire=?, est_active=?, est_recommandee=?, diagnostics_par_heure=?, acces_traitement=?, acces_plan_traitement=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, o.getNom());
        ps.setString(2, o.getDescription());
        ps.setFloat(3, o.getPrixMensuel());
        ps.setString(4, o.getAvantages());
        ps.setString(5, o.getCouleurPrimaire());
        ps.setString(6, o.getCouleurSecondaire());
        ps.setBoolean(7, o.isEstActive());
        ps.setBoolean(8, o.isEstRecommandee());
        ps.setInt(9, o.getDiagnosticsParHeure());
        ps.setBoolean(10, o.isAccesTraitement());
        ps.setBoolean(11, o.isAccesPlanTraitement());
        ps.setInt(12, o.getId());
        ps.executeUpdate();
    }

    // 3. SUPPRIMER une offre
    public void supprimer(int id) throws SQLException {
        // D'abord vérifier si des abonnements utilisent cette offre
        String checkSql = "SELECT COUNT(*) FROM abonnement WHERE offre_id = ?";
        PreparedStatement checkPs = cnx.prepareStatement(checkSql);
        checkPs.setInt(1, id);
        ResultSet rs = checkPs.executeQuery();
        if (rs.next() && rs.getInt(1) > 0) {
            throw new SQLException(
                    "Impossible de supprimer: " + rs.getInt(1) + " abonnement(s) utilisent cette offre.");
        }

        String sql = "DELETE FROM offre WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

    }

    // 4. RÉCUPÉRER TOUTES les offres
    @Override
    public List<Offre> recuperer() throws SQLException {
        List<Offre> offres = new ArrayList<>();
        String sql = "SELECT * FROM offre ORDER BY id DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            offres.add(mapResultSetToOffre(rs));
        }
        return offres;
    }

    @Override
    public List<Offre> rechercher(String keyword) throws SQLException {
        List<Offre> offres = new ArrayList<>();
        String sql = "SELECT * FROM offre WHERE LOWER(nom) LIKE ? OR LOWER(description) LIKE ? ORDER BY id DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        String pattern = "%" + keyword.toLowerCase() + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            offres.add(mapResultSetToOffre(rs));
        }
        return offres;
    }

    // 5. RÉCUPÉRER les offres ACTIVES uniquement (pour affichage client)
    public List<Offre> recupererActives() throws SQLException {
        List<Offre> offres = new ArrayList<>();
        String sql = "SELECT * FROM offre WHERE est_active = TRUE ORDER BY est_recommandee DESC, prix_mensuel ASC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            offres.add(mapResultSetToOffre(rs));
        }
        return offres;
    }

    // --- Méthode utilitaire pour mapper ResultSet -> Offre ---
    private Offre mapResultSetToOffre(ResultSet rs) throws SQLException {
        // Lecture sécurisée des nouvelles colonnes pour éviter les crashs si la BDD
        // n'est pas à jour
        int diagParHeure = 3;
        try {
            diagParHeure = rs.getInt("diagnostics_par_heure");
        } catch (SQLException e) {
            // Colonne inexistante, on garde la valeur par défaut
        }

        boolean accesTraitement = false;
        try {
            accesTraitement = rs.getBoolean("acces_traitement");
        } catch (SQLException e) {
            // Colonne inexistante
        }

        boolean accesPlanTraitement = false;
        try {
            accesPlanTraitement = rs.getBoolean("acces_plan_traitement");
        } catch (SQLException e) {
            // Colonne inexistante
        }

        return new Offre(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("description"),
                rs.getFloat("prix_mensuel"),
                rs.getString("avantages"),
                rs.getString("couleur_primaire"),
                rs.getString("couleur_secondaire"),
                rs.getBoolean("est_active"),
                rs.getBoolean("est_recommandee"),
                rs.getTimestamp("date_creation"),
                diagParHeure,
                accesTraitement,
                accesPlanTraitement);
    }

}
