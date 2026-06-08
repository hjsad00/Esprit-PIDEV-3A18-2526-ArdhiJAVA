package tn.neuron.ardhi.services.Parcelle_Cultures;

import tn.neuron.ardhi.interfaces.IService;
import tn.neuron.ardhi.models.Parcelle_Cultures.Culture;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Service CRUD pour l'entité Culture.
 * Version Premium: inclut surface_utilisee, rendement_estime et
 * la contrainte: somme surfaces cultures ≤ surface parcelle.
 */
public class CultureService implements IService<Culture> {

    private Connection cnx;

    public CultureService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    public CultureService(Connection cnx) {
        this.cnx = cnx;
    }

    // ==================== CRUD OPERATIONS ====================

    @Override
    public void ajouter(Culture c) throws SQLException {
        if (!c.isValidDates()) {
            throw new IllegalArgumentException(
                    "Contrainte temporelle violée: date_plantation doit être < date_recolte_prevue");
        }

        // Vérifier contrainte surface
        verifierContrainteSurface(c.getParcelleId(), c.getSurfaceUtilisee(), -1);

        String req = "INSERT INTO culture (nom_culture, type_culture, saison, date_plantation, date_recolte_prevue, etat_culture, parcelle_id, surface_utilisee, rendement_estime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, c.getNomCulture());
        ps.setString(2, c.getTypeCulture());
        ps.setString(3, c.getSaison());
        ps.setDate(4, c.getDatePlantation());
        ps.setDate(5, c.getDateRecoltePrevue());
        ps.setString(6, c.getEtatCulture());
        ps.setInt(7, c.getParcelleId());
        ps.setDouble(8, c.getSurfaceUtilisee());
        ps.setDouble(9, c.getRendementEstime());
        ps.executeUpdate();
    }

    public int ajouterEtRecupererId(Culture c) throws SQLException {
        if (!c.isValidDates()) {
            throw new IllegalArgumentException(
                    "Contrainte temporelle violée: date_plantation doit être < date_recolte_prevue");
        }

        verifierContrainteSurface(c.getParcelleId(), c.getSurfaceUtilisee(), -1);

        String req = "INSERT INTO culture (nom_culture, type_culture, saison, date_plantation, date_recolte_prevue, etat_culture, parcelle_id, surface_utilisee, rendement_estime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, c.getNomCulture());
        ps.setString(2, c.getTypeCulture());
        ps.setString(3, c.getSaison());
        ps.setDate(4, c.getDatePlantation());
        ps.setDate(5, c.getDateRecoltePrevue());
        ps.setString(6, c.getEtatCulture());
        ps.setInt(7, c.getParcelleId());
        ps.setDouble(8, c.getSurfaceUtilisee());
        ps.setDouble(9, c.getRendementEstime());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) return rs.getInt(1);
        return -1;
    }

    @Override
    public List<Culture> recuperer() throws SQLException {
        List<Culture> list = new ArrayList<>();
        String req = "SELECT c.*, p.surface as parcelle_surface, p.localisation as parcelle_localisation, u.nom as agriculteur_nom " +
                "FROM culture c " +
                "JOIN parcelle p ON c.parcelle_id = p.id " +
                "JOIN user u ON p.agriculteur_id = u.id " +
                "ORDER BY c.date_plantation DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapResultSetToCulture(rs, true));
        }
        return list;
    }

    public Culture recupererParId(int id) throws SQLException {
        String req = "SELECT c.*, p.surface as parcelle_surface, p.localisation as parcelle_localisation, u.nom as agriculteur_nom " +
                "FROM culture c " +
                "JOIN parcelle p ON c.parcelle_id = p.id " +
                "JOIN user u ON p.agriculteur_id = u.id " +
                "WHERE c.id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapResultSetToCulture(rs, true);
        return null;
    }

    public List<Culture> recupererParParcelle(int parcelleId) throws SQLException {
        List<Culture> list = new ArrayList<>();
        String req = "SELECT * FROM culture WHERE parcelle_id = ? ORDER BY date_plantation DESC";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, parcelleId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSetToCulture(rs, false));
        }
        return list;
    }

    public List<Culture> recupererParAgriculteur(int agriculteurId) throws SQLException {
        List<Culture> list = new ArrayList<>();
        String req = "SELECT c.*, p.surface as parcelle_surface, p.localisation as parcelle_localisation " +
                "FROM culture c " +
                "JOIN parcelle p ON c.parcelle_id = p.id " +
                "WHERE p.agriculteur_id = ? " +
                "ORDER BY c.date_plantation DESC";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, agriculteurId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSetToCulture(rs, true));
        }
        return list;
    }

    @Override
    public List<Culture> rechercher(String keyword) throws SQLException {
        List<Culture> list = new ArrayList<>();
        String req = "SELECT c.*, p.surface as parcelle_surface, p.localisation as parcelle_localisation, u.nom as agriculteur_nom " +
                "FROM culture c " +
                "JOIN parcelle p ON c.parcelle_id = p.id " +
                "JOIN user u ON p.agriculteur_id = u.id " +
                "WHERE LOWER(c.nom_culture) LIKE ? OR LOWER(c.type_culture) LIKE ? " +
                "ORDER BY c.date_plantation DESC";

        PreparedStatement ps = cnx.prepareStatement(req);
        String pattern = "%" + keyword.toLowerCase() + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSetToCulture(rs, true));
        }
        return list;
    }

    @Override
    public void modifier(Culture c) throws SQLException {
        if (!c.isValidDates()) {
            throw new IllegalArgumentException(
                    "Contrainte temporelle violée: date_plantation doit être < date_recolte_prevue");
        }

        // Vérifier contrainte surface pour la modification (exclure la culture courante)
        verifierContrainteSurface(c.getParcelleId(), c.getSurfaceUtilisee(), c.getId());

        String req = "UPDATE culture SET nom_culture=?, type_culture=?, saison=?, date_plantation=?, date_recolte_prevue=?, etat_culture=?, parcelle_id=?, surface_utilisee=?, rendement_estime=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, c.getNomCulture());
        ps.setString(2, c.getTypeCulture());
        ps.setString(3, c.getSaison());
        ps.setDate(4, c.getDatePlantation());
        ps.setDate(5, c.getDateRecoltePrevue());
        ps.setString(6, c.getEtatCulture());
        ps.setInt(7, c.getParcelleId());
        ps.setDouble(8, c.getSurfaceUtilisee());
        ps.setDouble(9, c.getRendementEstime());
        ps.setInt(10, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM culture WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // ==================== CONTRAINTE SURFACE ====================

    /**
     * Vérifie que la somme des surfaces des cultures de la parcelle
     * ne dépassera pas la surface totale de la parcelle.
     *
     * @param parcelleId    ID de la parcelle
     * @param nouvelleSurface Surface de la culture à ajouter/modifier
     * @param excludeCultureId ID de la culture à exclure du calcul (-1 si ajout nouveau)
     * @throws IllegalArgumentException si la contrainte est violée
     */
    public void verifierContrainteSurface(int parcelleId, double nouvelleSurface,
                                           int excludeCultureId) throws SQLException {
        if (nouvelleSurface <= 0) return; // Surface non renseignée, pas de contrainte

        // Récupérer la surface totale de la parcelle
        double surfaceParcelle = getSurfaceParcelle(parcelleId);
        if (surfaceParcelle <= 0) return;

        // Calculer la surface déjà utilisée (sans la culture courante)
        double surfaceDejaUtilisee = getSurfaceUtiliseeParParcelle(parcelleId, excludeCultureId);

        double surfaceTotaleApresAjout = surfaceDejaUtilisee + nouvelleSurface;

        if (surfaceTotaleApresAjout > surfaceParcelle + 0.001) { // tolérance float
            throw new IllegalArgumentException(String.format(
                    "⚠️ Contrainte surface violée: Parcelle=%.2f ha, Déjà utilisé=%.2f ha, Demandé=%.2f ha → Total=%.2f ha dépasse la surface totale !",
                    surfaceParcelle, surfaceDejaUtilisee, nouvelleSurface, surfaceTotaleApresAjout));
        }
    }

    /**
     * Retourne la surface totale utilisée par les cultures d'une parcelle.
     */
    public double getSurfaceUtiliseeParParcelle(int parcelleId) throws SQLException {
        return getSurfaceUtiliseeParParcelle(parcelleId, -1);
    }

    private double getSurfaceUtiliseeParParcelle(int parcelleId, int excludeCultureId) throws SQLException {
        String req = excludeCultureId > 0
                ? "SELECT COALESCE(SUM(surface_utilisee), 0) FROM culture WHERE parcelle_id = ? AND id != ?"
                : "SELECT COALESCE(SUM(surface_utilisee), 0) FROM culture WHERE parcelle_id = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, parcelleId);
        if (excludeCultureId > 0) ps.setInt(2, excludeCultureId);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getDouble(1);
        return 0;
    }

    private double getSurfaceParcelle(int parcelleId) throws SQLException {
        String req = "SELECT surface FROM parcelle WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, parcelleId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getDouble(1);
        return 0;
    }

    // ==================== STATISTIQUES ====================

    public boolean existe(int id) throws SQLException {
        String req = "SELECT 1 FROM culture WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public int compterCultures() {
        String req = "SELECT COUNT(*) FROM culture";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LogUtils.error(CultureService.class, "Erreur comptage cultures", e);
        }
        return 0;
    }

    public int compterCulturesParParcelle(int parcelleId) {
        String req = "SELECT COUNT(*) FROM culture WHERE parcelle_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, parcelleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LogUtils.error(CultureService.class, "Erreur comptage cultures parcelle", e);
        }
        return 0;
    }

    public List<Culture> getCulturesPretesARecolter(int agriculteurId) throws SQLException {
        List<Culture> list = new ArrayList<>();
        String req = "SELECT c.*, p.surface as parcelle_surface, p.localisation as parcelle_localisation " +
                "FROM culture c " +
                "JOIN parcelle p ON c.parcelle_id = p.id " +
                "WHERE p.agriculteur_id = ? AND c.date_recolte_prevue <= CURDATE() AND c.etat_culture != 'récoltée' " +
                "ORDER BY c.date_recolte_prevue ASC";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, agriculteurId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSetToCulture(rs, true));
        }
        return list;
    }

    public Map<String, Integer> getDistributionTypesCultures() {
        Map<String, Integer> stats = new HashMap<>();
        String req = "SELECT type_culture, COUNT(*) as total FROM culture GROUP BY type_culture ORDER BY total DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                stats.put(rs.getString("type_culture"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            LogUtils.error(CultureService.class, "Erreur stats types cultures", e);
        }
        return stats;
    }

    // ==================== MAPPING ====================

    private Culture mapResultSetToCulture(ResultSet rs, boolean includeJoinedNames) throws SQLException {
        Culture c = new Culture();
        c.setId(rs.getInt("id"));
        c.setNomCulture(rs.getString("nom_culture"));
        c.setTypeCulture(rs.getString("type_culture"));
        c.setSaison(rs.getString("saison"));
        c.setDatePlantation(rs.getDate("date_plantation"));
        c.setDateRecoltePrevue(rs.getDate("date_recolte_prevue"));
        c.setEtatCulture(rs.getString("etat_culture"));
        c.setParcelleId(rs.getInt("parcelle_id"));

        // Champs Premium (peuvent ne pas exister avant migration)
        try {
            c.setSurfaceUtilisee(rs.getDouble("surface_utilisee"));
        } catch (SQLException ignored) {}
        try {
            c.setRendementEstime(rs.getDouble("rendement_estime"));
        } catch (SQLException ignored) {}

        if (includeJoinedNames) {
            try {
                String loc = rs.getString("parcelle_localisation");
                double surface = rs.getDouble("parcelle_surface");
                String label = (loc != null && !loc.isBlank())
                        ? loc + " (" + surface + " ha)"
                        : "Parcelle #" + c.getParcelleId() + " (" + surface + " ha)";
                c.setNomParcelleTemp(label);
            } catch (SQLException ignored) {}
            try {
                c.setNomAgriculteurTemp(rs.getString("agriculteur_nom"));
            } catch (SQLException ignored) {}
        }
        return c;
    }
}
