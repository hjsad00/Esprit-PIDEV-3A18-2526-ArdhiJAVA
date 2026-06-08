package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.interfaces.marketplace.ICommandeService;
import tn.neuron.ardhi.interfaces.marketplace.IPanierService;
import tn.neuron.ardhi.interfaces.marketplace.IProduitService;
import tn.neuron.ardhi.models.marketplace.*;
import tn.neuron.ardhi.interfaces.marketplace.ICouponService;
import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.services.UserAndDiag.UserService;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandeService implements ICommandeService {

    private Connection connection;
    private IPanierService panierService;
    private IProduitService produitService;
    private MailService mailService;
    private UserService userService;
    private ICouponService couponService;
    private NotificationMarketService notificationService; // ✅ AJOUT

    public CommandeService() {
        this.connection = MyDatabase.getInstance().getCnx();
        this.panierService = new PanierService();
        this.produitService = new ProduitService();
        this.mailService = new MailService();
        this.userService = new UserService();
        this.couponService = new CouponService();
        this.notificationService = new NotificationMarketService(); // ✅ AJOUT
        checkAndAlterTable();
    }

    /** Ajoute les colonnes de livraison si elles n'existent pas encore */
    private void checkAndAlterTable() {
        try (Statement st = connection.createStatement()) {
            java.sql.DatabaseMetaData md = connection.getMetaData();

            // frais_livraison
            try (java.sql.ResultSet rs = md.getColumns(null, null, "commande", "frais_livraison")) {
                if (!rs.next()) {
                    st.executeUpdate("ALTER TABLE commande ADD COLUMN frais_livraison FLOAT DEFAULT 0.0");
                }
            }

            // mode_livraison
            try (java.sql.ResultSet rs = md.getColumns(null, null, "commande", "mode_livraison")) {
                if (!rs.next()) {
                    st.executeUpdate(
                            "ALTER TABLE commande ADD COLUMN mode_livraison ENUM('RECUPERATION','LIVRAISON') DEFAULT 'RECUPERATION'");
                }
            }
            try (java.sql.ResultSet rs = md.getColumns(null, null, "commande", "payee_par_points")) {
                if (!rs.next()) {
                    st.executeUpdate(
                            "ALTER TABLE commande ADD COLUMN payee_par_points TINYINT(1) DEFAULT 0");
                }
            }

        } catch (SQLException e) {
            System.err.println("[CommandeService] Erreur checkAndAlterTable : " + e.getMessage());
        }
    }

    // Met à jour les points de fidélité d'un utilisateur en base
    public boolean updateUserFidelityPoints(int userId, double points) {
        String query = "UPDATE user SET points_fidelite = ? WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setDouble(1, points);
            pst.setInt(2, userId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour des points de fidélité : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // PanierControllerRécupère les points de fidélité actuels d'un utilisateur
    // depuis la base
    public double getPointsFidelite(int userId) {
        String query = "SELECT points_fidelite FROM user WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("points_fidelite");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des points de fidélité : " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public boolean creerCommande(Commande commande) {
        String query = "INSERT INTO commande (dateCommande, etat, total, id_user, frais_livraison, mode_livraison, payee_par_points) VALUES (?, ?, ?, ?, ?, ?, ?)";        try {
            PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            LocalDate date = (commande.getDateCommande() != null)
                    ? commande.getDateCommande()
                    : LocalDate.now();
            pst.setDate(1, Date.valueOf(date));
            pst.setString(2, commande.getEtat().name());
            pst.setFloat(3, commande.getTotal());
            pst.setInt(4, commande.getIdUser());
            pst.setFloat(5, commande.getFraisLivraison());
            pst.setString(6, commande.getModeLivraison() != null
                    ? commande.getModeLivraison().name()
                    : ModeLivraison.RECUPERATION.name());
            pst.setBoolean(7, commande.isPayeeParPoints());
            int rows = pst.executeUpdate();
            if (rows > 0) {
                ResultSet rs = pst.getGeneratedKeys();
                if (rs.next()) {
                    commande.setIdCommande(rs.getInt(1));
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la création de la commande");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Commande> getCommandesByUser(int idUser) {
        List<Commande> commandes = new ArrayList<>();
        String query = "SELECT * FROM commande WHERE id_user = ? ORDER BY dateCommande DESC";

        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, idUser);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    commandes.add(extractCommandeFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des commandes : " + e.getMessage());
            e.printStackTrace();
        }

        return commandes;
    }

    @Override
    public List<Commande> getAllCommandes() {
        List<Commande> commandes = new ArrayList<>();
        String query = "SELECT * FROM commande ORDER BY dateCommande DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                commandes.add(extractCommandeFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de toutes les commandes : " + e.getMessage());
            e.printStackTrace();
        }

        return commandes;
    }

    @Override
    public List<Commande> getCommandesByStatut(EtatCommande statut) {
        List<Commande> commandes = new ArrayList<>();
        String query = "SELECT * FROM commande WHERE etat = ? ORDER BY dateCommande DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, statut.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    commandes.add(extractCommandeFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du filtrage par statut : " + e.getMessage());
            e.printStackTrace();
        }

        return commandes;
    }

    @Override
    public List<Commande> getCommandesByVendeur(int idVendeur) {
        List<Commande> commandes = new ArrayList<>();
        String query = "SELECT DISTINCT c.* " +
                "FROM commande c " +
                "JOIN detailscommande d ON d.id_commande = c.idCommande " +
                "JOIN produits p ON p.idProduit = d.id_produit " +
                "WHERE p.id_user = ? " +
                "ORDER BY c.dateCommande DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idVendeur);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    commandes.add(extractCommandeFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des commandes vendeur : " + e.getMessage());
            e.printStackTrace();
        }

        return commandes;
    }

    @Override
    public boolean updateStatutCommande(int idCommande, EtatCommande nouveauStatut) {
        boolean autoCommit = true;
        try {
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            Commande commandeExistante = getCommandeById(idCommande);
            if (commandeExistante == null) {
                return false;
            }

            if (nouveauStatut == EtatCommande.annulee) {

                // Réintégration du stock (inchangé)
                String queryStock = """
            UPDATE produits p
            JOIN detailscommande d ON p.idProduit = d.id_produit
            SET p.quantiteStock = p.quantiteStock + d.quantite
            WHERE d.id_commande = ?
        """;
                try {
                    PreparedStatement pst = connection.prepareStatement(queryStock);
                    pst.setInt(1, idCommande);
                    pst.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                // ✅ Ajustement des points de fidélité
                double soldeActuel = getPointsFidelite(commandeExistante.getIdUser());
                double nouveauSolde;

                if (commandeExistante.isPayeeParPoints()) {
                    // Commande payée par points → rembourser le total
                    nouveauSolde = soldeActuel + commandeExistante.getTotal();
                } else {
                    // Commande payée par carte → reprendre les 10% crédités à l'achat
                    double pointsARetirer = 0.1 * commandeExistante.getTotal();
                    nouveauSolde = Math.max(0, soldeActuel - pointsARetirer);
                }

                updateUserFidelityPoints(commandeExistante.getIdUser(), nouveauSolde);
            }

            String query = "UPDATE commande SET etat = ? WHERE idCommande = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, nouveauStatut.name());
                pstmt.setInt(2, idCommande);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    envoyerEmailNotification(commandeExistante, nouveauStatut);
                    return true;
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du statut : " + e.getMessage());
            e.printStackTrace();
            try {
                if (connection != null && !connection.getAutoCommit()) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Rollback échoué : " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(autoCommit);
                }
            } catch (SQLException e) {
                System.err.println("Restore autoCommit échoué : " + e.getMessage());
            }
        }
    }

    @Override
    public Commande getCommandeById(int idCommande) {
        String query = "SELECT * FROM commande WHERE idCommande = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idCommande);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractCommandeFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la commande : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean ajouterDetailsCommande(DetailsCommande detailsCommande) {
        String query = "INSERT INTO detailscommande (id_commande, id_produit, quantite, prixUnitaire) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, detailsCommande.getIdCommande());
            pstmt.setInt(2, detailsCommande.getIdProduit());
            pstmt.setInt(3, detailsCommande.getQuantite());
            pstmt.setFloat(4, detailsCommande.getPrixUnitaire());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout des détails de commande : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<DetailsCommande> getDetailsByCommande(int idCommande) {
        List<DetailsCommande> details = new ArrayList<>();
        String query = "SELECT * FROM detailscommande WHERE id_commande = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idCommande);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DetailsCommande detail = new DetailsCommande();
                    detail.setIdDetails(rs.getInt("idDetails"));
                    detail.setIdCommande(rs.getInt("id_commande"));
                    detail.setIdProduit(rs.getInt("id_produit"));
                    detail.setQuantite(rs.getInt("quantite"));
                    detail.setPrixUnitaire(rs.getFloat("prixUnitaire"));
                    details.add(detail);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des détails : " + e.getMessage());
            e.printStackTrace();
        }

        return details;
    }

    /**
     * Surcharge : permet de passer le mode de livraison et de calculer les frais.
     * 7 DT par vendeur distinct si mode == LIVRAISON.
     */
    public List<Commande> creerCommandeFromPanier(int idPanier, int idUser, String codeCoupon,
                                                  ModeLivraison modeLivraison, boolean payeeParPoints) {
        boolean autoCommit = true;
        List<Commande> commandesCreees = new ArrayList<>();

        try {
            List<PanierProduit> produitsPanier = panierService.getProduitsParPanier(idPanier);
            if (produitsPanier.isEmpty())
                return commandesCreees;

            Map<Integer, List<PanierProduit>> produitsParVendeur = new HashMap<>();
            double totalGlobal = 0.0;

            for (PanierProduit pp : produitsPanier) {
                int idVendeur = 0;
                float prixUnitaire = 0f;
                if (pp.getProduit() != null) {
                    idVendeur = pp.getProduit().getIdUser();
                    prixUnitaire = pp.getProduit().getPrix();
                } else {
                    Produit p = produitService.getProduitById(pp.getIdProduit());
                    if (p != null) {
                        idVendeur = p.getIdUser();
                        prixUnitaire = p.getPrix();
                    }
                }
                produitsParVendeur.computeIfAbsent(idVendeur, k -> new ArrayList<>()).add(pp);
                totalGlobal += (prixUnitaire * pp.getQuantite());
            }

            Coupon coupon = null;
            if (codeCoupon != null && !codeCoupon.isEmpty()) {
                coupon = couponService.findByCode(codeCoupon);
                if (coupon != null) {
                    String error = couponService.validerCoupon(coupon, totalGlobal, idUser);
                    if (error != null) {
                        System.err.println("Coupon ignoré: " + error);
                        coupon = null;
                    }
                }
            }

            // Frais de livraison : 7 DT par vendeur uniquement si LIVRAISON
            float fraisParVendeur = (modeLivraison == ModeLivraison.LIVRAISON) ? 7.0f : 0.0f;

            // ✅ AJOUT : récupérer le nom de l'acheteur une seule fois avant la boucle
            String nomAcheteur = getNomAcheteur(idUser);

            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            for (Map.Entry<Integer, List<PanierProduit>> entry : produitsParVendeur.entrySet()) {
                List<PanierProduit> produitsDuVendeur = entry.getValue();

                float totalVendeur = 0f;
                for (PanierProduit pp : produitsDuVendeur) {
                    float prixUnitaire = 0f;
                    if (pp.getProduit() != null) {
                        prixUnitaire = pp.getProduit().getPrix();
                    } else {
                        Produit p = produitService.getProduitById(pp.getIdProduit());
                        if (p != null)
                            prixUnitaire = p.getPrix();
                    }
                    totalVendeur += prixUnitaire * pp.getQuantite();
                }

                double reductionCommande = 0.0;
                if (coupon != null) {
                    if (coupon.getTypeReduction() == TypeReduction.POURCENTAGE) {
                        reductionCommande = totalVendeur * (coupon.getValeur() / 100.0);
                    } else if (totalGlobal > 0) {
                        double ratio = totalVendeur / totalGlobal;
                        reductionCommande = Math.min(coupon.getValeur(), totalGlobal) * ratio;
                    }
                }

                float totalFinal = (float) Math.max(0, totalVendeur - reductionCommande);

                Commande commande = new Commande();
                commande.setDateCommande(LocalDate.now());
                commande.setEtat(EtatCommande.en_attente);
                commande.setTotal(totalFinal + fraisParVendeur);
                commande.setFraisLivraison(fraisParVendeur);
                commande.setModeLivraison(modeLivraison);
                commande.setIdUser(idUser);
                commande.setPayeeParPoints(payeeParPoints);
                if (!creerCommande(commande)) {
                    connection.rollback();
                    return new ArrayList<>();
                }

                for (PanierProduit pp : produitsDuVendeur) {
                    float prixUnitaire = 0f;
                    if (pp.getProduit() != null) {
                        prixUnitaire = pp.getProduit().getPrix();
                    } else {
                        Produit p = produitService.getProduitById(pp.getIdProduit());
                        if (p != null)
                            prixUnitaire = p.getPrix();
                    }
                    DetailsCommande details = new DetailsCommande();
                    details.setIdCommande(commande.getIdCommande());
                    details.setIdProduit(pp.getIdProduit());
                    details.setQuantite(pp.getQuantite());
                    details.setPrixUnitaire(prixUnitaire);

                    if (!ajouterDetailsCommande(details)) {
                        connection.rollback();
                        return new ArrayList<>();
                    }
                    if (!produitService.diminuerStock(pp.getIdProduit(), pp.getQuantite())) {
                        connection.rollback();
                        return new ArrayList<>();
                    }
                }

                // ✅ AJOUT : notifier le vendeur après succès de sa sous-commande
                notificationService.notifierNouvelleCommande(
                        entry.getKey(),
                        commande.getIdCommande(),
                        produitsDuVendeur.get(0).getIdProduit(),
                        nomAcheteur,
                        commande.getTotal()
                );

                commandesCreees.add(commande);
            }

            if (coupon != null && !commandesCreees.isEmpty())
                couponService.incrementUsage(coupon, idUser);
            connection.commit();
            return commandesCreees;

        } catch (Exception e) {
            System.err.println("Erreur creerCommandeFromPanier : " + e.getMessage());
            e.printStackTrace();
            try {
                if (connection != null && !connection.getAutoCommit())
                    connection.rollback();
            } catch (SQLException ex) {
            }
            return new ArrayList<>();
        } finally {
            try {
                if (connection != null)
                    connection.setAutoCommit(autoCommit);
            } catch (SQLException e) {
            }
        }
    }

    @Override
    public List<Commande> creerCommandeFromPanier(int idPanier, int idUser, String codeCoupon) {
        boolean autoCommit = true;
        List<Commande> commandesCreees = new ArrayList<>();

        try {
            List<PanierProduit> produitsPanier = panierService.getProduitsParPanier(idPanier);

            if (produitsPanier.isEmpty()) {
                return commandesCreees;
            }

            Map<Integer, List<PanierProduit>> produitsParVendeur = new HashMap<>();
            double totalGlobal = 0.0;

            for (PanierProduit pp : produitsPanier) {
                int idVendeur = 0;
                float prixUnitaire = 0f;

                if (pp.getProduit() != null) {
                    idVendeur = pp.getProduit().getIdUser();
                    prixUnitaire = pp.getProduit().getPrix();
                } else {
                    Produit p = produitService.getProduitById(pp.getIdProduit());
                    if (p != null) {
                        idVendeur = p.getIdUser();
                        prixUnitaire = p.getPrix();
                    }
                }

                produitsParVendeur
                        .computeIfAbsent(idVendeur, k -> new ArrayList<>())
                        .add(pp);

                totalGlobal += (prixUnitaire * pp.getQuantite());
            }

            Coupon coupon = null;
            if (codeCoupon != null && !codeCoupon.isEmpty()) {
                coupon = couponService.findByCode(codeCoupon);
                if (coupon != null) {
                    String error = couponService.validerCoupon(coupon, totalGlobal, idUser);
                    if (error != null) {
                        System.err.println("Coupon ignoré (invalide à la commande): " + error);
                        coupon = null;
                    }
                }
            }

            // ✅ AJOUT : récupérer le nom de l'acheteur une seule fois avant la boucle
            String nomAcheteur = getNomAcheteur(idUser);

            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            for (Map.Entry<Integer, List<PanierProduit>> entry : produitsParVendeur.entrySet()) {
                List<PanierProduit> produitsDuVendeur = entry.getValue();

                float totalVendeur = 0f;
                for (PanierProduit pp : produitsDuVendeur) {
                    float prixUnitaire = 0f;
                    if (pp.getProduit() != null) {
                        prixUnitaire = pp.getProduit().getPrix();
                    } else {
                        Produit p = produitService.getProduitById(pp.getIdProduit());
                        if (p != null) {
                            prixUnitaire = p.getPrix();
                        }
                    }
                    totalVendeur += prixUnitaire * pp.getQuantite();
                }

                double reductionCommande = 0.0;
                if (coupon != null) {
                    if (coupon.getTypeReduction() == TypeReduction.POURCENTAGE) {
                        reductionCommande = totalVendeur * (coupon.getValeur() / 100.0);
                    } else {
                        if (totalGlobal > 0) {
                            double ratio = totalVendeur / totalGlobal;
                            double reductionTotalePossible = Math.min(coupon.getValeur(), totalGlobal);
                            reductionCommande = reductionTotalePossible * ratio;
                        }
                    }
                }

                float totalFinal = (float) Math.max(0, totalVendeur - reductionCommande);

                Commande commande = new Commande();
                commande.setDateCommande(LocalDate.now());
                commande.setEtat(EtatCommande.en_attente);
                commande.setTotal(totalFinal);
                commande.setIdUser(idUser);

                if (!creerCommande(commande)) {
                    connection.rollback();
                    return new ArrayList<>();
                }

                for (PanierProduit pp : produitsDuVendeur) {
                    float prixUnitaire = 0f;
                    if (pp.getProduit() != null) {
                        prixUnitaire = pp.getProduit().getPrix();
                    } else {
                        Produit p = produitService.getProduitById(pp.getIdProduit());
                        if (p != null) {
                            prixUnitaire = p.getPrix();
                        }
                    }

                    DetailsCommande details = new DetailsCommande();
                    details.setIdCommande(commande.getIdCommande());
                    details.setIdProduit(pp.getIdProduit());
                    details.setQuantite(pp.getQuantite());
                    details.setPrixUnitaire(prixUnitaire);

                    if (!ajouterDetailsCommande(details)) {
                        System.err.println("Échec insertion détail pour idProduit=" + pp.getIdProduit());
                        connection.rollback();
                        return new ArrayList<>();
                    }

                    if (!produitService.diminuerStock(pp.getIdProduit(), pp.getQuantite())) {
                        System.err.println("Échec diminution stock pour produit " + pp.getIdProduit());
                        connection.rollback();
                        return new ArrayList<>();
                    }
                }

                // ✅ AJOUT : notifier le vendeur après succès de sa sous-commande
                notificationService.notifierNouvelleCommande(
                        entry.getKey(),
                        commande.getIdCommande(),
                        produitsDuVendeur.get(0).getIdProduit(),
                        nomAcheteur,
                        commande.getTotal()
                );

                commandesCreees.add(commande);
            }

            if (coupon != null && !commandesCreees.isEmpty()) {
                couponService.incrementUsage(coupon, idUser);
            }

            connection.commit();
            return commandesCreees;

        } catch (Exception e) {
            System.err.println("Erreur lors de la création de commandes depuis panier : " + e.getMessage());
            e.printStackTrace();
            try {
                if (connection != null && !connection.getAutoCommit()) {
                    connection.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Rollback échoué : " + ex.getMessage());
            }
            return new ArrayList<>();
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(autoCommit);
                }
            } catch (SQLException e) {
                System.err.println("Restore autoCommit échoué : " + e.getMessage());
            }
        }
    }

    @Override
    public boolean deleteCommande(int idCommande) {
        return false;
    }

    private Commande extractCommandeFromResultSet(ResultSet rs) throws SQLException {
        Commande commande = new Commande();
        commande.setIdCommande(rs.getInt("idCommande"));
        java.sql.Date dateCommande = rs.getDate("dateCommande");
        commande.setDateCommande(dateCommande != null ? dateCommande.toLocalDate() : null);
        String etatStr = rs.getString("etat");
        commande.setEtat(etatStr != null ? EtatCommande.valueOf(etatStr) : null);
        commande.setTotal(rs.getFloat("total"));
        commande.setIdUser(rs.getInt("id_user"));

        // ✅ Ajout lecture livraison
        commande.setFraisLivraison(rs.getFloat("frais_livraison"));
        try {
            String modeLivraisonStr = rs.getString("mode_livraison");
            if (modeLivraisonStr != null)
                commande.setModeLivraison(ModeLivraison.valueOf(modeLivraisonStr));
        } catch (Exception ignored) {
        }
        try {
            commande.setPayeeParPoints(rs.getBoolean("payee_par_points"));
        } catch (Exception ignored) {}
        return commande;
    }

    // ✅ AJOUT : helper pour récupérer le nom de l'acheteur
    private String getNomAcheteur(int idUser) {
        String sql = "SELECT nom, prenom FROM user WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idUser);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getString("prenom") + " " + rs.getString("nom");
            }
        } catch (SQLException e) {
            System.err.println("[CommandeService] getNomAcheteur : " + e.getMessage());
        }
        return "Un client";
    }

    private void envoyerEmailNotification(Commande commande, EtatCommande nouveauStatut) {
        try {
            User user = userService.chercherParId(commande.getIdUser());
            if (user == null || user.getEmail() == null)
                return;

            // Récupérer mode livraison et frais depuis la commande
            String modeLivraisonLabel = (commande.getModeLivraison() == ModeLivraison.LIVRAISON)
                    ? "Livraison à domicile"
                    : "Récupération en magasin";
            double fraisLivraison = commande.getFraisLivraison();

            String subject = "";
            String body = "";

            if (nouveauStatut == EtatCommande.en_cours) {
                subject = "✅ Votre commande #" + commande.getIdCommande() + " est en cours de traitement";
                body = genererEmailEnCours(user, commande, modeLivraisonLabel, fraisLivraison);

            } else if (nouveauStatut == EtatCommande.annulee) {
                // Récupérer le solde actuel de points de fidélité
                double nouveauSolde = getPointsFidelite(commande.getIdUser());

                subject = "❌ Annulation de votre commande #" + commande.getIdCommande();
                body = genererEmailAnnulation(user, commande, modeLivraisonLabel, fraisLivraison, nouveauSolde);
            }

            if (!subject.isEmpty()) {
                mailService.sendEmail(user.getEmail(), subject, body, true);
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email de notification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String genererEmailEnCours(User user, Commande commande, String modeLivraisonLabel, double fraisLivraison) {
        List<DetailsCommande> details = getDetailsByCommande(commande.getIdCommande());

        StringBuilder produitsHtml = new StringBuilder();
        for (DetailsCommande d : details) {
            Produit p = produitService.getProduitById(d.getIdProduit());
            String nomProduit = (p != null) ? p.getNom() : "Produit inconnu";
            produitsHtml.append(
                    "                                        <tr>" +
                            "                                            <td style='padding: 12px; border-bottom: 1px solid #e0e0e0; color: #333333;'>"
                            + nomProduit + "</td>" +
                            "                                            <td style='padding: 12px; border-bottom: 1px solid #e0e0e0; text-align: center; color: #666666;'>x"
                            + d.getQuantite() + "</td>" +
                            "                                            <td style='padding: 12px; border-bottom: 1px solid #e0e0e0; text-align: right; color: #333333; font-weight: bold;'>"
                            +
                            String.format("%.2f DT", d.getPrixUnitaire() * d.getQuantite()) + "</td>" +
                            "                                        </tr>");
        }

        return "<!DOCTYPE html>" +
                "<html lang='fr'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f4f4f4; padding: 20px;'>"
                +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>"
                +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #2d7a4f 0%, #43a047 100%); padding: 40px 30px; text-align: center;'>"
                +
                "                            <img src='https://i.ibb.co/8nyxk57x/logo-ardhi.png' alt='Ardhi Logo' style='max-width: 150px; height: auto; margin-bottom: 10px; display: block; margin-left: auto; margin-right: auto;' />"
                +
                "                            <p style='margin: 10px 0 0 0; color: #e8f5e9; font-size: 14px;'>Votre marketplace de confiance</p>"
                +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 30px; text-align: center;'>" +
                "                            <table cellpadding='0' cellspacing='0' style='background-color: #e8f5e9; width: 80px; height: 80px; border-radius: 50%; margin: 0 auto;'>"
                +
                "                                <tr>" +
                "                                    <td style='text-align: center; vertical-align: middle; font-size: 48px; line-height: 80px;'>✅</td>"
                +
                "                                </tr>" +
                "                            </table>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 0 40px 30px 40px;'>" +
                "                            <h2 style='color: #2d7a4f; font-size: 24px; margin: 0 0 15px 0; text-align: center;'>Commande validée !</h2>"
                +
                "                            <p style='color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;'>"
                +
                "                                Bonjour <strong>" + user.getPrenom() + "</strong>,<br><br>" +
                "                                Nous avons le plaisir de vous informer que votre commande <strong>#"
                + commande.getIdCommande() + "</strong> " +
                "                                a été validée par le vendeur et est maintenant en cours de traitement."
                +
                "                            </p>" +
                "                            <div style='background-color: #fafafa; border-radius: 8px; padding: 20px; margin: 20px 0;'>"
                +
                "                                <h3 style='margin: 0 0 15px 0; color: #2d7a4f; font-size: 18px;'>📦 Détails de votre commande</h3>"
                +
                "                                <table width='100%' cellpadding='0' cellspacing='0' style='border-collapse: collapse;'>"
                +
                "                                    <thead>" +
                "                                        <tr style='background-color: #e8f5e9;'>" +
                "                                            <th style='padding: 12px; text-align: left; color: #2d7a4f; font-weight: bold;'>Produit</th>"
                +
                "                                            <th style='padding: 12px; text-align: center; color: #2d7a4f; font-weight: bold;'>Quantité</th>"
                +
                "                                            <th style='padding: 12px; text-align: right; color: #2d7a4f; font-weight: bold;'>Prix</th>"
                +
                "                                        </tr>" +
                "                                    </thead>" +
                "                                    <tbody>" +
                produitsHtml.toString() +
                "                                    </tbody>" +
                "                                    <tfoot>" +
                (commande.getTotal() < details.stream().mapToDouble(d -> d.getPrixUnitaire() * d.getQuantite()).sum()
                        - 0.01 ? "                                        <tr>" +
                        "                                            <td colspan='2' style='padding: 15px 12px 0 12px; text-align: right; color: #666666; font-size: 14px;'>Sous-total :</td>"
                        +
                        "                                            <td style='padding: 15px 12px 0 12px; text-align: right; color: #666666; font-size: 14px;'>"
                        +
                        String.format("%.2f DT",
                                details.stream().mapToDouble(d -> d.getPrixUnitaire() * d.getQuantite()).sum())
                        + "</td>" +
                        "                                        </tr>" +
                        "                                        <tr>" +
                        "                                            <td colspan='2' style='padding: 5px 12px 0 12px; text-align: right; color: #e74c3c; font-size: 14px;'>Réduction :</td>"
                        +
                        "                                            <td style='padding: 5px 12px 0 12px; text-align: right; color: #e74c3c; font-size: 14px;'>"
                        +
                        String.format("-%.2f DT",
                                details.stream().mapToDouble(d -> d.getPrixUnitaire() * d.getQuantite()).sum()
                                        - commande.getTotal())
                        + "</td>" +
                        "                                        </tr>" : "")
                +
                (fraisLivraison > 0 ? "                                        <tr>" +
                        "                                            <td colspan='2' style='padding: 5px 12px 0 12px; text-align: right; color: #2980b9; font-size: 14px;'>Frais de livraison :</td>"
                        +
                        "                                            <td style='padding: 5px 12px 0 12px; text-align: right; color: #2980b9; font-size: 14px;'>"
                        +
                        String.format("+%.2f DT", fraisLivraison) + "</td>" +
                        "                                        </tr>" : "")
                +
                "                                        <tr>" +
                "                                            <td colspan='2' style='padding: 15px 12px 0 12px; text-align: right; font-weight: bold; color: #2d7a4f; font-size: 18px;'>Total :</td>"
                +
                "                                            <td style='padding: 15px 12px 0 12px; text-align: right; font-weight: bold; color: #2d7a4f; font-size: 18px;'>"
                +
                String.format("%.2f DT", commande.getTotal()) + "</td>" +
                "                                        </tr>" +
                "                                    </tfoot>" + "                                </table>" +
                "                            </div>" +
                "                            <p style='background:#eef8ff;padding:12px;border-radius:6px;border-left:4px solid #2980b9; margin: 20px 0;'>"
                +
                "                                <b>🚚 Mode de livraison :</b> " + modeLivraisonLabel +
                "                            </p>" +
                "                            <div style='background-color: #e8f5e9; border-left: 4px solid #43a047; padding: 15px; margin: 20px 0;'>"
                +
                "                                <p style='margin: 0; color: #2d7a4f; font-size: 14px;'>" +
                "                                    <strong>💡 Bon à savoir :</strong> Vous recevrez un email dès que votre commande sera expédiée."
                +
                "                                </p>" +
                "                            </div>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='background-color: #f5f5f5; padding: 30px; text-align: center; border-top: 1px solid #e0e0e0;'>"
                +
                "                            <p style='margin: 0 0 10px 0; color: #666666; font-size: 14px;'>Merci de votre confiance,</p>"
                +
                "                            <p style='margin: 0 0 15px 0; color: #43a047; font-weight: bold; font-size: 16px;'>L'équipe Ardhi</p>"
                +
                "                            <p style='margin: 0; color: #999999; font-size: 12px;'>" +
                "                                Des questions ? Contactez-nous à <a href='mailto:contact@ardhi.com' style='color: #43a047;'>contact@ardhi.com</a>"
                +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                </table>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";
    }

    private String genererEmailAnnulation(User user, Commande commande, String modeLivraisonLabel,
                                          double fraisLivraison, double nouveauSolde) {
        List<DetailsCommande> details = getDetailsByCommande(commande.getIdCommande());

        StringBuilder produitsHtml = new StringBuilder();
        for (DetailsCommande d : details) {
            Produit p = produitService.getProduitById(d.getIdProduit());
            String nomProduit = (p != null) ? p.getNom() : "Produit inconnu";
            produitsHtml.append(
                    "                                        <tr>" +
                            "                                            <td style='padding: 12px; border-bottom: 1px solid #e0e0e0; color: #333333;'>"
                            + nomProduit + "</td>" +
                            "                                            <td style='padding: 12px; border-bottom: 1px solid #e0e0e0; text-align: center; color: #666666;'>x"
                            + d.getQuantite() + "</td>" +
                            "                                            <td style='padding: 12px; border-bottom: 1px solid #e0e0e0; text-align: right; color: #333333; font-weight: bold;'>"
                            +
                            String.format("%.2f DT", d.getPrixUnitaire() * d.getQuantite()) + "</td>" +
                            "                                        </tr>");
        }

        return "<!DOCTYPE html>" +
                "<html lang='fr'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #f4f4f4; padding: 20px;'>"
                +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>"
                +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #2d7a4f 0%, #43a047 100%); padding: 40px 30px; text-align: center;'>"
                +
                "                            <img src='https://i.ibb.co/8nyxk57x/logo-ardhi.png' alt='Ardhi Logo' style='max-width: 150px; height: auto; margin-bottom: 10px; display: block; margin-left: auto; margin-right: auto;' />"
                +
                "                            <p style='margin: 10px 0 0 0; color: #e8f5e9; font-size: 14px;'>Votre marketplace de confiance</p>"
                +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 30px; text-align: center;'>" +
                "                            <table cellpadding='0' cellspacing='0' style='background-color: #ffebee; width: 80px; height: 80px; border-radius: 50%; margin: 0 auto;'>"
                +
                "                                <tr>" +
                "                                    <td style='text-align: center; vertical-align: middle; font-size: 48px; line-height: 80px;'>❌</td>"
                +
                "                                </tr>" +
                "                            </table>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 0 40px 30px 40px;'>" +
                "                            <h2 style='color: #c62828; font-size: 24px; margin: 0 0 15px 0; text-align: center;'>Commande annulée</h2>"
                +
                "                            <p style='color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;'>"
                +
                "                                Bonjour <strong>" + user.getPrenom() + "</strong>,<br><br>" +
                "                                Nous sommes désolés de vous informer que votre commande <strong>#"
                + commande.getIdCommande() + "</strong> a été annulée." +
                "                            </p>" +
                "                            <div style='background-color: #fafafa; border-radius: 8px; padding: 20px; margin: 20px 0;'>"
                +
                "                                <h3 style='margin: 0 0 15px 0; color: #2d7a4f; font-size: 18px;'>📦 Détails de la commande</h3>"
                +
                "                                <table width='100%' cellpadding='0' cellspacing='0' style='border-collapse: collapse;'>"
                +
                "                                    <thead>" +
                "                                        <tr style='background-color: #e8f5e9;'>" +
                "                                            <th style='padding: 12px; text-align: left; color: #2d7a4f; font-weight: bold;'>Produit</th>"
                +
                "                                            <th style='padding: 12px; text-align: center; color: #2d7a4f; font-weight: bold;'>Quantité</th>"
                +
                "                                            <th style='padding: 12px; text-align: right; color: #2d7a4f; font-weight: bold;'>Prix</th>"
                +
                "                                        </tr>" +
                "                                    </thead>" +
                "                                    <tbody>" +
                produitsHtml.toString() +
                "                                    </tbody>" +
                "                                    <tfoot>" +
                (commande.getTotal() < details.stream().mapToDouble(d -> d.getPrixUnitaire() * d.getQuantite()).sum()
                        - 0.01 ? "                                        <tr>" +
                        "                                            <td colspan='2' style='padding: 15px 12px 0 12px; text-align: right; color: #666666; font-size: 14px;'>Sous-total :</td>"
                        +
                        "                                            <td style='padding: 15px 12px 0 12px; text-align: right; color: #666666; font-size: 14px;'>"
                        +
                        String.format("%.2f DT",
                                details.stream().mapToDouble(d -> d.getPrixUnitaire() * d.getQuantite()).sum())
                        + "</td>" +
                        "                                        </tr>" +
                        "                                        <tr>" +
                        "                                            <td colspan='2' style='padding: 5px 12px 0 12px; text-align: right; color: #e74c3c; font-size: 14px;'>Réduction :</td>"
                        +
                        "                                            <td style='padding: 5px 12px 0 12px; text-align: right; color: #e74c3c; font-size: 14px;'>"
                        +
                        String.format("-%.2f DT",
                                details.stream().mapToDouble(d -> d.getPrixUnitaire() * d.getQuantite()).sum()
                                        - commande.getTotal())
                        + "</td>" +
                        "                                        </tr>" : "")
                +
                (fraisLivraison > 0 ? "                                        <tr>" +
                        "                                            <td colspan='2' style='padding: 5px 12px 0 12px; text-align: right; color: #2980b9; font-size: 14px;'>Frais de livraison :</td>"
                        +
                        "                                            <td style='padding: 5px 12px 0 12px; text-align: right; color: #2980b9; font-size: 14px;'>"
                        +
                        String.format("+%.2f DT", fraisLivraison) + "</td>" +
                        "                                        </tr>" : "")
                +
                "                                        <tr>" +
                "                                            <td colspan='2' style='padding: 15px 12px 0 12px; text-align: right; font-weight: bold; color: #2d7a4f; font-size: 18px;'>Total :</td>"
                +
                "                                            <td style='padding: 15px 12px 0 12px; text-align: right; font-weight: bold; color: #2d7a4f; font-size: 18px;'>"
                +
                String.format("%.2f DT", commande.getTotal()) + "</td>" +
                "                                        </tr>" +
                "                                    </tfoot>" +
                "                                </table>" +
                "                            </div>" +
                "                            <p style='background:#eef8ff;padding:12px;border-radius:6px;border-left:4px solid #2980b9; margin: 20px 0;'>"
                +
                "                                <b>🚚 Mode de livraison :</b> " + modeLivraisonLabel +
                "                            </p>" +
                "                            <p style='background:#e6f5ee;padding:12px;border-radius:6px;border-left:4px solid #046436; margin: 20px 0;'>"
                +
                "                                🎁 <b>Votre solde de points de fidélité :</b> <b>"
                + String.format("%.2f pts", nouveauSolde) + "</b>" +
                "                            </p>" +
                "                            <div style='background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0;'>"
                +
                "                                <p style='margin: 0; color: #e65100; font-size: 14px;'>" +
                "                                    <strong>ℹ️ Besoin d'aide ?</strong> N'hésitez pas à nous contacter pour plus d'informations sur cette annulation."
                +
                "                                </p>" +
                "                            </div>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='background-color: #f5f5f5; padding: 30px; text-align: center; border-top: 1px solid #e0e0e0;'>"
                +
                "                            <p style='margin: 0 0 10px 0; color: #666666; font-size: 14px;'>Cordialement,</p>"
                +
                "                            <p style='margin: 0 0 15px 0; color: #43a047; font-weight: bold; font-size: 16px;'>L'équipe Ardhi</p>"
                +
                "                            <p style='margin: 0; color: #999999; font-size: 12px;'>" +
                "                                Des questions ? Contactez-nous à <a href='mailto:contact@ardhi.com' style='color: #43a047;'>contact@ardhi.com</a>"
                +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                </table>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";
    }

    @Override
    public boolean updateCommande(Commande commande) {
        String query = "UPDATE commande SET dateCommande = ?, etat = ?, total = ?, id_user = ? WHERE idCommande = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            java.sql.Date date = (commande.getDateCommande() != null)
                    ? java.sql.Date.valueOf(commande.getDateCommande())
                    : null;
            pstmt.setDate(1, date);
            pstmt.setString(2, commande.getEtat().name());
            pstmt.setFloat(3, commande.getTotal());
            pstmt.setInt(4, commande.getIdUser());
            pstmt.setInt(5, commande.getIdCommande());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de la commande : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Produit> getDerniersAchatsProduits(int idUser, int limit) {
        List<Produit> produits = new ArrayList<>();
        String query = "SELECT p.* FROM produits p " +
                "JOIN detailscommande dc ON p.idProduit = dc.id_produit " +
                "JOIN commande c ON dc.id_commande = c.idCommande " +
                "WHERE c.id_user = ? " +
                "GROUP BY p.idProduit " +
                "ORDER BY MAX(c.dateCommande) DESC " +
                "LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idUser);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(extractProduitFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getDerniersAchatsProduits : " + e.getMessage());
        }
        return produits;
    }

    private Produit extractProduitFromResultSet(ResultSet rs) throws SQLException {
        tn.neuron.ardhi.models.marketplace.UniteMesure unite = null;
        try {
            String uniteStr = rs.getString("uniteMesure");
            if (uniteStr != null)
                unite = tn.neuron.ardhi.models.marketplace.UniteMesure.valueOf(uniteStr);
        } catch (Exception ignored) {
        }

        Produit p = new Produit(
                rs.getInt("idProduit"),
                rs.getString("nom"),
                rs.getString("description"),
                rs.getFloat("prix"),
                rs.getInt("quantiteStock"),
                rs.getString("categorie"),
                rs.getInt("id_user"),
                unite,
                rs.getString("image"));

        try {
            String typeRemiseStr = rs.getString("typeRemise");
            if (typeRemiseStr != null)
                p.setTypeRemise(tn.neuron.ardhi.models.marketplace.TypeReduction.valueOf(typeRemiseStr));
            p.setRemise(rs.getFloat("remise"));
        } catch (Exception ignored) {
        }

        return p;
    }
}

