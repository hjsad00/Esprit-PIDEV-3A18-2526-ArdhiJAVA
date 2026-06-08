package tn.neuron.ardhi.utils.MaterielEtMaintenance;

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

                        // ── Materiel & Maintenance tables ──

                        // 1. Table Materiel (Equipment management)
                        executeCritical(st, "materiel",
                                        "CREATE TABLE IF NOT EXISTS materiel (" +
                                                        "id_materiel INT(11) NOT NULL AUTO_INCREMENT, " +
                                                        "nom VARCHAR(100) NOT NULL, " +
                                                        "type VARCHAR(50) NOT NULL, " +
                                                        "etat VARCHAR(50) NOT NULL, " +
                                                        "user_id INT(11) NOT NULL, " +
                                                        "date_achat DATE DEFAULT NULL, " +
                                                        "date_prochaine_maintenance DATE DEFAULT NULL, " +
                                                        "google_calendar_event_id VARCHAR(255) DEFAULT NULL, " +
                                                        "derniere_maintenance DATE DEFAULT NULL, " +
                                                        "frequence_maintenance_mois INT(11) DEFAULT 12, " +
                                                        "PRIMARY KEY (id_materiel), " +
                                                        "KEY fk_materiel_user (user_id)" +
                                                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

                        // Migration: add columns if missing
                        executeQuietly(st, "ALTER TABLE materiel ADD COLUMN date_achat DATE DEFAULT NULL");
                        executeQuietly(st,
                                        "ALTER TABLE materiel ADD COLUMN date_prochaine_maintenance DATE DEFAULT NULL");
                        executeQuietly(st,
                                        "ALTER TABLE materiel ADD COLUMN google_calendar_event_id VARCHAR(255) DEFAULT NULL");
                        executeQuietly(st, "ALTER TABLE materiel ADD COLUMN derniere_maintenance DATE DEFAULT NULL");
                        executeQuietly(st,
                                        "ALTER TABLE materiel ADD COLUMN frequence_maintenance_mois INT(11) DEFAULT 12");

                        // Add foreign key for materiel (references user table)
                        executeQuietly(st,
                                        "ALTER TABLE materiel ADD CONSTRAINT fk_materiel_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE ON UPDATE CASCADE");

                        // 2. Table Maintenance (Maintenance tracking)
                        executeCritical(st, "maintenance",
                                        "CREATE TABLE IF NOT EXISTS maintenance (" +
                                                        "id_maintenance INT(11) NOT NULL AUTO_INCREMENT, " +
                                                        "materiel_id INT(11) NOT NULL, " +
                                                        "description TEXT DEFAULT NULL, " +
                                                        "date_maintenance DATE NOT NULL, " +
                                                        "cout DECIMAL(10,2) NOT NULL, " +
                                                        "google_calendar_event_id VARCHAR(255) DEFAULT NULL, " +
                                                        "statut_maintenance VARCHAR(50) DEFAULT 'planifiee', " +
                                                        "date_planifiee DATE DEFAULT NULL, " +
                                                        "date_realisee DATE DEFAULT NULL, " +
                                                        "type_maintenance VARCHAR(50) DEFAULT 'preventive', " +
                                                        "PRIMARY KEY (id_maintenance), " +
                                                        "KEY idx_materiel_id (materiel_id), " +
                                                        "KEY idx_statut (statut_maintenance), " +
                                                        "KEY idx_date_planifiee (date_planifiee)" +
                                                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

                        // Migration: add columns if missing
                        executeQuietly(st,
                                        "ALTER TABLE maintenance ADD COLUMN google_calendar_event_id VARCHAR(255) DEFAULT NULL");
                        executeQuietly(st,
                                        "ALTER TABLE maintenance ADD COLUMN statut_maintenance VARCHAR(50) DEFAULT 'planifiee'");
                        executeQuietly(st, "ALTER TABLE maintenance ADD COLUMN date_planifiee DATE DEFAULT NULL");
                        executeQuietly(st, "ALTER TABLE maintenance ADD COLUMN date_realisee DATE DEFAULT NULL");
                        executeQuietly(st,
                                        "ALTER TABLE maintenance ADD COLUMN type_maintenance VARCHAR(50) DEFAULT 'preventive'");

                        // Add foreign key for maintenance (references materiel table)
                        executeQuietly(st,
                                        "ALTER TABLE maintenance ADD CONSTRAINT fk_maintenance_materiel FOREIGN KEY (materiel_id) REFERENCES materiel(id_materiel) ON DELETE CASCADE ON UPDATE CASCADE");

                        // Add indexes for performance optimization
                        executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_materiel_id ON maintenance(materiel_id)");
                        executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_statut ON maintenance(statut_maintenance)");
                        executeQuietly(st,
                                        "CREATE INDEX IF NOT EXISTS idx_date_planifiee ON maintenance(date_planifiee)");
                        executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_materiel_user ON materiel(user_id)");

                        // System.out.println("Materiel & Maintenance database schema initialized
                        // successfully.");

                } catch (SQLException e) {
                        System.err.println(
                                        "Error initializing Materiel & Maintenance database schema: " + e.getMessage());
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
                        // System.out.println(" ✓ Table '" + tableName + "' ready.");
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