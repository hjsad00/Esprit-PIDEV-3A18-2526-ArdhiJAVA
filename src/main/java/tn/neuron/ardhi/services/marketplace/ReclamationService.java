package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.interfaces.marketplace.IReclamationService;
import tn.neuron.ardhi.models.marketplace.Reclamation;
import tn.neuron.ardhi.models.marketplace.TypeReclamation;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;
import tn.neuron.ardhi.models.marketplace.StatutReclamation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService implements IReclamationService {

    private Connection connection;

    public ReclamationService() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    @Override
    public boolean creerReclamation(Reclamation reclamation) {
        System.out.println("TYPE = [" + reclamation.getType().name() + "]");

        String query = "INSERT INTO reclamation (description, `type`, statut, date_reclamation, nom_produit, id_produit, id_user) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, reclamation.getDescription());
            pstmt.setString(2, reclamation.getType().name());
            pstmt.setString(3, reclamation.getStatut().name());
            pstmt.setTimestamp(4, Timestamp.valueOf(reclamation.getDateReclamation()));
            pstmt.setString(5, reclamation.getNomProduit());
            pstmt.setInt(6, reclamation.getIdProduit());
            pstmt.setInt(7, reclamation.getIdUser());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        reclamation.setIdReclamation(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de la réclamation : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public List<Reclamation> getReclamationsByUser(int idUser) {
        List<Reclamation> reclamations = new ArrayList<>();
        String query = "SELECT * FROM reclamation WHERE id_user = ? ORDER BY date_reclamation DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idUser);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reclamations.add(extractReclamationFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des réclamations : " + e.getMessage());
            e.printStackTrace();
        }

        return reclamations;
    }

    @Override
    public List<Reclamation> getReclamationsByProduit(int idProduit) {
        List<Reclamation> reclamations = new ArrayList<>();
        String query = "SELECT * FROM reclamation WHERE id_produit = ? ORDER BY date_reclamation DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idProduit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reclamations.add(extractReclamationFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des réclamations : " + e.getMessage());
            e.printStackTrace();
        }

        return reclamations;
    }

    @Override
    public List<Reclamation> getAllReclamations() {
        List<Reclamation> reclamations = new ArrayList<>();
        String query = "SELECT * FROM reclamation ORDER BY date_reclamation DESC";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                reclamations.add(extractReclamationFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de toutes les réclamations : " + e.getMessage());
            e.printStackTrace();
        }

        return reclamations;
    }

    @Override
    public List<Reclamation> getReclamationsByStatut(StatutReclamation statut) {
        List<Reclamation> reclamations = new ArrayList<>();
        String query = "SELECT * FROM reclamation WHERE statut = ? ORDER BY date_reclamation DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, statut.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reclamations.add(extractReclamationFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du filtrage par statut : " + e.getMessage());
            e.printStackTrace();
        }

        return reclamations;
    }

    @Override
    public boolean updateStatut(int idReclamation, StatutReclamation nouveauStatut) {
        String query = "UPDATE reclamation SET statut = ? WHERE idReclamation = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, nouveauStatut.name());
            pstmt.setInt(2, idReclamation);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du statut : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean supprimerReclamation(int idReclamation) {
        String query = "DELETE FROM reclamation WHERE idReclamation = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idReclamation);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Reclamation extractReclamationFromResultSet(ResultSet rs) throws SQLException {
        Reclamation reclamation = new Reclamation();
        reclamation.setIdReclamation(rs.getInt("idReclamation"));
        reclamation.setDescription(rs.getString("description"));
        reclamation.setType(TypeReclamation.valueOf(rs.getString("type")));
        reclamation.setStatut(StatutReclamation.valueOf(rs.getString("statut")));
        reclamation.setDateReclamation(rs.getTimestamp("date_reclamation").toLocalDateTime());
        reclamation.setNomProduit(rs.getString("nom_produit"));
        reclamation.setIdProduit(rs.getInt("id_produit"));
        reclamation.setIdUser(rs.getInt("id_user"));
        return reclamation;
    }
}