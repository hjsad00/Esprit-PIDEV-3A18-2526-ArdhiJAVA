package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.interfaces.marketplace.IProduitService;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.models.marketplace.UniteMesure;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService implements IProduitService {

    private Connection connection;

    // ── Services injectés pour les notifications ──────────────────────────────
    private final WishlistService wishlistService;
    private final WhatsAppService whatsAppService;

    public ProduitService() {
        this.connection      = MyDatabase.getInstance().getCnx();
        this.wishlistService = new WishlistService();
        this.whatsAppService = new WhatsAppService();
    }

    // ── getAllProduits ─────────────────────────────────────────────────────────
    @Override
    public List<Produit> getAllProduits() {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT * FROM produits WHERE quantiteStock > 0 ORDER BY nom";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                produits.add(extractProduitFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des produits : " + e.getMessage());
            e.printStackTrace();
        }

        return produits;
    }

    // ── searchProduits ────────────────────────────────────────────────────────
    @Override
    public List<Produit> searchProduits(String searchTerm) {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT * FROM produits WHERE (nom LIKE ? OR categorie LIKE ?) AND quantiteStock > 0";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(extractProduitFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche : " + e.getMessage());
            e.printStackTrace();
        }

        return produits;
    }

    // ── getProduitById ────────────────────────────────────────────────────────
    @Override
    public Produit getProduitById(int idProduit) {
        String query = "SELECT * FROM produits WHERE idProduit = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idProduit);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractProduitFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du produit : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // ── getProduitsByCategorie ────────────────────────────────────────────────
    @Override
    public List<Produit> getProduitsByCategorie(String categorie) {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT * FROM produits WHERE categorie = ? AND quantiteStock > 0 ORDER BY nom";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, categorie);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(extractProduitFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du filtrage par catégorie : " + e.getMessage());
            e.printStackTrace();
        }

        return produits;
    }

    // ── getAllCategories ───────────────────────────────────────────────────────
    @Override
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String query = "SELECT DISTINCT categorie FROM produits WHERE categorie IS NOT NULL ORDER BY categorie";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                categories.add(rs.getString("categorie"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des catégories : " + e.getMessage());
            e.printStackTrace();
        }

        return categories;
    }

    // ── verifierStock ─────────────────────────────────────────────────────────
    @Override
    public boolean verifierStock(int idProduit, int quantiteDemandee) {
        String query = "SELECT quantiteStock FROM produits WHERE idProduit = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idProduit);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int stockDisponible = rs.getInt("quantiteStock");
                    return stockDisponible >= quantiteDemandee;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification du stock : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ── diminuerStock ─────────────────────────────────────────────────────────
    @Override
    public boolean diminuerStock(int idProduit, int quantite) {
        String query = "UPDATE produits SET quantiteStock = quantiteStock - ? WHERE idProduit = ? AND quantiteStock >= ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, quantite);
            pstmt.setInt(2, idProduit);
            pstmt.setInt(3, quantite);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la diminution du stock : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ── augmenterStock ────────────────────────────────────────────────────────
    @Override
    public boolean augmenterStock(int idProduit, int quantite) {
        String query = "UPDATE produits SET quantiteStock = quantiteStock + ? WHERE idProduit = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, quantite);
            pstmt.setInt(2, idProduit);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'augmentation du stock : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ── ajouterProduit ────────────────────────────────────────────────────────
    @Override
    public boolean ajouterProduit(Produit produit) {
        String query = "INSERT INTO produits " +
                "(nom, description, prix, quantiteStock, categorie, id_user, uniteMesure, image, remise, typeRemise) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, produit.getNom());
            pstmt.setString(2, produit.getDescription());
            pstmt.setFloat(3, produit.getPrix());
            pstmt.setInt(4, produit.getQuantiteStock());
            pstmt.setString(5, produit.getCategorie());
            pstmt.setInt(6, produit.getIdUser());
            pstmt.setString(7, produit.getUniteMesure().name());
            pstmt.setString(8, produit.getImage());
            pstmt.setFloat(9, produit.getRemise());
            if (produit.getTypeRemise() != null) {
                pstmt.setString(10, produit.getTypeRemise().name());
            } else {
                pstmt.setNull(10, Types.VARCHAR);
            }
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du produit : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ── modifierProduit ── [MODIFIÉ] Ajout détection + notifications ──────────
    @Override
    public boolean modifierProduit(Produit nouveauProduit) {

        // ── ÉTAPE 1 : Récupérer l'état AVANT modification ─────────────────────
        Produit ancienProduit = getProduitById(nouveauProduit.getIdProduit());

        // ── ÉTAPE 2 : Appliquer la modification en base ───────────────────────
        String query = "UPDATE produits SET nom = ?, description = ?, prix = ?, quantiteStock = ?, " +
                "categorie = ?, uniteMesure = ?, image = ?, remise = ?, typeRemise = ? " +
                "WHERE idProduit = ?";

        boolean success = false;
        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, nouveauProduit.getNom());
            pstmt.setString(2, nouveauProduit.getDescription());
            pstmt.setFloat(3, nouveauProduit.getPrix());
            pstmt.setInt(4, nouveauProduit.getQuantiteStock());
            pstmt.setString(5, nouveauProduit.getCategorie());
            pstmt.setString(6, nouveauProduit.getUniteMesure().name());
            pstmt.setString(7, nouveauProduit.getImage());
            pstmt.setFloat(8, nouveauProduit.getRemise());
            if (nouveauProduit.getTypeRemise() != null) {
                pstmt.setString(9, nouveauProduit.getTypeRemise().name());
            } else {
                pstmt.setNull(9, Types.VARCHAR);
            }
            pstmt.setInt(10, nouveauProduit.getIdProduit());

            success = pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification du produit : " + e.getMessage());
            e.printStackTrace();
        }

        // ── ÉTAPE 3 : Analyser les changements et envoyer les notifications ───
        if (success && ancienProduit != null) {
            envoyerNotificationsWishlist(ancienProduit, nouveauProduit);
        }

        return success;
    }

    // ── Logique de détection et d'envoi des notifications ────────────────────

    /**
     * Compare l'ancien et le nouveau produit selon 3 critères,
     * puis notifie par WhatsApp tous les utilisateurs ayant ce produit en favoris.
     *
     * Critères :
     *   1. Baisse de prix      → nouveau prix < ancien prix
     *   2. Nouvelle promotion  → nouvelle remise > ancienne remise
     *   3. Retour en stock     → ancien stock == 0 ET nouveau stock > 0
     */
    private void envoyerNotificationsWishlist(Produit ancien, Produit nouveau) {
        String messageNotif = construireMessage(ancien, nouveau);

        // Aucun critère déclenché → pas de notification
        if (messageNotif == null) {
            System.out.println("[ProduitService] Aucun critère de notification déclenché pour "
                    + nouveau.getNom());
            return;
        }

        // Récupérer les utilisateurs intéressés (wishlist + téléphone valide)
        List<User> usersInteresses = wishlistService.getInterestedUsers(nouveau.getIdProduit());

        if (usersInteresses.isEmpty()) {
            System.out.println("[ProduitService] Aucun utilisateur à notifier pour le produit #"
                    + nouveau.getIdProduit());
            return;
        }

        System.out.println("[ProduitService] 📲 Envoi de notifications à "
                + usersInteresses.size() + " utilisateur(s)...");

        // Envoyer le message à chaque utilisateur
        for (User user : usersInteresses) {
            boolean envoye = whatsAppService.sendWhatsApp(user.getPhone(), messageNotif);
            if (envoye) {
                System.out.println("  ✅ Notifié : " + user.getPrenom() + " " + user.getNom()
                        + " (" + user.getPhone() + ")");
            } else {
                System.err.println("  ❌ Échec notification : " + user.getPrenom() + " " + user.getNom());
            }
        }
    }

    /**
     * Construit le message WhatsApp adapté selon le type de changement détecté.
     *
     * @return Le message à envoyer, ou null si aucun critère n'est satisfait.
     */
    private String construireMessage(Produit ancien, Produit nouveau) {
        String nomProduit = nouveau.getNom();

        // ── Critère 1 : Baisse de prix ────────────────────────────────────────
        if (nouveau.getPrix() < ancien.getPrix()) {
            return String.format(
                    "🌍 Ardhi Marketplace : Bonne nouvelle ! Le produit *%s* que vous suivez a baissé de prix. " +
                            "Il est maintenant à *%.2f DT* (au lieu de %.2f DT) ! Profitez-en. 🛒",
                    nomProduit, nouveau.getPrix(), ancien.getPrix()
            );
        }

        // ── Critère 2 : Nouvelle promotion (remise augmentée) ─────────────────
        if (nouveau.getRemise() > ancien.getRemise()) {
            return String.format(
                    "🌍 Ardhi Marketplace : 🎉 Nouvelle promotion sur *%s* ! " +
                            "Une remise de *%.0f%s* est maintenant disponible. Dépêchez-vous !",
                    nomProduit,
                    nouveau.getRemise(),
                    (nouveau.getTypeRemise() != null &&
                            nouveau.getTypeRemise().name().equals("POURCENTAGE")) ? "%" : " DT"
            );
        }

        // ── Critère 3 : Retour en stock ────────────────────────────────────────
        if (ancien.getQuantiteStock() == 0 && nouveau.getQuantiteStock() > 0) {
            return String.format(
                    "🌍 Ardhi Marketplace : ✅ Bonne nouvelle ! Le produit *%s* que vous attendiez " +
                            "est de nouveau disponible en stock (%d unités). Commandez maintenant !",
                    nomProduit, nouveau.getQuantiteStock()
            );
        }

        // Aucun critère déclenché
        return null;
    }

    // ── supprimerProduit ──────────────────────────────────────────────────────
    @Override
    public boolean supprimerProduit(int idProduit) {
        String query = "DELETE FROM produits WHERE idProduit = ?";

        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, idProduit);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du produit : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ── getProduitsByUser ─────────────────────────────────────────────────────
    @Override
    public List<Produit> getProduitsByUser(int idUser) {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT * FROM produits WHERE id_user = ? ORDER BY nom";

        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, idUser);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(extractProduitFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des produits de l'utilisateur : " + e.getMessage());
            e.printStackTrace();
        }

        return produits;
    }

    // ── getNouveauxProduits ───────────────────────────────────────────────────
    public List<Produit> getNouveauxProduits(int currentUserId, int limit) {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT * FROM produits WHERE quantiteStock > 0 AND id_user != ? ORDER BY idProduit DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, currentUserId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(extractProduitFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getNouveauxProduits : " + e.getMessage());
        }
        return produits;
    }

    // ── extractProduitFromResultSet ───────────────────────────────────────────
    private Produit extractProduitFromResultSet(ResultSet rs) throws SQLException {
        Produit p = new Produit(
                rs.getInt("idProduit"),
                rs.getString("nom"),
                rs.getString("description"),
                rs.getFloat("prix"),
                rs.getInt("quantiteStock"),
                rs.getString("categorie"),
                rs.getInt("id_user"),
                UniteMesure.valueOf(rs.getString("uniteMesure")),
                rs.getString("image"));

        p.setRemise(rs.getFloat("remise"));
        String typeRemiseStr = rs.getString("typeRemise");
        if (typeRemiseStr != null && !typeRemiseStr.isEmpty()) {
            try {
                p.setTypeRemise(TypeReduction.valueOf(typeRemiseStr));
            } catch (IllegalArgumentException ignored) {
                p.setTypeRemise(null);
            }
        }

        return p;
    }
}