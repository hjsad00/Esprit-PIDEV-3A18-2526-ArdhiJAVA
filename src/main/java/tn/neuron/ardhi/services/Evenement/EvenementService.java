package tn.neuron.ardhi.services.Evenement;

import tn.neuron.ardhi.models.Evenement.Evenement;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService {
    private Connection connection;

    public EvenementService() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    // Créer un événement
    public boolean creerEvenement(Evenement evenement) {
        String query = "INSERT INTO evenement (titre, description, lieu, date_debut, date_fin, " +
                "type, nombre_places_max, organisateur, image_url, statut, date_creation, id_createur) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, evenement.getTitre());
            stmt.setString(2, evenement.getDescription());
            stmt.setString(3, evenement.getLieu());
            stmt.setDate(4, Date.valueOf(evenement.getDateDebut()));
            stmt.setDate(5, Date.valueOf(evenement.getDateFin()));
            stmt.setString(6, evenement.getType());
            stmt.setInt(7, evenement.getNombrePlacesMax());
            stmt.setString(8, evenement.getOrganisateur());
            stmt.setString(9, evenement.getImageUrl());
            stmt.setString(10, evenement.getStatut());
            stmt.setTimestamp(11, Timestamp.valueOf(evenement.getDateCreation()));
            stmt.setInt(12, evenement.getIdCreateur());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    evenement.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de l'événement: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Récupérer tous les événements
    public List<Evenement> getAllEvenements() {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT e.*, COUNT(p.id) as nb_participants " +
                "FROM evenement e " +
                "LEFT JOIN participation p ON e.id = p.id_evenement AND p.statut = 'CONFIRME' " +
                "GROUP BY e.id " +
                "ORDER BY e.date_debut DESC";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Evenement evenement = extractEvenementFromResultSet(rs);
                evenement.setNombreParticipants(rs.getInt("nb_participants"));
                evenements.add(evenement);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des événements: " + e.getMessage());
            e.printStackTrace();
        }

        return evenements;
    }

    // Récupérer un événement par ID
    public Evenement getEvenementById(int id) {
        String query = "SELECT e.*, COUNT(p.id) as nb_participants " +
                "FROM evenement e " +
                "LEFT JOIN participation p ON e.id = p.id_evenement AND p.statut = 'CONFIRME' " +
                "WHERE e.id = ? " +
                "GROUP BY e.id";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Evenement evenement = extractEvenementFromResultSet(rs);
                evenement.setNombreParticipants(rs.getInt("nb_participants"));
                return evenement;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'événement: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // Mettre à jour un événement
    public boolean modifierEvenement(Evenement evenement) {
        String query = "UPDATE evenement SET titre = ?, description = ?, lieu = ?, " +
                "date_debut = ?, date_fin = ?, type = ?, nombre_places_max = ?, " +
                "organisateur = ?, image_url = ?, statut = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, evenement.getTitre());
            stmt.setString(2, evenement.getDescription());
            stmt.setString(3, evenement.getLieu());
            stmt.setDate(4, Date.valueOf(evenement.getDateDebut()));
            stmt.setDate(5, Date.valueOf(evenement.getDateFin()));
            stmt.setString(6, evenement.getType());
            stmt.setInt(7, evenement.getNombrePlacesMax());
            stmt.setString(8, evenement.getOrganisateur());
            stmt.setString(9, evenement.getImageUrl());
            stmt.setString(10, evenement.getStatut());
            stmt.setInt(11, evenement.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification de l'événement: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Supprimer un événement
    public boolean supprimerEvenement(int id) {
        String query = "DELETE FROM evenement WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de l'événement: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Rechercher des événements
    public List<Evenement> rechercherEvenements(String motCle) {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT e.*, COUNT(p.id) as nb_participants " +
                "FROM evenement e " +
                "LEFT JOIN participation p ON e.id = p.id_evenement AND p.statut = 'CONFIRME' " +
                "WHERE e.titre LIKE ? OR e.description LIKE ? OR e.lieu LIKE ? " +
                "GROUP BY e.id " +
                "ORDER BY e.date_debut DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            String searchPattern = "%" + motCle + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Evenement evenement = extractEvenementFromResultSet(rs);
                evenement.setNombreParticipants(rs.getInt("nb_participants"));
                evenements.add(evenement);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche d'événements: " + e.getMessage());
            e.printStackTrace();
        }

        return evenements;
    }

    // Filtrer par type
    public List<Evenement> getEvenementsByType(String type) {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT e.*, COUNT(p.id) as nb_participants " +
                "FROM evenement e " +
                "LEFT JOIN participation p ON e.id = p.id_evenement AND p.statut = 'CONFIRME' " +
                "WHERE e.type = ? " +
                "GROUP BY e.id " +
                "ORDER BY e.date_debut DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, type);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Evenement evenement = extractEvenementFromResultSet(rs);
                evenement.setNombreParticipants(rs.getInt("nb_participants"));
                evenements.add(evenement);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du filtrage par type: " + e.getMessage());
            e.printStackTrace();
        }

        return evenements;
    }

    // Filtrer par statut
    public List<Evenement> getEvenementsByStatut(String statut) {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT e.*, COUNT(p.id) as nb_participants " +
                "FROM evenement e " +
                "LEFT JOIN participation p ON e.id = p.id_evenement AND p.statut = 'CONFIRME' " +
                "WHERE e.statut = ? " +
                "GROUP BY e.id " +
                "ORDER BY e.date_debut DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, statut);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Evenement evenement = extractEvenementFromResultSet(rs);
                evenement.setNombreParticipants(rs.getInt("nb_participants"));
                evenements.add(evenement);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du filtrage par statut: " + e.getMessage());
            e.printStackTrace();
        }

        return evenements;
    }

    // Récupérer les événements à venir
    public List<Evenement> getEvenementsAVenir() {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT e.*, COUNT(p.id) as nb_participants " +
                "FROM evenement e " +
                "LEFT JOIN participation p ON e.id = p.id_evenement AND p.statut = 'CONFIRME' " +
                "WHERE e.date_debut >= CURDATE() AND e.statut = 'A_VENIR' " +
                "GROUP BY e.id " +
                "ORDER BY e.date_debut ASC";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Evenement evenement = extractEvenementFromResultSet(rs);
                evenement.setNombreParticipants(rs.getInt("nb_participants"));
                evenements.add(evenement);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des événements à venir: " + e.getMessage());
            e.printStackTrace();
        }

        return evenements;
    }

    // Récupérer les événements créés par un utilisateur
    public List<Evenement> getEvenementsByCreateur(int idCreateur) {
        List<Evenement> evenements = new ArrayList<>();
        String query = "SELECT e.*, COUNT(p.id) as nb_participants " +
                "FROM evenement e " +
                "LEFT JOIN participation p ON e.id = p.id_evenement AND p.statut = 'CONFIRME' " +
                "WHERE e.id_createur = ? " +
                "GROUP BY e.id " +
                "ORDER BY e.date_creation DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idCreateur);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Evenement evenement = extractEvenementFromResultSet(rs);
                evenement.setNombreParticipants(rs.getInt("nb_participants"));
                evenements.add(evenement);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des événements du créateur: " + e.getMessage());
            e.printStackTrace();
        }

        return evenements;
    }

    // Méthode helper pour extraire un événement du ResultSet
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

    // Obtenir des statistiques
    public int getNombreTotalEvenements() {
        String query = "SELECT COUNT(*) as total FROM evenement";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getNombreEvenementsActifs() {
        String query = "SELECT COUNT(*) as total FROM evenement WHERE statut IN ('A_VENIR', 'EN_COURS')";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}