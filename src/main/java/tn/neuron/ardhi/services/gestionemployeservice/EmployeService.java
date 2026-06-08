package tn.neuron.ardhi.services.gestionemployeservice;

import tn.neuron.ardhi.models.gestionemployemodel.Employe;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;
import tn.neuron.ardhi.utils.gestionemployeutils.AgriculteurContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeService {

    /**
     * Toujours récupérer la connexion fraîche via MyDatabase.
     * NE PAS stocker la connexion dans un champ → évite "connection closed"
     */
    private Connection getCnx() {
        return MyDatabase.getInstance().getCnx();
    }

    // ── CRUD principal ────────────────────────────────────────────────────────

    /**
     * Créer un nouvel employé.
     * Assigne automatiquement id_agriculteur depuis AgriculteurContext.
     */
    public boolean createEmploye(Employe employe) {
        // 🔍 DEBUG: Vérifier le contexte agriculteur
        Integer idAgriculteur = AgriculteurContext.getActiveAgriculteurId();

        System.out.println("=== DEBUG createEmploye ===");
        System.out.println("ID Agriculteur récupéré: " + idAgriculteur);
        System.out.println("Employé: " + employe.getNom() + " " + employe.getPrenom());
        System.out.println("==========================");

        // ⚠️ VALIDATION: L'ID agriculteur est obligatoire
        if (idAgriculteur == null || idAgriculteur == 0) {
            System.err.println("❌ ERREUR: Impossible de créer un employé sans ID agriculteur !");
            System.err.println("   Le contexte AgriculteurContext n'est pas défini correctement.");
            System.err.println("   L'admin doit ouvrir cette page via la supervision d'un agriculteur.");
            return false;
        }

        employe.setIdAgriculteur(idAgriculteur);

        String query = "INSERT INTO employe (nom, prenom, email, poste, telephone, actif, id_agriculteur, photo_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getCnx().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, employe.getNom());
            stmt.setString(2, employe.getPrenom());
            stmt.setString(3, employe.getEmail());
            stmt.setString(4, employe.getPoste());
            stmt.setString(5, employe.getTelephone());
            stmt.setBoolean(6, employe.isActif());
            stmt.setInt(7, idAgriculteur);
            if (employe.hasPhoto()) stmt.setString(8, employe.getPhotoPath());
            else stmt.setNull(8, java.sql.Types.VARCHAR);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        employe.setId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("✅ Employé créé ! ID: " + employe.getId() + " | Agriculteur: " + idAgriculteur);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL création employé:");
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   Code erreur: " + e.getErrorCode());
            System.err.println("   État SQL: " + e.getSQLState());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupérer les employés filtrés par agriculteur actif.
     * - Agriculteur connecté → ses propres employés (via UserSession)
     * - Admin en supervision → employés de l'agriculteur supervisé
     */
    public List<Employe> getAllEmployes() {
        List<Employe> employes = new ArrayList<>();
        Integer idAgriculteur = AgriculteurContext.getActiveAgriculteurId();

        String query;
        if (idAgriculteur != null) {
            query = "SELECT * FROM employe WHERE id_agriculteur = " + idAgriculteur + " ORDER BY nom, prenom";
        } else {
            query = "SELECT * FROM employe ORDER BY nom, prenom"; // Fallback admin sans contexte
        }

        try (Statement stmt = getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                employes.add(mapResultSetToEmploye(rs));
            }
            System.out.println("✅ Employés récupérés: " + employes.size() + " (agriculteur=" + idAgriculteur + ")");

        } catch (SQLException e) {
            System.err.println("❌ Erreur récupération employés: " + e.getMessage());
            e.printStackTrace();
        }
        return employes;
    }

    /**
     * Récupérer un employé par ID
     */
    public Employe getEmployeById(int id) {
        String query = "SELECT * FROM employe WHERE id_employe = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return mapResultSetToEmploye(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getEmployeById: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Mettre à jour un employé (conserve id_agriculteur existant)
     */
    public boolean updateEmploye(Employe employe) {
        String query = "UPDATE employe SET nom=?, prenom=?, email=?, poste=?, telephone=?, actif=?, photo_path=? " +
                "WHERE id_employe=?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setString(1, employe.getNom());
            stmt.setString(2, employe.getPrenom());
            stmt.setString(3, employe.getEmail());
            stmt.setString(4, employe.getPoste());
            stmt.setString(5, employe.getTelephone());
            stmt.setBoolean(6, employe.isActif());
            if (employe.hasPhoto()) stmt.setString(7, employe.getPhotoPath());
            else stmt.setNull(7, java.sql.Types.VARCHAR);
            stmt.setInt(8, employe.getId());

            int rows = stmt.executeUpdate();
            System.out.println("✅ Employé modifié. Lignes: " + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur updateEmploye: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Supprimer un employé
     */
    public boolean deleteEmploye(int id) {
        String query = "DELETE FROM employe WHERE id_employe=?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            System.out.println("✅ Employé supprimé. Lignes: " + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("❌ Erreur deleteEmploye: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Rechercher des employés (dans le contexte agriculteur actif)
     */
    public List<Employe> searchEmployes(String recherche) {
        List<Employe> employes = new ArrayList<>();
        Integer idAgriculteur = AgriculteurContext.getActiveAgriculteurId();

        String query;
        String pattern = "%" + recherche + "%";

        if (idAgriculteur != null) {
            query = "SELECT * FROM employe WHERE id_agriculteur = ? " +
                    "AND (nom LIKE ? OR prenom LIKE ? OR email LIKE ?) ORDER BY nom, prenom";
            try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
                stmt.setInt(1, idAgriculteur);
                stmt.setString(2, pattern);
                stmt.setString(3, pattern);
                stmt.setString(4, pattern);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next())
                        employes.add(mapResultSetToEmploye(rs));
                }
            } catch (SQLException e) {
                System.err.println("❌ Erreur searchEmployes: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            query = "SELECT * FROM employe WHERE nom LIKE ? OR prenom LIKE ? OR email LIKE ? ORDER BY nom, prenom";
            try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
                stmt.setString(1, pattern);
                stmt.setString(2, pattern);
                stmt.setString(3, pattern);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next())
                        employes.add(mapResultSetToEmploye(rs));
                }
            } catch (SQLException e) {
                System.err.println("❌ Erreur searchEmployes: " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("🔍 Résultats recherche: " + employes.size());
        return employes;
    }

    /**
     * Vérifier si un email existe déjà
     */
    public boolean emailExists(String email, int excludeId) {
        String query = "SELECT COUNT(*) FROM employe WHERE email = ? AND id_employe != ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setString(1, email);
            stmt.setInt(2, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur emailExists: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ── Méthodes pour AdminAgriculteursController ─────────────────────────────

    public List<Employe> getEmployesByAgriculteur(Integer idAgriculteur) {
        List<Employe> employes = new ArrayList<>();
        String query = "SELECT * FROM employe WHERE id_agriculteur = ? ORDER BY nom, prenom";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idAgriculteur);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    employes.add(mapResultSetToEmploye(rs));
            }
            System.out.println("✅ " + employes.size() + " employé(s) pour agriculteur " + idAgriculteur);
        } catch (SQLException e) {
            System.err.println("❌ getEmployesByAgriculteur: " + e.getMessage());
            e.printStackTrace();
        }
        return employes;
    }

    public int countByAgriculteur(Integer idAgriculteur) {
        String query = "SELECT COUNT(*) FROM employe WHERE id_agriculteur = ?";
        try (PreparedStatement stmt = getCnx().prepareStatement(query)) {
            stmt.setInt(1, idAgriculteur);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ countByAgriculteur (employe): " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private Employe mapResultSetToEmploye(ResultSet rs) throws SQLException {
        Employe employe = new Employe();
        employe.setId(rs.getInt("id_employe"));
        employe.setNom(rs.getString("nom"));
        employe.setPrenom(rs.getString("prenom"));
        employe.setEmail(rs.getString("email"));
        employe.setPoste(rs.getString("poste"));
        employe.setTelephone(rs.getString("telephone"));
        employe.setActif(rs.getBoolean("actif"));
        employe.setIdAgriculteur(rs.getObject("id_agriculteur", Integer.class));
        try { employe.setPhotoPath(rs.getString("photo_path")); } catch (Exception ignored) {}
        return employe;
    }
}