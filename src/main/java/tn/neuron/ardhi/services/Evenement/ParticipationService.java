package tn.neuron.ardhi.services.Evenement;

import tn.neuron.ardhi.models.Evenement.Participation;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService {
    private Connection connection;

    public ParticipationService() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    // Créer une participation (inscription à un événement)
    public boolean inscrireParticipant(Participation participation) {
        // Vérifier d'abord si l'événement n'est pas complet
        if (isEvenementComplet(participation.getIdEvenement())) {
            System.out.println("Événement complet, inscription impossible");
            return false;
        }

        // Vérifier si l'utilisateur n'est pas déjà inscrit
        if (isUserDejaInscrit(participation.getIdEvenement(), participation.getIdUtilisateur())) {
            System.out.println("L'utilisateur est déjà inscrit à cet événement");
            return false;
        }

        String query = "INSERT INTO participation (id_evenement, id_utilisateur, date_inscription, " +
                "statut, commentaire) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, participation.getIdEvenement());
            stmt.setInt(2, participation.getIdUtilisateur());
            stmt.setTimestamp(3, Timestamp.valueOf(participation.getDateInscription()));
            stmt.setString(4, participation.getStatut());
            stmt.setString(5, participation.getCommentaire());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    participation.setId(rs.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'inscription: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Vérifier si un utilisateur est déjà inscrit à un événement
    public boolean isUserDejaInscrit(int idEvenement, int idUtilisateur) {
        String query = "SELECT COUNT(*) as count FROM participation " +
                "WHERE id_evenement = ? AND id_utilisateur = ? AND statut != 'ANNULE'";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idEvenement);
            stmt.setInt(2, idUtilisateur);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification d'inscription: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // Vérifier si un événement est complet
    private boolean isEvenementComplet(int idEvenement) {
        String query = "SELECT e.nombre_places_max, COUNT(p.id) as nb_participants " +
                "FROM evenement e " +
                "LEFT JOIN participation p ON e.id = p.id_evenement AND p.statut = 'CONFIRME' " +
                "WHERE e.id = ? " +
                "GROUP BY e.id, e.nombre_places_max";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idEvenement);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int placesMax = rs.getInt("nombre_places_max");
                int nbParticipants = rs.getInt("nb_participants");
                return nbParticipants >= placesMax;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification des places: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // Récupérer toutes les participations d'un événement
    public List<Participation> getParticipationsByEvenement(int idEvenement) {
        List<Participation> participations = new ArrayList<>();
        String query = "SELECT p.*, u.nom, u.prenom, u.email " +
                "FROM participation p " +
                "JOIN user u ON p.id_utilisateur = u.id " +
                "WHERE p.id_evenement = ? " +
                "ORDER BY p.date_inscription DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idEvenement);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Participation participation = extractParticipationFromResultSet(rs);
                participation.setNomUtilisateur(rs.getString("nom"));
                participation.setPrenomUtilisateur(rs.getString("prenom"));
                participation.setEmailUtilisateur(rs.getString("email"));
                participations.add(participation);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des participations: " + e.getMessage());
            e.printStackTrace();
        }

        return participations;
    }

    // Récupérer les événements auxquels un utilisateur participe
    public List<Participation> getParticipationsByUtilisateur(int idUtilisateur) {
        List<Participation> participations = new ArrayList<>();
        String query = "SELECT p.*, e.titre as titre_evenement " +
                "FROM participation p " +
                "JOIN evenement e ON p.id_evenement = e.id " +
                "WHERE p.id_utilisateur = ? " +
                "ORDER BY p.date_inscription DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idUtilisateur);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Participation participation = extractParticipationFromResultSet(rs);
                participations.add(participation);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des participations de l'utilisateur: " + e.getMessage());
            e.printStackTrace();
        }

        return participations;
    }

    // Récupérer une participation spécifique
    public Participation getParticipationById(int id) {
        String query = "SELECT p.*, u.nom, u.prenom, u.email " +
                "FROM participation p " +
                "JOIN user u ON p.id_utilisateur = u.id " +
                "WHERE p.id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Participation participation = extractParticipationFromResultSet(rs);
                participation.setNomUtilisateur(rs.getString("nom"));
                participation.setPrenomUtilisateur(rs.getString("prenom"));
                participation.setEmailUtilisateur(rs.getString("email"));
                return participation;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la participation: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // Annuler une participation
    public boolean annulerParticipation(int idParticipation) {
        String query = "UPDATE participation SET statut = 'ANNULE' WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idParticipation);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'annulation de la participation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Annuler une participation par utilisateur et événement
    public boolean annulerParticipation(int idEvenement, int idUtilisateur) {
        String query = "UPDATE participation SET statut = 'ANNULE' " +
                "WHERE id_evenement = ? AND id_utilisateur = ? AND statut != 'ANNULE'";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idEvenement);
            stmt.setInt(2, idUtilisateur);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'annulation de la participation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Supprimer une participation
    public boolean supprimerParticipation(int id) {
        String query = "DELETE FROM participation WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la participation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Ajouter un avis et une note (VERSION UNIQUE)
    public boolean ajouterAvis(int idParticipation, int note, String avis) {
        String query = "UPDATE participation SET note = ?, avis = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, note);
            stmt.setString(2, avis);
            stmt.setInt(3, idParticipation);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de l'avis: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Récupérer les avis d'un événement
    public List<Participation> getAvisByEvenement(int idEvenement) {
        List<Participation> avis = new ArrayList<>();
        String query = "SELECT p.*, u.nom, u.prenom " +
                "FROM participation p " +
                "JOIN user u ON p.id_utilisateur = u.id " +
                "WHERE p.id_evenement = ? AND p.avis IS NOT NULL AND p.avis != '' " +
                "ORDER BY p.date_inscription DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idEvenement);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Participation participation = extractParticipationFromResultSet(rs);
                participation.setNomUtilisateur(rs.getString("nom"));
                participation.setPrenomUtilisateur(rs.getString("prenom"));
                avis.add(participation);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des avis: " + e.getMessage());
            e.printStackTrace();
        }

        return avis;
    }

    // Calculer la note moyenne d'un événement (VERSION UNIQUE)
    public double getNoteMoyenneEvenement(int idEvenement) {
        String query = "SELECT AVG(note) as moyenne FROM participation " +
                "WHERE id_evenement = ? AND note > 0";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idEvenement);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("moyenne");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul de la note moyenne: " + e.getMessage());
            e.printStackTrace();
        }

        return 0.0;
    }

    // Récupérer le nombre de participants confirmés
    public int getNombreParticipantsConfirmes(int idEvenement) {
        String query = "SELECT COUNT(*) as count FROM participation " +
                "WHERE id_evenement = ? AND statut = 'CONFIRME'";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idEvenement);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage des participants: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    // Méthode helper pour extraire une participation du ResultSet
    private Participation extractParticipationFromResultSet(ResultSet rs) throws SQLException {
        Participation participation = new Participation();
        participation.setId(rs.getInt("id"));
        participation.setIdEvenement(rs.getInt("id_evenement"));
        participation.setIdUtilisateur(rs.getInt("id_utilisateur"));

        Timestamp dateInscription = rs.getTimestamp("date_inscription");
        if (dateInscription != null) {
            participation.setDateInscription(dateInscription.toLocalDateTime());
        }

        participation.setStatut(rs.getString("statut"));
        participation.setCommentaire(rs.getString("commentaire"));
        participation.setNote(rs.getInt("note"));
        participation.setAvis(rs.getString("avis"));
        participation.setAttestationEnvoyee(rs.getBoolean("attestation_envoyee"));

        return participation;
    }

    // Statistiques
    public int getNombreTotalParticipations() {
        String query = "SELECT COUNT(*) as total FROM participation WHERE statut = 'CONFIRME'";
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

    public int getNombreParticipationsByUtilisateur(int idUtilisateur) {
        String query = "SELECT COUNT(*) as total FROM participation " +
                "WHERE id_utilisateur = ? AND statut = 'CONFIRME'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idUtilisateur);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Récupérer les participations uniquement pour les événements créés par un
    // utilisateur (AGRICULTEUR)
    public List<Participation> getParticipationsByCreateurEvenement(int idCreateur) {
        List<Participation> participations = new ArrayList<>();
        String query = "SELECT p.*, u.nom, u.prenom, u.email, e.titre as titre_evenement " +
                "FROM participation p " +
                "JOIN user u ON p.id_utilisateur = u.id " +
                "JOIN evenement e ON p.id_evenement = e.id " +
                "WHERE e.id_createur = ? " +
                "ORDER BY p.date_inscription DESC";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idCreateur);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Participation participation = extractParticipationFromResultSet(rs);
                participation.setNomUtilisateur(rs.getString("nom"));
                participation.setPrenomUtilisateur(rs.getString("prenom"));
                participation.setEmailUtilisateur(rs.getString("email"));
                participation.setTitreEvenement(rs.getString("titre_evenement"));
                participations.add(participation);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getParticipationsByCreateurEvenement: " + e.getMessage());
            e.printStackTrace();
        }

        return participations;
    }

    // Récupérer toutes les participations (pour admin)
    public List<Participation> getAllParticipations() {
        List<Participation> participations = new ArrayList<>();
        String query = "SELECT p.*, u.nom, u.prenom, u.email, e.titre as titre_evenement " +
                "FROM participation p " +
                "JOIN user u ON p.id_utilisateur = u.id " +
                "JOIN evenement e ON p.id_evenement = e.id " +
                "ORDER BY p.date_inscription DESC";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Participation participation = extractParticipationFromResultSet(rs);
                participation.setNomUtilisateur(rs.getString("nom"));
                participation.setPrenomUtilisateur(rs.getString("prenom"));
                participation.setEmailUtilisateur(rs.getString("email"));
                participation.setTitreEvenement(rs.getString("titre_evenement"));
                participations.add(participation);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de toutes les participations: " + e.getMessage());
            e.printStackTrace();
        }

        return participations;
    }

    // Mettre à jour le statut d'une participation
    public boolean updateStatut(int idParticipation, String nouveauStatut) {
        String query = "UPDATE participation SET statut = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, nouveauStatut);
            stmt.setInt(2, idParticipation);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du statut: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Mettre à jour le nombre de personnes
    public boolean updateNombrePersonnes(int idParticipation, int nombrePersonnes) {
        String query = "UPDATE participation SET nombre_personnes = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, nombrePersonnes);
            stmt.setInt(2, idParticipation);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du nombre de personnes: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    // Marquer qu'une attestation a été envoyée
    public boolean marquerAttestationEnvoyee(int participationId) {
        String sql = "UPDATE participation SET attestation_envoyee = TRUE WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, participationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur marquer attestation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Vérifier si attestation déjà envoyée
    public boolean isAttestationEnvoyee(int participationId) {
        String sql = "SELECT attestation_envoyee FROM participation WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, participationId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getBoolean("attestation_envoyee");
        } catch (SQLException e) {
            System.err.println("Erreur vérification attestation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public int getNombreParticipants(int idEvenement, String statut) {
        String query = "SELECT COUNT(*) as count FROM participation " +
                "WHERE id_evenement = ? AND statut = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, idEvenement);
            stmt.setString(2, statut);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage des participants: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
}