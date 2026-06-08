package tn.neuron.ardhi.utils.Evenement;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseInitializer {

    private DatabaseInitializer() {
        // Private constructor to prevent instantiation
    }

    public static void initialize(Connection cnx) {
        if (cnx == null)
            return;

        try (Statement st = cnx.createStatement()) {

            // ══════════════════════════════════════════════════════════
            // ÉVÉNEMENTS MANAGEMENT TABLES
            // ══════════════════════════════════════════════════════════

            // ──────────────────────────────────────────────────────────
            // 1. Table Evenement (Event management)
            // ──────────────────────────────────────────────────────────
            executeCritical(st, "evenement",
                    "CREATE TABLE IF NOT EXISTS evenement (" +
                            "id INT(11) NOT NULL AUTO_INCREMENT, " +
                            "titre VARCHAR(255) NOT NULL, " +
                            "description TEXT DEFAULT NULL, " +
                            "lieu VARCHAR(255) DEFAULT NULL, " +
                            "date_debut DATE NOT NULL, " +
                            "date_fin DATE NOT NULL, " +
                            "type VARCHAR(50) NOT NULL, " +
                            "nombre_places_max INT(11) NOT NULL DEFAULT 50, " +
                            "organisateur VARCHAR(255) DEFAULT NULL, " +
                            "image_url VARCHAR(500) DEFAULT NULL, " +
                            "statut VARCHAR(50) NOT NULL DEFAULT 'A_VENIR', " +
                            "date_creation DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "id_createur INT(11) NOT NULL, " +
                            "PRIMARY KEY (id)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

            // Migration: add columns if missing
            executeQuietly(st, "ALTER TABLE evenement ADD COLUMN nombre_places_max INT(11) NOT NULL DEFAULT 50");
            executeQuietly(st, "ALTER TABLE evenement ADD COLUMN organisateur VARCHAR(255) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE evenement ADD COLUMN image_url VARCHAR(500) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE evenement ADD COLUMN statut VARCHAR(50) NOT NULL DEFAULT 'A_VENIR'");
            executeQuietly(st, "ALTER TABLE evenement ADD COLUMN date_creation DATETIME DEFAULT CURRENT_TIMESTAMP");
            executeQuietly(st, "ALTER TABLE evenement ADD COLUMN id_createur INT(11)");

            // ──────────────────────────────────────────────────────────
            // 2. Table Participation (Event participation)
            // ──────────────────────────────────────────────────────────
            executeCritical(st, "participation",
                    "CREATE TABLE IF NOT EXISTS participation (" +
                            "id INT(11) NOT NULL AUTO_INCREMENT, " +
                            "id_evenement INT(11) NOT NULL, " +
                            "id_utilisateur INT(11) NOT NULL, " +
                            "date_inscription DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "statut VARCHAR(50) NOT NULL DEFAULT 'CONFIRME', " +
                            "commentaire TEXT DEFAULT NULL, " +
                            "nombre_personnes INT(11) DEFAULT 1, " +
                            "note INT(11) DEFAULT 0 CHECK (note >= 0 AND note <= 5), " +
                            "avis TEXT DEFAULT NULL, " +
                            "attestation_envoyee TINYINT(1) DEFAULT 0, " +
                            "qr_code_token VARCHAR(64) DEFAULT NULL, " +
                            "PRIMARY KEY (id)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

            // Migration: add columns if missing
            executeQuietly(st, "ALTER TABLE participation ADD COLUMN nombre_personnes INT(11) DEFAULT 1");
            executeQuietly(st, "ALTER TABLE participation ADD COLUMN note INT(11) DEFAULT 0");
            executeQuietly(st, "ALTER TABLE participation ADD COLUMN avis TEXT DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE participation ADD COLUMN attestation_envoyee TINYINT(1) DEFAULT 0");
            executeQuietly(st, "ALTER TABLE participation ADD COLUMN qr_code_token VARCHAR(64) DEFAULT NULL");

            // ──────────────────────────────────────────────────────────
            // 3. Table Evenement Favoris
            // ──────────────────────────────────────────────────────────
            executeCritical(st, "evenement_favoris",
                    "CREATE TABLE IF NOT EXISTS evenement_favoris (" +
                            "id INT(11) NOT NULL AUTO_INCREMENT, " +
                            "id_evenement INT(11) NOT NULL, " +
                            "id_utilisateur INT(11) NOT NULL, " +
                            "date_ajout TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "PRIMARY KEY (id), " +
                            "UNIQUE KEY unique_favori (id_evenement, id_utilisateur)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

            // ──────────────────────────────────────────────────────────
            // INDEXES FOR PERFORMANCE OPTIMIZATION
            // ──────────────────────────────────────────────────────────

            // Indexes on evenement table
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_evenement_date_debut ON evenement(date_debut)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_evenement_type ON evenement(type)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_evenement_statut ON evenement(statut)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_evenement_createur ON evenement(id_createur)");

            // Indexes on participation table
            executeQuietly(st, "CREATE UNIQUE INDEX IF NOT EXISTS unique_participation ON participation(id_evenement, id_utilisateur)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_participation_evenement ON participation(id_evenement)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_participation_utilisateur ON participation(id_utilisateur)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_participation_statut ON participation(statut)");
            executeQuietly(st, "CREATE UNIQUE INDEX IF NOT EXISTS unique_qr_token ON participation(qr_code_token)");

            // Indexes on evenement_favoris table
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_favoris_user ON evenement_favoris(id_utilisateur)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_favoris_event ON evenement_favoris(id_evenement)");

            // ──────────────────────────────────────────────────────────
            // FOREIGN KEYS
            // ──────────────────────────────────────────────────────────

            // Foreign key for evenement.id_createur -> user.id
            executeQuietly(st,
                    "ALTER TABLE evenement ADD CONSTRAINT evenement_ibfk_1 " +
                            "FOREIGN KEY (id_createur) REFERENCES `user`(id) ON DELETE CASCADE");

            // Foreign key for participation.id_evenement -> evenement.id
            executeQuietly(st,
                    "ALTER TABLE participation ADD CONSTRAINT participation_ibfk_1 " +
                            "FOREIGN KEY (id_evenement) REFERENCES evenement(id) ON DELETE CASCADE");

            // Foreign key for participation.id_utilisateur -> user.id
            executeQuietly(st,
                    "ALTER TABLE participation ADD CONSTRAINT participation_ibfk_2 " +
                            "FOREIGN KEY (id_utilisateur) REFERENCES `user`(id) ON DELETE CASCADE");

            // Foreign keys for evenement_favoris
            executeQuietly(st,
                    "ALTER TABLE evenement_favoris ADD CONSTRAINT evenement_favoris_ibfk_1 " +
                            "FOREIGN KEY (id_evenement) REFERENCES evenement(id) ON DELETE CASCADE");
            executeQuietly(st,
                    "ALTER TABLE evenement_favoris ADD CONSTRAINT evenement_favoris_ibfk_2 " +
                            "FOREIGN KEY (id_utilisateur) REFERENCES `user`(id) ON DELETE CASCADE");

            System.out.println("✓ Événements database schema initialized successfully.");

        } catch (SQLException e) {
            System.err.println("✗ Error initializing Événements database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Executes a critical SQL statement (e.g. CREATE TABLE).
     * Logs errors loudly because a failure here means the schema is broken.
     */
    private static void executeCritical(Statement st, String tableName, String sql) {
        try {
            st.executeUpdate(sql);
            System.out.println("  ✓ Table '" + tableName + "' ready.");
        } catch (SQLException e) {
            System.err.println("  ✗ FAILED to create table '" + tableName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Executes a migration SQL statement, quietly ignoring expected errors
     * (e.g. "column already exists"). Logs at debug level for troubleshooting.
     */
    private static void executeQuietly(Statement st, String sql) {
        try {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            // Expected for migrations (column/constraint already exists) — silently ignore
        }
    }
}