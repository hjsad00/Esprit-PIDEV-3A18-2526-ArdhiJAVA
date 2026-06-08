package tn.neuron.ardhi.services.Parcelle_Cultures;

import tn.neuron.ardhi.models.Parcelle_Cultures.AdminAgricultureStats;
import tn.neuron.ardhi.models.Parcelle_Cultures.FarmerStats;
import tn.neuron.ardhi.models.Parcelle_Cultures.AdminAgricultureStats.TopCulture;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service central pour les statistiques Parcelles / Cultures.
 * Architecture : Controller -> AgricultureStatisticsService -> Database.
 */
public class AgricultureStatisticsService {

    private final Connection cnx;

    public AgricultureStatisticsService() {
        this.cnx = MyDatabase.getInstance().getCnx();
    }

    // ==================== STATS AGRICULTEUR ====================

    /**
     * Calcule tous les indicateurs pour un agriculteur.
     *
     * @param agriculteurId     id de l'agriculteur
     * @param optimisationEau   score 0..10 calculé par IrrigationService
     * @param rentabilite       score 0..10 calculé par FinancialService
     */
    public FarmerStats getFarmerStats(int agriculteurId,
                                      double optimisationEau,
                                      double rentabilite) throws SQLException {
        FarmerStats stats = new FarmerStats();

        // Surface totale (somme des parcelles)
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT COALESCE(SUM(surface),0) " +
                        "FROM parcelle WHERE agriculteur_id = ?")) {
            ps.setInt(1, agriculteurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setSurfaceTotale(rs.getDouble(1));
                }
            }
        }

        // Production totale et rendement moyen (somme surface_utilisee * rendement_estime)
        double prodTotale = 0;
        double surfaceCultivee = 0;
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT " +
                        "COALESCE(SUM(c.surface_utilisee * c.rendement_estime),0), " +
                        "COALESCE(SUM(c.surface_utilisee),0) " +
                        "FROM culture c " +
                        "JOIN parcelle p ON c.parcelle_id = p.id " +
                        "WHERE p.agriculteur_id = ?")) {
            ps.setInt(1, agriculteurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    prodTotale = rs.getDouble(1);
                    surfaceCultivee = rs.getDouble(2);
                }
            }
        }
        stats.setProductionTotale(prodTotale);
        stats.setRendementMoyen(surfaceCultivee > 0 ? prodTotale / surfaceCultivee : 0);

        // Répartition des types de cultures
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        long totalCultures = 0;
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT c.type_culture, COUNT(*) " +
                        "FROM culture c " +
                        "JOIN parcelle p ON c.parcelle_id = p.id " +
                        "WHERE p.agriculteur_id = ? " +
                        "GROUP BY c.type_culture")) {
            ps.setInt(1, agriculteurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString(1);
                    long nb = rs.getLong(2);
                    typeCounts.put(type == null ? "Inconnu" : type, nb);
                    totalCultures += nb;
                }
            }
        }

        Map<String, Double> repartitionPourcent = new LinkedHashMap<>();
        if (totalCultures > 0) {
            for (Map.Entry<String, Long> e : typeCounts.entrySet()) {
                repartitionPourcent.put(e.getKey(), e.getValue() * 100.0 / totalCultures);
            }
        }
        stats.setRepartitionTypesPourcent(repartitionPourcent);

        // Diversification = nombre types / total cultures
        double diversification = (totalCultures > 0)
                ? (double) typeCounts.size() / totalCultures
                : 0.0;
        stats.setDiversification(diversification);

        // Taux de parcelles actives
        double actives = 0;
        double totalParcelles = 0;
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT " +
                        "COALESCE(SUM(CASE WHEN statut = 'active' THEN 1 ELSE 0 END),0), " +
                        "COALESCE(COUNT(*),0) " +
                        "FROM parcelle WHERE agriculteur_id = ?")) {
            ps.setInt(1, agriculteurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    actives = rs.getDouble(1);
                    totalParcelles = rs.getDouble(2);
                }
            }
        }
        double tauxActives = totalParcelles > 0 ? (actives * 100.0 / totalParcelles) : 0.0;
        stats.setTauxParcellesActives(tauxActives);

        // Scores externes
        stats.setOptimisationEau(optimisationEau);
        stats.setRentabilite(rentabilite);

        // Score global (toutes les composantes ramenées sur 0..10)
        double diversificationScore = diversification * 10.0;          // 0..1 -> 0..10
        double rendementScore = Math.min(10.0, stats.getRendementMoyen() / 3.0 * 10.0); // 0..3 t/ha -> 0..10
        double optimEauScore = Math.max(0.0, Math.min(10.0, optimisationEau));
        double rentabiliteScore = Math.max(0.0, Math.min(10.0, rentabilite));

        double scoreGlobal =
                0.3 * diversificationScore +
                0.3 * rendementScore +
                0.2 * optimEauScore +
                0.2 * rentabiliteScore;
        stats.setScoreGlobal(scoreGlobal);

        // Seuils de performance affinés
        String niveau;
        if (scoreGlobal < 3) {
            niveau = "Faible";
        } else if (scoreGlobal < 7) {
            niveau = "Moyen";
        } else {
            niveau = "Excellent";
        }
        stats.setNiveauPerformance(niveau);

        return stats;
    }

    // ==================== STATS ADMIN ====================

    public AdminAgricultureStats getAdminStats() throws SQLException {
        AdminAgricultureStats stats = new AdminAgricultureStats();

        // Surface totale plateforme
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(surface),0) FROM parcelle")) {
            if (rs.next()) {
                stats.setSurfaceTotalePlateforme(rs.getDouble(1));
            }
        }

        // Nombre d'agriculteurs actifs (au moins une parcelle)
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(DISTINCT agriculteur_id) " +
                             "FROM parcelle WHERE agriculteur_id IS NOT NULL")) {
            if (rs.next()) {
                stats.setNbAgriculteursActifs(rs.getInt(1));
            }
        }

        // Production globale et rendement moyen global
        double prodTotale = 0;
        double surfaceCultivee = 0;
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT " +
                             "COALESCE(SUM(c.surface_utilisee * c.rendement_estime),0), " +
                             "COALESCE(SUM(c.surface_utilisee),0) " +
                             "FROM culture c " +
                             "JOIN parcelle p ON c.parcelle_id = p.id")) {
            if (rs.next()) {
                prodTotale = rs.getDouble(1);
                surfaceCultivee = rs.getDouble(2);
            }
        }
        stats.setProductionGlobale(prodTotale);
        stats.setRendementMoyenGlobal(surfaceCultivee > 0 ? prodTotale / surfaceCultivee : 0.0);

        // Top 5 cultures par fréquence
        List<TopCulture> topCultures = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT nom_culture, type_culture, COUNT(*) AS nb " +
                             "FROM culture " +
                             "GROUP BY nom_culture, type_culture " +
                             "ORDER BY nb DESC " +
                             "LIMIT 5")) {
            while (rs.next()) {
                TopCulture tc = new TopCulture();
                tc.setNom(rs.getString(1));
                tc.setType(rs.getString(2));
                tc.setFrequence(rs.getLong(3));
                topCultures.add(tc);
            }
        }
        stats.setTopCultures(topCultures);

        return stats;
    }
}

