package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.interfaces.marketplace.IWishlistService;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.models.marketplace.UniteMesure;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.UserAndDiag.Role;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion de la wishlist (favoris) utilisateur.
 */
public class WishlistService implements IWishlistService {

    private final Connection connection;

    public WishlistService() {
        this.connection = MyDatabase.getInstance().getCnx();
        creerTableSiAbsente();
    }

    // ── Création automatique de la table si elle n'existe pas ─────────────────
    private void creerTableSiAbsente() {
        String sql = "CREATE TABLE IF NOT EXISTS wishlist (" +
                "idWishlist  INT          AUTO_INCREMENT PRIMARY KEY, " +
                "id_user     INT          NOT NULL, " +
                "id_produit  INT          NOT NULL, " +
                "dateAjout   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY uq_wishlist_user_produit (id_user, id_produit), " +
                "CONSTRAINT fk_wish_user    FOREIGN KEY (id_user)    REFERENCES user(id)              ON DELETE CASCADE, " +
                "CONSTRAINT fk_wish_produit FOREIGN KEY (id_produit) REFERENCES produits(idProduit)   ON DELETE CASCADE" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("[WishlistService] Erreur création table wishlist : " + e.getMessage());
        }
    }

    // ── Ajouter favori ────────────────────────────────────────────────────────
    @Override
    public boolean ajouterFavori(int idUser, int idProduit) {
        String sql = "INSERT IGNORE INTO wishlist (id_user, id_produit) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, idUser);
            pstmt.setInt(2, idProduit);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[WishlistService] ajouterFavori : " + e.getMessage());
            return false;
        }
    }

    // ── Supprimer favori ──────────────────────────────────────────────────────
    @Override
    public boolean supprimerFavori(int idUser, int idProduit) {
        String sql = "DELETE FROM wishlist WHERE id_user = ? AND id_produit = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, idUser);
            pstmt.setInt(2, idProduit);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[WishlistService] supprimerFavori : " + e.getMessage());
            return false;
        }
    }

    // ── Vérifier si favori ────────────────────────────────────────────────────
    @Override
    public boolean estFavori(int idUser, int idProduit) {
        String sql = "SELECT COUNT(*) FROM wishlist WHERE id_user = ? AND id_produit = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, idUser);
            pstmt.setInt(2, idProduit);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[WishlistService] estFavori : " + e.getMessage());
            return false;
        }
    }

    // ── Récupérer les favoris (avec jointure produit) ─────────────────────────
    @Override
    public List<Produit> getFavoris(int idUser) {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT p.*, w.dateAjout " +
                "FROM wishlist w " +
                "JOIN produits p ON w.id_produit = p.idProduit " +
                "WHERE w.id_user = ? " +
                "ORDER BY w.dateAjout DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, idUser);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                produits.add(extractProduit(rs));
            }
        } catch (SQLException e) {
            System.err.println("[WishlistService] getFavoris : " + e.getMessage());
        }
        return produits;
    }

    // ── [NOUVEAU] Récupérer les utilisateurs intéressés par un produit ─────────

    /**
     * Retourne la liste des utilisateurs ayant ce produit en favoris
     * ET possédant un numéro de téléphone valide (pour l'envoi WhatsApp).
     *
     * @param idProduit Identifiant du produit modifié
     * @return Liste des utilisateurs à notifier
     */
    public List<User> getInterestedUsers(int idProduit) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.* FROM user u " +
                "JOIN wishlist w ON u.id = w.id_user " +
                "WHERE w.id_produit = ? " +
                "AND u.phone IS NOT NULL " +
                "AND TRIM(u.phone) != ''";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, idProduit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                User u = mapRowToUser(rs);
                users.add(u);
            }

            System.out.println("[WishlistService] " + users.size()
                    + " utilisateur(s) intéressé(s) trouvé(s) pour le produit #" + idProduit);

        } catch (SQLException e) {
            System.err.println("[WishlistService] getInterestedUsers : " + e.getMessage());
        }

        return users;
    }

    // ── Extraction Produit depuis ResultSet ───────────────────────────────────
    private Produit extractProduit(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setIdProduit(rs.getInt("idProduit"));
        p.setNom(rs.getString("nom"));
        p.setDescription(rs.getString("description"));
        p.setPrix(rs.getFloat("prix"));
        p.setQuantiteStock(rs.getInt("quantiteStock"));
        p.setCategorie(rs.getString("categorie"));
        p.setIdUser(rs.getInt("id_user"));
        p.setImage(rs.getString("image"));

        // Remise
        p.setRemise(rs.getFloat("remise"));
        String typeRemise = rs.getString("typeRemise");
        if (typeRemise != null) {
            try {
                p.setTypeRemise(TypeReduction.valueOf(typeRemise));
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Unité de mesure
        String unite = rs.getString("uniteMesure");
        if (unite != null) {
            try {
                p.setUniteMesure(UniteMesure.valueOf(unite));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return p;
    }

    // ── Mapping ResultSet → User ──────────────────────────────────────────────
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        try {
            u.setRole(Role.valueOf(rs.getString("role")));
        } catch (IllegalArgumentException | NullPointerException e) {
            u.setRole(Role.CLIENT);
        }
        try { u.setPhone(rs.getString("phone")); }
        catch (SQLException ignored) { u.setPhone(null); }
        try { u.setLocation(rs.getString("location")); }
        catch (SQLException ignored) { u.setLocation(null); }
        return u;
    }
}