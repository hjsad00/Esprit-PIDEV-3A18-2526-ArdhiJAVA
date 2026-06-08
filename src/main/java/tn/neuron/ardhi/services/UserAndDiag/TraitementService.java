package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.interfaces.IService;

import tn.neuron.ardhi.models.UserAndDiag.Traitement;
import tn.neuron.ardhi.models.UserAndDiag.TypeTraitement;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TraitementService implements IService<Traitement> {

    private Connection cnx = MyDatabase.getInstance().getCnx();

    // Auto-create table if not exists

    @Override
    public void ajouter(Traitement t) throws SQLException {
        String req = "INSERT INTO traitement (diagnostic_id, solution_nom, description_detaillee, type_traitement) VALUES (?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, t.getDiagnosticId());
        pst.setString(2, t.getSolutionNom());
        pst.setString(3, t.getDescriptionDetaillee());
        pst.setString(4, t.getTypeTraitement().name());
        pst.executeUpdate();
    }

    @Override
    public void modifier(Traitement t) throws SQLException {
        String req = "UPDATE traitement SET diagnostic_id = ?, solution_nom = ?, description_detaillee = ?, type_traitement = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, t.getDiagnosticId());
        pst.setString(2, t.getSolutionNom());
        pst.setString(3, t.getDescriptionDetaillee());
        pst.setString(4, t.getTypeTraitement().name());
        pst.setInt(5, t.getId());
        pst.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM traitement WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
    }

    // --- Méthode utilitaire pour mapper ResultSet -> Traitement ---
    private Traitement mapResultSetToTraitement(ResultSet rs) throws SQLException {
        TypeTraitement type;
        try {
            type = TypeTraitement.valueOf(rs.getString("type_traitement"));
        } catch (Exception e) {
            type = TypeTraitement.AUTRE; // Default fallback
        }
        Traitement t = new Traitement(
                rs.getInt("diagnostic_id"),
                rs.getString("solution_nom"),
                rs.getString("description_detaillee"),
                type);
        t.setId(rs.getInt("id"));
        return t;
    }

    @Override
    public List<Traitement> recuperer() throws SQLException {
        List<Traitement> list = new ArrayList<>();
        String req = "SELECT * FROM traitement ORDER BY id DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapResultSetToTraitement(rs));
        }
        return list;
    }

    @Override
    public List<Traitement> rechercher(String keyword) throws SQLException {
        List<Traitement> list = new ArrayList<>();
        String req = "SELECT * FROM traitement WHERE LOWER(solution_nom) LIKE ? OR LOWER(description_detaillee) LIKE ? ORDER BY id DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        String pattern = "%" + keyword.toLowerCase() + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSetToTraitement(rs));
        }
        return list;
    }

    /**
     * Récupère le traitement associé à un diagnostic spécifique.
     * 
     * @param diagnosticId L'ID du diagnostic
     * @return Le traitement associé ou null si aucun n'existe
     */
    public Traitement getByDiagnosticId(int diagnosticId) throws SQLException {
        String req = "SELECT * FROM traitement WHERE diagnostic_id = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, diagnosticId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return mapResultSetToTraitement(rs);
        }
        return null;
    }
}