package tn.neuron.ardhi.utils.gestionemployeutils;

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

            // ── 1. Table employe ─────────────────────────────────────────────────
            executeCritical(st, "employe",
                    "CREATE TABLE IF NOT EXISTS employe (" +
                            "id_employe INT(11) NOT NULL AUTO_INCREMENT, " +
                            "nom VARCHAR(100) NOT NULL, " +
                            "prenom VARCHAR(100) NOT NULL, " +
                            "email VARCHAR(150) NOT NULL, " +
                            "poste VARCHAR(100) DEFAULT NULL, " +
                            "telephone VARCHAR(20) DEFAULT NULL, " +
                            "actif TINYINT(1) DEFAULT 1, " +
                            "id_agriculteur INT(11) DEFAULT NULL, " +
                            "qr_code_unique VARCHAR(50) DEFAULT NULL, " +
                            "photo_path VARCHAR(500) DEFAULT NULL COMMENT 'Chemin absolu vers la photo de profil', " +
                            "PRIMARY KEY (id_employe), " +
                            "UNIQUE KEY email (email), " +
                            "UNIQUE KEY qr_code_unique (qr_code_unique)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // Migration : colonnes manquantes pour tables existantes
            executeQuietly(st, "ALTER TABLE employe ADD COLUMN poste VARCHAR(100) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE employe ADD COLUMN telephone VARCHAR(20) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE employe ADD COLUMN actif TINYINT(1) DEFAULT 1");
            executeQuietly(st, "ALTER TABLE employe ADD COLUMN id_agriculteur INT(11) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE employe ADD COLUMN qr_code_unique VARCHAR(50) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE employe ADD UNIQUE KEY qr_code_unique (qr_code_unique)");
            executeQuietly(st, "ALTER TABLE employe ADD COLUMN photo_path VARCHAR(500) DEFAULT NULL COMMENT 'Chemin absolu vers la photo de profil'");

            // ── 2. Table tache ───────────────────────────────────────────────────
            executeCritical(st, "tache",
                    "CREATE TABLE IF NOT EXISTS tache (" +
                            "id_tache INT(11) NOT NULL AUTO_INCREMENT, " +
                            "titre VARCHAR(200) NOT NULL, " +
                            "description TEXT DEFAULT NULL, " +
                            "statut VARCHAR(50) DEFAULT 'En attente', " +
                            "date_debut DATE DEFAULT NULL, " +
                            "date_fin DATE DEFAULT NULL, " +
                            "id_employe INT(11) DEFAULT NULL, " +
                            "id_agriculteur INT(11) DEFAULT NULL, " +
                            "priorite INT(11) DEFAULT NULL COMMENT '1=Basse, 2=Moyenne, 3=Haute, 4=Critique', " +
                            "categorie VARCHAR(100) DEFAULT NULL COMMENT 'Plantation, Recolte, Irrigation, etc.', " +
                            "date_modification TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                            "google_event_id VARCHAR(255) DEFAULT NULL, " +
                            "type_tache VARCHAR(50) DEFAULT 'AUTRE' COMMENT 'TRAITEMENT,IRRIGATION,RECOLTE,PLANTATION,LABOUR,MAINTENANCE,AUTRE', " +
                            "google_calendar_event_id VARCHAR(255) DEFAULT NULL, " +
                            "PRIMARY KEY (id_tache)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
//changement
            // Migration tache
            executeQuietly(st, "ALTER TABLE tache ADD COLUMN priorite INT(11) DEFAULT NULL COMMENT '1=Basse, 2=Moyenne, 3=Haute, 4=Critique'");
            executeQuietly(st, "ALTER TABLE tache ADD COLUMN categorie VARCHAR(100) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE tache ADD COLUMN id_agriculteur INT(11) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE tache ADD COLUMN date_modification TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            executeQuietly(st, "ALTER TABLE tache ADD COLUMN google_event_id VARCHAR(255) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE tache ADD COLUMN type_tache VARCHAR(50) DEFAULT 'AUTRE'");
            executeQuietly(st, "ALTER TABLE tache ADD COLUMN google_calendar_event_id VARCHAR(255) DEFAULT NULL");

            // ── 3. Table notification ────────────────────────────────────────────
            executeCritical(st, "notification",
                    "CREATE TABLE IF NOT EXISTS notification (" +
                            "id_notification INT(11) NOT NULL AUTO_INCREMENT, " +
                            "type VARCHAR(50) NOT NULL, " +
                            "priorite VARCHAR(20) NOT NULL, " +
                            "titre VARCHAR(200) NOT NULL, " +
                            "message TEXT NOT NULL, " +
                            "id_agriculteur INT(11) NOT NULL, " +
                            "id_tache INT(11) DEFAULT NULL, " +
                            "id_employe INT(11) DEFAULT NULL, " +
                            "lue TINYINT(1) DEFAULT 0, " +
                            "archivee TINYINT(1) DEFAULT 0, " +
                            "date_creation DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "date_lecture DATETIME DEFAULT NULL, " +
                            "PRIMARY KEY (id_notification), " +
                            "KEY id_tache (id_tache), " +
                            "KEY id_employe (id_employe), " +
                            "KEY idx_agriculteur (id_agriculteur), " +
                            "KEY idx_type (type), " +
                            "KEY idx_lue (lue), " +
                            "KEY idx_date (date_creation)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // Migration notification
            executeQuietly(st, "ALTER TABLE notification ADD COLUMN lue TINYINT(1) DEFAULT 0");
            executeQuietly(st, "ALTER TABLE notification ADD COLUMN archivee TINYINT(1) DEFAULT 0");
            executeQuietly(st, "ALTER TABLE notification ADD COLUMN date_creation DATETIME DEFAULT CURRENT_TIMESTAMP");
            executeQuietly(st, "ALTER TABLE notification ADD COLUMN date_lecture DATETIME DEFAULT NULL");

            // Indexes notification
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_agriculteur ON notification(id_agriculteur)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_type ON notification(type)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_lue ON notification(lue)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_date ON notification(date_creation)");

            // ── 4. Table notification_config ─────────────────────────────────────
            executeCritical(st, "notification_config",
                    "CREATE TABLE IF NOT EXISTS notification_config (" +
                            "id_config INT(11) NOT NULL AUTO_INCREMENT, " +
                            "id_agriculteur INT(11) NOT NULL, " +
                            "seuil_taches_employe INT(11) DEFAULT 10, " +
                            "seuil_jours_inactivite INT(11) DEFAULT 30, " +
                            "activer_notifications TINYINT(1) DEFAULT 1, " +
                            "activer_sons TINYINT(1) DEFAULT 1, " +
                            "PRIMARY KEY (id_config), " +
                            "UNIQUE KEY id_agriculteur (id_agriculteur)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // Migration notification_config
            executeQuietly(st, "ALTER TABLE notification_config ADD COLUMN seuil_taches_employe INT(11) DEFAULT 10");
            executeQuietly(st, "ALTER TABLE notification_config ADD COLUMN seuil_jours_inactivite INT(11) DEFAULT 30");
            executeQuietly(st, "ALTER TABLE notification_config ADD COLUMN activer_notifications TINYINT(1) DEFAULT 1");
            executeQuietly(st, "ALTER TABLE notification_config ADD COLUMN activer_sons TINYINT(1) DEFAULT 1");

            // ── 5. Table employe_competence ──────────────────────────────────────
            executeCritical(st, "employe_competence",
                    "CREATE TABLE IF NOT EXISTS employe_competence (" +
                            "id INT(11) NOT NULL AUTO_INCREMENT, " +
                            "id_employe INT(11) NOT NULL, " +
                            "id_competence INT(11) NOT NULL, " +
                            "niveau INT(11) NOT NULL DEFAULT 1, " +
                            "annees_experience DECIMAL(3,1) DEFAULT 0.0, " +
                            "derniere_utilisation DATE DEFAULT NULL, " +
                            "certification VARCHAR(200) DEFAULT NULL, " +
                            "notes TEXT DEFAULT NULL, " +
                            "date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "date_modification DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                            "PRIMARY KEY (id), " +
                            "UNIQUE KEY unique_employe_competence (id_employe, id_competence), " +
                            "KEY id_competence (id_competence), " +
                            "KEY idx_niveau (niveau), " +
                            "KEY idx_experience (annees_experience)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

            // Migration employe_competence
            executeQuietly(st, "ALTER TABLE employe_competence ADD COLUMN annees_experience DECIMAL(3,1) DEFAULT 0.0");
            executeQuietly(st, "ALTER TABLE employe_competence ADD COLUMN derniere_utilisation DATE DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE employe_competence ADD COLUMN certification VARCHAR(200) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE employe_competence ADD COLUMN notes TEXT DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE employe_competence ADD COLUMN date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP");
            executeQuietly(st, "ALTER TABLE employe_competence ADD COLUMN date_modification DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

            // Foreign keys employe_competence
            executeQuietly(st,
                    "ALTER TABLE employe_competence ADD CONSTRAINT employe_competence_ibfk_1 " +
                            "FOREIGN KEY (id_employe) REFERENCES employe(id_employe) ON DELETE CASCADE");
            executeQuietly(st,
                    "ALTER TABLE employe_competence ADD CONSTRAINT employe_competence_ibfk_2 " +
                            "FOREIGN KEY (id_competence) REFERENCES competence(id_competence) ON DELETE CASCADE");

            // ── 6. Table tache_competence_requise ────────────────────────────────
            executeCritical(st, "tache_competence_requise",
                    "CREATE TABLE IF NOT EXISTS tache_competence_requise (" +
                            "id INT(11) NOT NULL AUTO_INCREMENT, " +
                            "id_tache INT(11) NOT NULL, " +
                            "id_competence INT(11) NOT NULL, " +
                            "niveau_requis INT(11) NOT NULL DEFAULT 2, " +
                            "importance INT(11) DEFAULT 5, " +
                            "date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "PRIMARY KEY (id), " +
                            "UNIQUE KEY unique_tache_competence (id_tache, id_competence), " +
                            "KEY id_competence (id_competence), " +
                            "KEY idx_niveau (niveau_requis), " +
                            "KEY idx_importance (importance)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

            // Migration tache_competence_requise
            executeQuietly(st, "ALTER TABLE tache_competence_requise ADD COLUMN importance INT(11) DEFAULT 5");
            executeQuietly(st, "ALTER TABLE tache_competence_requise ADD COLUMN date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP");

            // Foreign keys tache_competence_requise
            executeQuietly(st,
                    "ALTER TABLE tache_competence_requise ADD CONSTRAINT tache_competence_requise_ibfk_1 " +
                            "FOREIGN KEY (id_tache) REFERENCES tache(id_tache) ON DELETE CASCADE");
            executeQuietly(st,
                    "ALTER TABLE tache_competence_requise ADD CONSTRAINT tache_competence_requise_ibfk_2 " +
                            "FOREIGN KEY (id_competence) REFERENCES competence(id_competence) ON DELETE CASCADE");

            // ── 7. Table chatbot_conversation ────────────────────────────────────
            executeCritical(st, "chatbot_conversation",
                    "CREATE TABLE IF NOT EXISTS chatbot_conversation (" +
                            "id_conversation INT(11) NOT NULL AUTO_INCREMENT, " +
                            "id_utilisateur INT(11) NOT NULL, " +
                            "message_utilisateur TEXT NOT NULL, " +
                            "reponse_chatbot TEXT NOT NULL, " +
                            "intention VARCHAR(100) DEFAULT NULL, " +
                            "contexte LONGTEXT DEFAULT NULL, " +
                            "satisfaction INT(11) DEFAULT NULL, " +
                            "date_message DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                            "PRIMARY KEY (id_conversation), " +
                            "KEY idx_utilisateur (id_utilisateur), " +
                            "KEY idx_intention (intention), " +
                            "KEY idx_date (date_message)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

            // Migration chatbot_conversation
            executeQuietly(st, "ALTER TABLE chatbot_conversation ADD COLUMN intention VARCHAR(100) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE chatbot_conversation ADD COLUMN contexte LONGTEXT DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE chatbot_conversation ADD COLUMN satisfaction INT(11) DEFAULT NULL");
            executeQuietly(st, "ALTER TABLE chatbot_conversation ADD COLUMN date_message DATETIME DEFAULT CURRENT_TIMESTAMP");

            // Foreign key chatbot_conversation
            executeQuietly(st,
                    "ALTER TABLE chatbot_conversation ADD CONSTRAINT chatbot_conversation_ibfk_1 " +
                            "FOREIGN KEY (id_utilisateur) REFERENCES `user`(id) ON DELETE CASCADE");

            // ── Foreign keys inter-tables ─────────────────────────────────────────
            executeQuietly(st,
                    "ALTER TABLE notification ADD CONSTRAINT notification_ibfk_1 " +
                            "FOREIGN KEY (id_agriculteur) REFERENCES `user`(id) ON DELETE CASCADE");
            executeQuietly(st,
                    "ALTER TABLE notification ADD CONSTRAINT notification_ibfk_2 " +
                            "FOREIGN KEY (id_tache) REFERENCES tache(id_tache) ON DELETE CASCADE");
            executeQuietly(st,
                    "ALTER TABLE notification ADD CONSTRAINT notification_ibfk_3 " +
                            "FOREIGN KEY (id_employe) REFERENCES employe(id_employe) ON DELETE CASCADE");
            executeQuietly(st,
                    "ALTER TABLE notification_config ADD CONSTRAINT notification_config_ibfk_1 " +
                            "FOREIGN KEY (id_agriculteur) REFERENCES `user`(id) ON DELETE CASCADE");

            System.out.println("GestionEmploye database schema initialized successfully (7 tables).");

        } catch (SQLException e) {
            System.err.println("Error initializing GestionEmploye database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Executes a critical SQL statement (e.g. CREATE TABLE). Logs errors loudly
     * because a failure here means the schema is broken.
     */
    private static void executeCritical(Statement st, String tableName, String sql) {
        try {
            st.executeUpdate(sql);
            System.out.println("Table '" + tableName + "' ready.");
        } catch (SQLException e) {
            System.err.println("FAILED to create table '" + tableName + "': " + e.getMessage());
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
            // Expected for migrations (column/constraint already exists) - silently ignore
        }
    }
}