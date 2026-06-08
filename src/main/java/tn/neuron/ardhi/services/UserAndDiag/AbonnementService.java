package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.interfaces.IService;

import tn.neuron.ardhi.models.UserAndDiag.Abonnement;
import tn.neuron.ardhi.models.UserAndDiag.Offre;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AbonnementService implements IService<Abonnement> {

    private Connection cnx;

    // Auto-create table if not exists

    public AbonnementService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    // 1. AJOUTER un abonnement (annule automatiquement les anciens abonnements
    // actifs)
    public void ajouter(Abonnement a) throws SQLException {
        // D'abord, annuler tous les abonnements actifs de cet utilisateur
        annulerTousLesAbonnementsActifs(a.getUserId());

        String sql = "INSERT INTO abonnement (type, prix, date_debut, date_fin, statut, user_id, offre_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getType());
        ps.setFloat(2, a.getPrix());
        ps.setDate(3, a.getDateDebut());
        ps.setDate(4, a.getDateFin());
        ps.setString(5, a.getStatut());
        ps.setInt(6, a.getUserId());
        if (a.getOffreId() > 0) {
            ps.setInt(7, a.getOffreId());
        } else {
            ps.setNull(7, java.sql.Types.INTEGER);
        }
        ps.executeUpdate();

    }

    // 1b. ANNULER TOUS les abonnements actifs d'un utilisateur (utilisé avant
    // l'ajout d'un nouveau)
    public int annulerTousLesAbonnementsActifs(int userId) throws SQLException {
        String sql = "UPDATE abonnement SET statut = 'ANNULE' WHERE user_id = ? AND statut = 'ACTIF'";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        int updated = ps.executeUpdate();
        return updated;
    }

    // 1c. ANNULER TOUS les abonnements actifs liés à une offre (utilisé quand
    // l'offre est désactivée)
    public int annulerAbonnementsParOffre(int offreId) throws SQLException {
        String sql = "UPDATE abonnement SET statut = 'ANNULE' WHERE offre_id = ? AND statut = 'ACTIF'";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, offreId);
        int updated = ps.executeUpdate();
        return updated;
    }

    // 2. AFFICHER les abonnements d'un utilisateur précis
    public List<Abonnement> recupererParUser(int userId) throws SQLException {
        List<Abonnement> abonnements = new ArrayList<>();
        String sql = "SELECT * FROM abonnement WHERE user_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Abonnement a = mapResultSetToAbonnement(rs);
            abonnements.add(a);
        }
        return abonnements;
    }

    // 3. VERIFIER si un user a un abonnement ACTIF
    public boolean aAbonnementActif(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM abonnement WHERE user_id = ? AND statut = 'ACTIF' AND date_fin >= CURRENT_DATE";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    // 3b. GET the active subscription's Offer for a user (returns null if no active
    // subscription)
    public Offre getOffreActiveParUser(int userId) throws SQLException {
        String sql = "SELECT o.* FROM offre o " +
                "JOIN abonnement a ON a.offre_id = o.id " +
                "WHERE a.user_id = ? AND a.statut = 'ACTIF' AND a.date_fin >= CURRENT_DATE " +
                "ORDER BY a.date_fin DESC LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
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
                    rs.getInt("diagnostics_par_heure"),
                    rs.getBoolean("acces_traitement"),
                    rs.getBoolean("acces_plan_traitement"));
        }
        return null;
    }

    // 4. ANNULER un abonnement (Update status to ANNULE)
    public void annulerAbonnement(int userId) throws SQLException {
        String sql = "UPDATE abonnement SET statut = 'ANNULE' WHERE user_id = ? AND statut = 'ACTIF'";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();

    }

    // 5. METTRE A JOUR les abonnements expirés automatiquement
    public void mettreAJourExpirations() throws SQLException {
        String sql = "UPDATE abonnement SET statut = 'EXPIRE' WHERE date_fin < CURRENT_DATE AND statut = 'ACTIF'";
        Statement st = cnx.createStatement();
        st.executeUpdate(sql);
    }

    // 6. RECUPERER TOUS les abonnements (ADMIN)
    @Override
    public List<Abonnement> recuperer() throws SQLException {
        // D'abord, mettre à jour les expirations
        mettreAJourExpirations();
        List<Abonnement> abonnements = new ArrayList<>();
        String sql = "SELECT * FROM abonnement ORDER BY id DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Abonnement a = mapResultSetToAbonnement(rs);
            abonnements.add(a);
        }
        return abonnements;
    }

    @Override
    public List<Abonnement> rechercher(String keyword) throws SQLException {
        mettreAJourExpirations();
        List<Abonnement> abonnements = new ArrayList<>();
        String sql = "SELECT * FROM abonnement WHERE LOWER(type) LIKE ? OR LOWER(statut) LIKE ? ORDER BY id DESC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        String pattern = "%" + keyword.toLowerCase() + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            abonnements.add(mapResultSetToAbonnement(rs));
        }
        return abonnements;
    }

    // 6. MODIFIER un abonnement (ADMIN)
    // Enforce: only one active subscription per user
    public void modifier(Abonnement a) throws SQLException {
        // If setting to ACTIF, cancel all other active subscriptions for this user
        // first
        if ("ACTIF".equals(a.getStatut())) {
            String cancelSql = "UPDATE abonnement SET statut = 'ANNULE' WHERE user_id = ? AND statut = 'ACTIF' AND id != ?";
            PreparedStatement cancelPs = cnx.prepareStatement(cancelSql);
            cancelPs.setInt(1, a.getUserId());
            cancelPs.setInt(2, a.getId());
            cancelPs.executeUpdate();
        }

        String sql = "UPDATE abonnement SET type=?, prix=?, date_debut=?, date_fin=?, statut=?, user_id=?, offre_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getType());
        ps.setFloat(2, a.getPrix());
        ps.setDate(3, a.getDateDebut());
        ps.setDate(4, a.getDateFin());
        ps.setString(5, a.getStatut());
        ps.setInt(6, a.getUserId());
        if (a.getOffreId() > 0) {
            ps.setInt(7, a.getOffreId());
        } else {
            ps.setNull(7, java.sql.Types.INTEGER);
        }
        ps.setInt(8, a.getId());
        ps.executeUpdate();

    }

    // 7. SUPPRIMER un abonnement (ADMIN)
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM abonnement WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

    }

    // --- Méthode utilitaire pour mapper ResultSet -> Abonnement ---
    private Abonnement mapResultSetToAbonnement(ResultSet rs) throws SQLException {
        int offreId = 0;
        try {
            offreId = rs.getInt("offre_id");
            if (rs.wasNull()) {
                offreId = 0;
            }
        } catch (SQLException e) {
            // Colonne potentiellement manquante - ignorée silencieusement
        }

        Abonnement a = new Abonnement(
                rs.getInt("id"),
                offreId,
                rs.getString("type"),
                rs.getFloat("prix"),
                rs.getDate("date_debut"),
                rs.getDate("date_fin"),
                rs.getString("statut"),
                rs.getInt("user_id"));
        return a;
    }

}