package tn.neuron.ardhi.services.Evenement;

import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.models.Evenement.EvenementFavori;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les favoris d'événements
 */
public class FavorisService {

    private Connection connection;
    private EvenementService evenementService;

    public FavorisService() {
        this.connection = MyDatabase.getInstance().getCnx();
        this.evenementService = new EvenementService();
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Ajoute un événement aux favoris
     */
    public boolean ajouterFavori(int idEvenement, int idUtilisateur) {
        String sql = "INSERT INTO evenement_favoris (id_evenement, id_utilisateur) VALUES (?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idEvenement);
            pst.setInt(2, idUtilisateur);

            int result = pst.executeUpdate();

            if (result > 0) {
                System.out.println("✓ Événement " + idEvenement + " ajouté aux favoris de l'utilisateur " + idUtilisateur);
                return true;
            }

        } catch (SQLException e) {
            // Si c'est une contrainte unique, c'est déjà en favoris
            if (e.getErrorCode() == 1062) { // Duplicate entry
                System.out.println("⚠ Événement déjà en favoris");
            } else {
                System.err.println("✗ Erreur ajout favori: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Retire un événement des favoris
     */
    public boolean retirerFavori(int idEvenement, int idUtilisateur) {
        String sql = "DELETE FROM evenement_favoris WHERE id_evenement = ? AND id_utilisateur = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idEvenement);
            pst.setInt(2, idUtilisateur);

            int result = pst.executeUpdate();

            if (result > 0) {
                System.out.println("✓ Événement " + idEvenement + " retiré des favoris");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur retrait favori: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Toggle favori (ajoute si pas présent, retire si présent)
     */
    public boolean toggleFavori(int idEvenement, int idUtilisateur) {
        if (estFavori(idEvenement, idUtilisateur)) {
            return retirerFavori(idEvenement, idUtilisateur);
        } else {
            return ajouterFavori(idEvenement, idUtilisateur);
        }
    }

    // ==================== QUERIES ====================

    /**
     * Vérifie si un événement est en favoris
     */
    public boolean estFavori(int idEvenement, int idUtilisateur) {
        String sql = "SELECT COUNT(*) FROM evenement_favoris WHERE id_evenement = ? AND id_utilisateur = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idEvenement);
            pst.setInt(2, idUtilisateur);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur vérification favori: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Récupère tous les événements favoris d'un utilisateur
     */
    public List<Evenement> getFavorisUtilisateur(int idUtilisateur) {
        List<Evenement> favoris = new ArrayList<>();

        String sql = "SELECT e.*, COUNT(p.id) as nb_participants " +
                "FROM evenement e " +
                "INNER JOIN evenement_favoris f ON e.id = f.id_evenement " +
                "LEFT JOIN participation p ON e.id = p.id_evenement AND p.statut = 'CONFIRME' " +
                "WHERE f.id_utilisateur = ? " +
                "GROUP BY e.id " +
                "ORDER BY f.date_ajout DESC";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idUtilisateur);

            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Evenement event = extractEvenementFromResultSet(rs);
                event.setNombreParticipants(rs.getInt("nb_participants"));
                favoris.add(event);
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur récupération favoris: " + e.getMessage());
            e.printStackTrace();
        }

        return favoris;
    }

    /**
     * Récupère le nombre de favoris d'un événement
     */
    public int getNombreFavoris(int idEvenement) {
        String sql = "SELECT COUNT(*) FROM evenement_favoris WHERE id_evenement = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idEvenement);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur comptage favoris: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Récupère tous les favoris (pour admin)
     */
    public List<EvenementFavori> getAllFavoris() {
        List<EvenementFavori> favoris = new ArrayList<>();

        String sql = "SELECT f.*, e.titre as titre_evenement, e.type as type_evenement, " +
                "u.nom as nom_utilisateur, u.prenom as prenom_utilisateur " +
                "FROM evenement_favoris f " +
                "INNER JOIN evenement e ON f.id_evenement = e.id " +
                "INNER JOIN user u ON f.id_utilisateur = u.id " +
                "ORDER BY f.date_ajout DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                EvenementFavori favori = new EvenementFavori();
                favori.setId(rs.getInt("id"));
                favori.setIdEvenement(rs.getInt("id_evenement"));
                favori.setIdUtilisateur(rs.getInt("id_utilisateur"));
                favori.setDateAjout(rs.getTimestamp("date_ajout").toLocalDateTime());
                favori.setTitreEvenement(rs.getString("titre_evenement"));
                favori.setTypeEvenement(rs.getString("type_evenement"));
                favori.setNomUtilisateur(rs.getString("nom_utilisateur"));
                favori.setPrenomUtilisateur(rs.getString("prenom_utilisateur"));

                favoris.add(favori);
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur récupération tous favoris: " + e.getMessage());
            e.printStackTrace();
        }

        return favoris;
    }

    /**
     * Récupère le nombre total de favoris d'un utilisateur
     */
    public int getNombreFavorisUtilisateur(int idUtilisateur) {
        String sql = "SELECT COUNT(*) FROM evenement_favoris WHERE id_utilisateur = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idUtilisateur);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur comptage favoris utilisateur: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Récupère les événements les plus favorisés (top N)
     */
    public List<Evenement> getTopEvenementsFavoris(int limit) {
        List<Evenement> topEvents = new ArrayList<>();

        String sql = "SELECT e.*, COUNT(f.id) as nb_favoris, " +
                "       (SELECT COUNT(*) FROM participation p WHERE p.id_evenement = e.id AND p.statut = 'CONFIRME') as nb_participants " +
                "FROM evenement e " +
                "INNER JOIN evenement_favoris f ON e.id = f.id_evenement " +
                "GROUP BY e.id " +
                "ORDER BY nb_favoris DESC " +
                "LIMIT ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, limit);

            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Evenement event = extractEvenementFromResultSet(rs);
                event.setNombreParticipants(rs.getInt("nb_participants"));
                topEvents.add(event);
            }

        } catch (SQLException e) {
            System.err.println("✗ Erreur top favoris: " + e.getMessage());
            e.printStackTrace();
        }

        return topEvents;
    }

    // ==================== BULK OPERATIONS ====================

    /**
     * Supprime tous les favoris d'un utilisateur
     */
    public boolean supprimerTousFavorisUtilisateur(int idUtilisateur) {
        String sql = "DELETE FROM evenement_favoris WHERE id_utilisateur = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idUtilisateur);

            int result = pst.executeUpdate();
            System.out.println("✓ " + result + " favoris supprimés pour l'utilisateur " + idUtilisateur);
            return true;

        } catch (SQLException e) {
            System.err.println("✗ Erreur suppression favoris utilisateur: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Extrait un événement depuis un ResultSet
     * (Copie de la méthode extractEvenementFromResultSet d'EvenementService)
     */
    private Evenement extractEvenementFromResultSet(ResultSet rs) throws SQLException {
        Evenement evenement = new Evenement();
        evenement.setId(rs.getInt("id"));
        evenement.setTitre(rs.getString("titre"));
        evenement.setDescription(rs.getString("description"));
        evenement.setLieu(rs.getString("lieu"));

        Date dateDebut = rs.getDate("date_debut");
        if (dateDebut != null) {
            evenement.setDateDebut(dateDebut.toLocalDate());
        }

        Date dateFin = rs.getDate("date_fin");
        if (dateFin != null) {
            evenement.setDateFin(dateFin.toLocalDate());
        }

        evenement.setType(rs.getString("type"));
        evenement.setNombrePlacesMax(rs.getInt("nombre_places_max"));
        evenement.setOrganisateur(rs.getString("organisateur"));
        evenement.setImageUrl(rs.getString("image_url"));
        evenement.setStatut(rs.getString("statut"));

        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            evenement.setDateCreation(dateCreation.toLocalDateTime());
        }

        evenement.setIdCreateur(rs.getInt("id_createur"));

        return evenement;
    }
}