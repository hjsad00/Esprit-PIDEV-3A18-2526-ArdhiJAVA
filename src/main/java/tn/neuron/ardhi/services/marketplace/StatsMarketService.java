package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.models.marketplace.UniteMesure;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class StatsMarketService {

    private final Connection connection;

    public StatsMarketService() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. TOP PRODUITS PAR QUANTITÉ VENDUE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Retourne les N meilleurs produits du vendeur par quantité totale vendue.
     * Clé   = nom du produit
     * Valeur = quantité totale vendue
     */
    public Map<String, Integer> getTopSellingProducts(int idVendeur, int limit) {
        // LinkedHashMap pour conserver l'ordre décroissant
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = """
                SELECT p.nom, SUM(dc.quantite) AS totalVendu
                FROM detailscommande dc
                JOIN produits p ON dc.id_produit = p.idProduit
                JOIN commande  c ON dc.id_commande = c.idCommande
                WHERE p.id_user = ?
                  AND c.etat != 'annulee'
                GROUP BY p.idProduit, p.nom
                ORDER BY totalVendu DESC
                LIMIT ?
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            pst.setInt(2, limit);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("nom"), rs.getInt("totalVendu"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[StatsMarketService] getTopSellingProducts : " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. GAINS TOTAUX
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Retourne la somme totale des gains du vendeur (quantité × prixUnitaire)
     * sur toutes les commandes non annulées.
     */
    public double getTotalEarnings(int idVendeur) {
        String sql = """
                SELECT COALESCE(SUM(dc.quantite * dc.prixUnitaire), 0) AS total
                FROM detailscommande dc
                JOIN produits p ON dc.id_produit = p.idProduit
                JOIN commande  c ON dc.id_commande = c.idCommande
                WHERE p.id_user = ?
                  AND c.etat != 'annulee'
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("[StatsMarketService] getTotalEarnings : " + e.getMessage());
        }
        return 0.0;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. TENDANCE DES VENTES PAR JOUR (LineChart)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Retourne le chiffre d'affaires journalier du vendeur sur les N derniers jours.
     * Clé   = date (yyyy-MM-dd)
     * Valeur = montant total ce jour-là
     */
    public Map<String, Double> getSalesTrend(int idVendeur, int nbJours) {
        Map<String, Double> result = new LinkedHashMap<>();

        // Pré-remplir les N derniers jours à 0 pour avoir une courbe continue
        LocalDate today = LocalDate.now();
        for (int i = nbJours - 1; i >= 0; i--) {
            result.put(today.minusDays(i).toString(), 0.0);
        }

        String sql = """
                SELECT DATE(c.dateCommande) AS jour,
                       SUM(dc.quantite * dc.prixUnitaire) AS montant
                FROM detailscommande dc
                JOIN produits p ON dc.id_produit = p.idProduit
                JOIN commande  c ON dc.id_commande = c.idCommande
                WHERE p.id_user = ?
                  AND c.etat != 'annulee'
                  AND c.dateCommande >= ?
                GROUP BY DATE(c.dateCommande)
                ORDER BY jour ASC
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            pst.setDate(2, Date.valueOf(today.minusDays(nbJours - 1)));
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String jour = rs.getString("jour");
                    if (result.containsKey(jour)) {
                        result.put(jour, rs.getDouble("montant"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[StatsMarketService] getSalesTrend : " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. RÉPARTITION DU STOCK PAR CATÉGORIE (PieChart)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Retourne le stock total par catégorie pour le vendeur.
     * Clé   = catégorie
     * Valeur = stock total dans cette catégorie
     */
    public Map<String, Integer> getStockDistribution(int idVendeur) {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = """
                SELECT COALESCE(categorie, 'Non classé') AS categorie,
                       SUM(quantiteStock) AS totalStock
                FROM produits
                WHERE id_user = ?
                GROUP BY categorie
                ORDER BY totalStock DESC
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("categorie"), rs.getInt("totalStock"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[StatsMarketService] getStockDistribution : " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. NOTE MOYENNE GLOBALE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Retourne la note moyenne (sur 5) de tous les produits du vendeur.
     */
    public double getAverageRating(int idVendeur) {
        String sql = """
                SELECT COALESCE(AVG(a.note), 0) AS moyenne
                FROM avis a
                JOIN produits p ON a.id_produit = p.idProduit
                WHERE p.id_user = ?
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getDouble("moyenne");
            }
        } catch (SQLException e) {
            System.err.println("[StatsMarketService] getAverageRating : " + e.getMessage());
        }
        return 0.0;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. ALERTES STOCK BAS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Retourne les produits du vendeur dont le stock est inférieur au seuil.
     */
    public List<Produit> getLowStockAlerts(int idVendeur, int seuil) {
        List<Produit> result = new ArrayList<>();
        String sql = """
                SELECT * FROM produits
                WHERE id_user = ?
                  AND quantiteStock <= ?
                ORDER BY quantiteStock ASC
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            pst.setInt(2, seuil);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    result.add(extraireProduit(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[StatsMarketService] getLowStockAlerts : " + e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. NOMBRE TOTAL DE COMMANDES REÇUES
    // ════════════════════════════════════════════════════════════════════════

    public int getTotalCommandes(int idVendeur) {
        String sql = """
                SELECT COUNT(DISTINCT c.idCommande) AS total
                FROM commande c
                JOIN detailscommande dc ON c.idCommande = dc.id_commande
                JOIN produits p ON dc.id_produit = p.idProduit
                WHERE p.id_user = ?
                  AND c.etat != 'annulee'
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("[StatsMarketService] getTotalCommandes : " + e.getMessage());
        }
        return 0;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. NOMBRE TOTAL DE PRODUITS
    // ════════════════════════════════════════════════════════════════════════

    public int getTotalProduits(int idVendeur) {
        String sql = "SELECT COUNT(*) AS total FROM produits WHERE id_user = ?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("[StatsMarketService] getTotalProduits : " + e.getMessage());
        }
        return 0;
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPER
    // ════════════════════════════════════════════════════════════════════════

    private Produit extraireProduit(ResultSet rs) throws SQLException {
        UniteMesure unite = null;
        try {
            String u = rs.getString("uniteMesure");
            if (u != null) unite = UniteMesure.valueOf(u);
        } catch (Exception ignored) { }

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
            String tr = rs.getString("typeRemise");
            if (tr != null) p.setTypeRemise(TypeReduction.valueOf(tr));
            p.setRemise(rs.getFloat("remise"));
        } catch (Exception ignored) { }

        return p;
    }
}