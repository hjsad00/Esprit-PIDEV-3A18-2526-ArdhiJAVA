package tn.neuron.ardhi.utils.Parcelle_Cultures;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * DatabaseInitializer - Initialise uniquement les tables parcelle et culture (Ardhi).
 */
public class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize(Connection cnx) {
        if (cnx == null) {
            System.err.println("⚠️ Connection is null, cannot initialize database");
            return;
        }

        try (Statement st = cnx.createStatement()) {
            System.out.println("🚀 Initializing parcelle & culture tables...");

            // ═══════════════════════════════════════════════════════════════
            // PARCELLE & CULTURE
            // ═══════════════════════════════════════════════════════════════

            // Table Parcelle (parcelles de l'agriculteur)
            executeCritical(st, "parcelle",
                    "CREATE TABLE IF NOT EXISTS `parcelle` (" +
                            "`id` INT(11) NOT NULL AUTO_INCREMENT, " +
                            "`surface` DOUBLE NOT NULL, " +
                            "`localisation` VARCHAR(255) DEFAULT NULL, " +
                            "`type_sol` VARCHAR(255) DEFAULT NULL, " +
                            "`systeme_irrigation` VARCHAR(255) DEFAULT NULL, " +
                            "`statut` VARCHAR(50) DEFAULT 'active', " +
                            "`agriculteur_id` INT(11) DEFAULT NULL, " +
                            "`latitude` DOUBLE DEFAULT NULL, " +
                            "`longitude` DOUBLE DEFAULT NULL, " +
                            "PRIMARY KEY (`id`), " +
                            "KEY `fk_parcelle_agriculteur` (`agriculteur_id`)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

            // Migration Parcelle
            executeQuietly(st, "ALTER TABLE `parcelle` ADD COLUMN `latitude` DOUBLE DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE `parcelle` ADD COLUMN `longitude` DOUBLE DEFAULT NULL");

            // Table Culture (cultures par parcelle)
            executeCritical(st, "culture",
                    "CREATE TABLE IF NOT EXISTS `culture` (" +
                            "`id` INT(11) NOT NULL AUTO_INCREMENT, " +
                            "`nom_culture` VARCHAR(255) NOT NULL, " +
                            "`type_culture` VARCHAR(255) DEFAULT NULL, " +
                            "`saison` VARCHAR(100) DEFAULT NULL, " +
                            "`date_plantation` DATE DEFAULT NULL, " +
                            "`date_recolte_prevue` DATE DEFAULT NULL, " +
                            "`etat_culture` VARCHAR(50) DEFAULT 'en_croissance', " +
                            "`parcelle_id` INT(11) NOT NULL, " +
                            "`surface_utilisee` DOUBLE NOT NULL DEFAULT 0, " +
                            "`rendement_estime` DOUBLE DEFAULT 0, " +
                            "PRIMARY KEY (`id`), " +
                            "KEY `fk_culture_parcelle` (`parcelle_id`)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

            // Migration Culture
            executeQuietly(st, "ALTER TABLE `culture` ADD COLUMN `surface_utilisee` DOUBLE NOT NULL DEFAULT 0");
            executeQuietly(st, "ALTER TABLE `culture` ADD COLUMN `rendement_estime` DOUBLE DEFAULT 0");

            // Foreign keys
            executeQuietly(st,
                    "ALTER TABLE `parcelle` ADD CONSTRAINT `parcelle_ibfk_1` FOREIGN KEY (`agriculteur_id`) REFERENCES `user`(`id`) ON DELETE CASCADE");
            executeQuietly(st,
                    "ALTER TABLE `culture` ADD CONSTRAINT `culture_ibfk_1` FOREIGN KEY (`parcelle_id`) REFERENCES `parcelle`(`id`) ON DELETE CASCADE");

            System.out.println("✅ Parcelle & culture tables ready (with Smart Agriculture fields).");
        } catch (SQLException e) {
            System.err.println("❌ Error initializing parcelle/culture: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeCritical(Statement st, String tableName, String sql) {
        try {
            st.executeUpdate(sql);
            System.out.println("  ✓ Table '" + tableName + "' ready");
        } catch (SQLException e) {
            System.err.println("  ✗ FAILED to create table '" + tableName + "': " + e.getMessage());
        }
    }

    private static void executeQuietly(Statement st, String sql) {
        try {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            // ignoré (contrainte déjà existante, etc.)
        }
    }
}
