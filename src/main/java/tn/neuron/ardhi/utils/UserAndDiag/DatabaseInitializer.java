package tn.neuron.ardhi.utils.UserAndDiag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class DatabaseInitializer {

        private DatabaseInitializer() {
                // Private constructor to prevent instantiation
        }

        public static void initialize(Connection cnx) {
                if (cnx == null)
                        return;

                try (Statement st = cnx.createStatement()) {

                        // ── Core tables (failures here are fatal) ──

                        // 1. Table User
                        executeCritical(st, "user",
                                        "CREATE TABLE IF NOT EXISTS `user` (" +
                                                        "id int(11) NOT NULL AUTO_INCREMENT, " +
                                                        "email varchar(180) NOT NULL, " +
                                                        "role enum('ADMIN','AGRICULTEUR','CLIENT','AGRONOME') NOT NULL DEFAULT 'AGRICULTEUR', "
                                                        +
                                                        "password varchar(255) NOT NULL, " +
                                                        "nom varchar(255) NOT NULL, " +
                                                        "prenom varchar(255) NOT NULL, " +
                                                        "points INT DEFAULT 0, " +
                                                        "level INT DEFAULT 1, " +
                                                        "PRIMARY KEY (id), " +
                                                        "UNIQUE KEY UNIQ_IDENTIFIER_EMAIL (email)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                        // Migration: add columns if missing
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN points INT DEFAULT 0");
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN level INT DEFAULT 1");
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN face_signature LONGTEXT");
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE");
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN two_factor_code VARCHAR(10)");
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN two_factor_expires_at DATETIME");
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN fingerprint_signature MEDIUMTEXT");
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN reset_password_code VARCHAR(10)");
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN reset_password_expires_at DATETIME");
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN phone VARCHAR(20)");
                        executeQuietly(st, "ALTER TABLE `user` ADD COLUMN location VARCHAR(255)");

                        // 2. Table Offre
                        executeCritical(st, "offre",
                                        "CREATE TABLE IF NOT EXISTS offre (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "nom VARCHAR(100) NOT NULL, " +
                                                        "description VARCHAR(255), " +
                                                        "prix_mensuel FLOAT NOT NULL, " +
                                                        "avantages TEXT, " +
                                                        "couleur_primaire VARCHAR(20) DEFAULT '#6B7F3F', " +
                                                        "couleur_secondaire VARCHAR(20) DEFAULT '#4A5A2B', " +
                                                        "est_active BOOLEAN DEFAULT TRUE, " +
                                                        "est_recommandee BOOLEAN DEFAULT FALSE, " +
                                                        "date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "diagnostics_par_heure INT DEFAULT 3, " +
                                                        "acces_traitement BOOLEAN DEFAULT FALSE)");

                        // Migration: add columns if missing
                        executeQuietly(st, "ALTER TABLE offre ADD COLUMN diagnostics_par_heure INT DEFAULT 3");
                        executeQuietly(st, "ALTER TABLE offre ADD COLUMN acces_traitement BOOLEAN DEFAULT FALSE");
                        executeQuietly(st, "ALTER TABLE offre ADD COLUMN acces_plan_traitement BOOLEAN DEFAULT FALSE");

                        // 3. Table Abonnement
                        executeCritical(st, "abonnement",
                                        "CREATE TABLE IF NOT EXISTS abonnement (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "type VARCHAR(100), " +
                                                        "prix FLOAT NOT NULL, " +
                                                        "date_debut DATE, " +
                                                        "date_fin DATE, " +
                                                        "statut VARCHAR(50) DEFAULT 'ACTIF', " +
                                                        "user_id INT, " +
                                                        "offre_id INT)");

                        // 4. Table Diagnostic
                        executeCritical(st, "diagnostic",
                                        "CREATE TABLE IF NOT EXISTS diagnostic (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "date_scan TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "image_scannee VARCHAR(500), " +
                                                        "resultat_ia TEXT, " +
                                                        "confiance FLOAT DEFAULT 0, " +
                                                        "user_id INT, " +
                                                        "latitude DOUBLE, " +
                                                        "longitude DOUBLE, " +
                                                        "location_label VARCHAR(255), " +
                                                        "severity VARCHAR(20))");

                        // Migration: add location/severity columns if missing
                        executeQuietly(st, "ALTER TABLE diagnostic ADD COLUMN latitude DOUBLE");
                        executeQuietly(st, "ALTER TABLE diagnostic ADD COLUMN longitude DOUBLE");
                        executeQuietly(st, "ALTER TABLE diagnostic ADD COLUMN location_label VARCHAR(255)");
                        executeQuietly(st, "ALTER TABLE diagnostic ADD COLUMN severity VARCHAR(20)");

                        // 5. Table Traitement
                        executeCritical(st, "traitement",
                                        "CREATE TABLE IF NOT EXISTS traitement (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "diagnostic_id INT NOT NULL, " +
                                                        "solution_nom VARCHAR(255) NOT NULL, " +
                                                        "description_detaillee TEXT, " +
                                                        "type_traitement ENUM('FONGICIDE','HERBICIDE','INSECTICIDE','BACTERICIDE','NEMATICIDE','VIRUCIDE','NUTRIMENT','REGULATEUR_CROISSANCE','AUTRE') DEFAULT 'AUTRE', "
                                                        + "duree_recommandee VARCHAR(50))");

                        // 6. Table TreatmentPlan (Active Recovery)
                        executeCritical(st, "treatment_plan",
                                        "CREATE TABLE IF NOT EXISTS treatment_plan (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "diagnostic_id INT NOT NULL, " +
                                                        "start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "status ENUM('ACTIVE', 'COMPLETED', 'ABANDONED') DEFAULT 'ACTIVE', "
                                                        + "FOREIGN KEY (diagnostic_id) REFERENCES diagnostic(id) ON DELETE CASCADE)");

                        // Migration: add column if it was created before the update
                        executeQuietly(st, "ALTER TABLE treatment_task ADD COLUMN tech_x DOUBLE DEFAULT 0");
                        executeQuietly(st, "ALTER TABLE treatment_task ADD COLUMN tech_y DOUBLE DEFAULT 0");

                        // 7. Table TreatmentTask (Daily steps)
                        executeCritical(st, "treatment_task",
                                        "CREATE TABLE IF NOT EXISTS treatment_task (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "treatment_plan_id INT NOT NULL, " +
                                                        "day_offset INT NOT NULL, " +
                                                        "task_description VARCHAR(255) NOT NULL, " +
                                                        "status ENUM('PENDING', 'COMPLETED', 'MISSED') DEFAULT 'PENDING', "
                                                        +
                                                        "tech_x DOUBLE DEFAULT 0, " +
                                                        "tech_y DOUBLE DEFAULT 0, " +
                                                        "image_path VARCHAR(500), " +
                                                        "FOREIGN KEY (treatment_plan_id) REFERENCES treatment_plan(id) ON DELETE CASCADE)");

                        // ── Health Scan tables ──

                        // 8. Farm Health Scan
                        executeCritical(st, "farm_health_scan",
                                        "CREATE TABLE IF NOT EXISTS farm_health_scan (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "user_id INT NOT NULL, " +
                                                        "crop_type VARCHAR(100) NOT NULL, " +
                                                        "planting_date DATE NOT NULL, " +
                                                        "growth_stage VARCHAR(50) NOT NULL, " +
                                                        "latitude DOUBLE, " +
                                                        "longitude DOUBLE, " +
                                                        "concerns TEXT, " +
                                                        "photo_crops VARCHAR(500), " +
                                                        "photo_soil VARCHAR(500), " +
                                                        "photo_edges VARCHAR(500), " +
                                                        "photo_insects VARCHAR(500), " +
                                                        "photo_spacing VARCHAR(500), " +
                                                        "photo_overview VARCHAR(500), " +
                                                        "scan_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "status ENUM('PENDING','PROCESSING','COMPLETED','FAILED') DEFAULT 'PENDING', "
                                                        +
                                                        "FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE)");

                        // 9. Farm Health Report
                        executeCritical(st, "farm_health_report",
                                        "CREATE TABLE IF NOT EXISTS farm_health_report (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "scan_id INT NOT NULL, " +
                                                        "health_score INT, " +
                                                        "biodiversity_score INT, " +
                                                        "llava_analysis TEXT, " +
                                                        "generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "FOREIGN KEY (scan_id) REFERENCES farm_health_scan(id) ON DELETE CASCADE)");

                        // 10. Vulnerability
                        executeCritical(st, "vulnerability",
                                        "CREATE TABLE IF NOT EXISTS vulnerability (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "report_id INT NOT NULL, " +
                                                        "type ENUM('PEST_OUTBREAK_RISK','DISEASE_RISK','NUTRIENT_DEFICIENCY','LOW_POLLINATION','SOIL_DEGRADATION') NOT NULL, "
                                                        +
                                                        "severity ENUM('CRITICAL','MEDIUM','LOW') NOT NULL, " +
                                                        "threat VARCHAR(255) NOT NULL, " +
                                                        "description TEXT, " +
                                                        "risk_score FLOAT, " +
                                                        "timeframe_days INT, " +
                                                        "estimated_yield_loss_percent INT, " +
                                                        "estimated_cost_if_occurs FLOAT, " +
                                                        "FOREIGN KEY (report_id) REFERENCES farm_health_report(id) ON DELETE CASCADE)");

                        // 11. Prevention Plan
                        executeCritical(st, "prevention_plan",
                                        "CREATE TABLE IF NOT EXISTS prevention_plan (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "report_id INT NOT NULL, " +
                                                        "vulnerability_id INT, " +
                                                        "title VARCHAR(255) NOT NULL, " +
                                                        "problem_summary TEXT, " +
                                                        "steps TEXT NOT NULL, " +
                                                        "timeline_days INT, " +
                                                        "estimated_cost FLOAT, " +
                                                        "expected_outcome TEXT, " +
                                                        "impact_level ENUM('HIGH','MEDIUM','LOW'), " +
                                                        "status ENUM('ACTIVE','COMPLETED','ABANDONED') DEFAULT 'ACTIVE', "
                                                        +
                                                        "start_date DATE, " +
                                                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "FOREIGN KEY (report_id) REFERENCES farm_health_report(id) ON DELETE CASCADE, "
                                                        +
                                                        "FOREIGN KEY (vulnerability_id) REFERENCES vulnerability(id))");

                        // 12. Prevention Task
                        executeCritical(st, "prevention_task",
                                        "CREATE TABLE IF NOT EXISTS prevention_task (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "prevention_plan_id INT NOT NULL, " +
                                                        "day_offset INT NOT NULL, " +
                                                        "task_description VARCHAR(255) NOT NULL, " +
                                                        "status ENUM('PENDING','COMPLETED','MISSED') DEFAULT 'PENDING', "
                                                        +
                                                        "proof_photo_url VARCHAR(500), " +
                                                        "completed_at TIMESTAMP NULL, " +
                                                        "FOREIGN KEY (prevention_plan_id) REFERENCES prevention_plan(id) ON DELETE CASCADE)");

                        // Migration: backfill null start_dates (plans auto-start at creation)
                        executeQuietly(st,
                                        "UPDATE prevention_plan SET start_date = DATE(created_at) WHERE start_date IS NULL");

                        // 13. Review (Expert review requests)
                        executeCritical(st, "review",
                                        "CREATE TABLE IF NOT EXISTS review (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "diagnostic_id INT, " +
                                                        "treatment_plan_id INT, " +
                                                        "prevention_plan_id INT, " +
                                                        "review_type ENUM('DIAGNOSIS','PROGRESS','PREVENTION') NOT NULL, "
                                                        +
                                                        "status ENUM('PENDING','IN_PROGRESS','COMPLETED') DEFAULT 'PENDING', "
                                                        +
                                                        "photo_url VARCHAR(500), " +
                                                        "ai_analysis TEXT, " +
                                                        "ai_proposed_plan TEXT, " +
                                                        "expert_id INT, " +
                                                        "expert_notes TEXT, " +
                                                        "expert_verdict ENUM('HEALED','CONTINUE','WORSENED'), " +
                                                        "expert_disease_name VARCHAR(255), " +
                                                        "farmer_response ENUM('ACCEPTED','REJECTED','ACKNOWLEDGED'), " +
                                                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");

                        // Migration: add PREVENTION to review_type ENUM (for existing databases)
                        executeQuietly(st,
                                        "ALTER TABLE review MODIFY COLUMN review_type ENUM('DIAGNOSIS','PROGRESS','PREVENTION') NOT NULL");
                        // Migration: add prevention_plan_id column if missing
                        executeQuietly(st, "ALTER TABLE review ADD COLUMN prevention_plan_id INT");

                        // 6. Gamification: Badges
                        executeCritical(st, "badge",
                                        "CREATE TABLE IF NOT EXISTS badge (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "name VARCHAR(100) NOT NULL, " +
                                                        "description VARCHAR(255), " +
                                                        "icon VARCHAR(50), " +
                                                        "condition_type ENUM('DIAGNOSTIC', 'POINTS', 'HEALTHY_PLANTS', 'SOLUTION') DEFAULT 'DIAGNOSTIC', "
                                                        +
                                                        "threshold INT)");

                        // Migration: Update ENUM to include SOLUTION
                        executeQuietly(st,
                                        "ALTER TABLE badge MODIFY COLUMN condition_type ENUM('DIAGNOSTIC', 'POINTS', 'HEALTHY_PLANTS', 'SOLUTION') DEFAULT 'DIAGNOSTIC'");

                        // 7. Gamification: User_Badge
                        executeCritical(st, "user_badge",
                                        "CREATE TABLE IF NOT EXISTS user_badge (" +
                                                        "user_id INT, " +
                                                        "badge_id INT, " +
                                                        "acquired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "PRIMARY KEY(user_id, badge_id), " +
                                                        "FOREIGN KEY(user_id) REFERENCES `user`(id), " +
                                                        "FOREIGN KEY(badge_id) REFERENCES badge(id))");

                        // 8. Community: Posts
                        executeCritical(st, "community_posts",
                                        "CREATE TABLE IF NOT EXISTS community_posts (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "user_id INT NOT NULL, " +
                                                        "title VARCHAR(255) NOT NULL, " +
                                                        "description TEXT, " +
                                                        "image_url VARCHAR(500), " +
                                                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "likes INT DEFAULT 0, " +
                                                        "dislikes INT DEFAULT 0, " +
                                                        "is_resolved BOOLEAN DEFAULT FALSE, " +
                                                        "solution_comment_id INT, " +
                                                        "FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE)");

                        // 9. Community: Comments
                        executeCritical(st, "community_comments",
                                        "CREATE TABLE IF NOT EXISTS community_comments (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "post_id INT NOT NULL, " +
                                                        "user_id INT NOT NULL, " +
                                                        "content TEXT NOT NULL, " +
                                                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "likes INT DEFAULT 0, " +
                                                        "dislikes INT DEFAULT 0, " +
                                                        "is_solution BOOLEAN DEFAULT FALSE, " +
                                                        "parent_comment_id INT DEFAULT NULL, " +
                                                        "FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE, "
                                                        +
                                                        "FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE, "
                                                        +
                                                        "CONSTRAINT fk_parent_comment FOREIGN KEY (parent_comment_id) REFERENCES community_comments(id) ON DELETE CASCADE)");

                        // 10. Community: Likes
                        executeCritical(st, "community_likes",
                                        "CREATE TABLE IF NOT EXISTS community_likes (" +
                                                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                                        "user_id INT NOT NULL, " +
                                                        "post_id INT, " +
                                                        "comment_id INT, " +
                                                        "vote_type ENUM('LIKE', 'DISLIKE') NOT NULL DEFAULT 'LIKE', " +
                                                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                                        "FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE, "
                                                        +
                                                        "FOREIGN KEY (post_id) REFERENCES community_posts(id) ON DELETE CASCADE, "
                                                        +
                                                        "FOREIGN KEY (comment_id) REFERENCES community_comments(id) ON DELETE CASCADE, "
                                                        +
                                                        "CONSTRAINT unique_post_like UNIQUE (user_id, post_id), " +
                                                        "CONSTRAINT unique_comment_like UNIQUE (user_id, comment_id))");

                        // ── Marketplace tables ──

                        // ── Migrations for existing tables (failures are expected & non-fatal) ──

                        executeQuietly(st, "ALTER TABLE community_posts ADD COLUMN likes INT DEFAULT 0");
                        executeQuietly(st, "ALTER TABLE community_posts ADD COLUMN dislikes INT DEFAULT 0");
                        executeQuietly(st, "ALTER TABLE community_posts ADD COLUMN is_resolved BOOLEAN DEFAULT FALSE");
                        executeQuietly(st, "ALTER TABLE community_posts ADD COLUMN solution_comment_id INT");
                        executeQuietly(st, "ALTER TABLE community_comments ADD COLUMN dislikes INT DEFAULT 0");
                        executeQuietly(st,
                                        "ALTER TABLE community_comments ADD COLUMN is_solution BOOLEAN DEFAULT FALSE");

                        // Check for parent_comment_id in community_comments (needs FK too)
                        Set<String> communityCommentsColumns = new HashSet<>();
                        try (ResultSet rs = cnx.getMetaData().getColumns(null, null, "community_comments", null)) {
                                while (rs.next()) {
                                        communityCommentsColumns.add(rs.getString("COLUMN_NAME"));
                                }
                        }
                        if (!communityCommentsColumns.contains("parent_comment_id")) {
                                executeQuietly(st,
                                                "ALTER TABLE community_comments ADD COLUMN parent_comment_id INT DEFAULT NULL");
                                executeQuietly(st,
                                                "ALTER TABLE community_comments ADD CONSTRAINT fk_parent_comment FOREIGN KEY (parent_comment_id) REFERENCES community_comments(id) ON DELETE CASCADE");
                        }

                        // Migrate vote_type from INT to ENUM (for existing tables)
                        executeQuietly(st,
                                        "UPDATE community_likes SET vote_type = 'LIKE' WHERE vote_type = '1' OR vote_type = 1");
                        executeQuietly(st,
                                        "UPDATE community_likes SET vote_type = 'DISLIKE' WHERE vote_type = '-1' OR vote_type = -1");
                        executeQuietly(st,
                                        "ALTER TABLE community_likes MODIFY COLUMN vote_type ENUM('LIKE', 'DISLIKE') NOT NULL DEFAULT 'LIKE'");

                        // ── Seed default gamification badges ──
                        seedDefaultBadges(cnx);

                        // ── Seed default offers ──
                        seedDefaultOffers(cnx);

                        // System.out.println("Database schema initialized successfully.");

                } catch (SQLException e) {
                        System.err.println("Error initializing database schema: " + e.getMessage());
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

        /**
         * Seeds default gamification badges using the provided connection directly,
         * avoiding MyDatabase.getInstance() which would trigger recursive
         * initialization.
         */
        private static void seedDefaultBadges(Connection cnx) {
                try {
                        seedBadge(cnx, "Bienvenue", "Premier diagnostic réalisé", "🌱", "DIAGNOSTIC", 1);
                        seedBadge(cnx, "Explorateur", "10 diagnostics réalisés", "🔍", "DIAGNOSTIC", 10);
                        seedBadge(cnx, "Expert", "50 diagnostics réalisés", "🎓", "DIAGNOSTIC", 50);
                        seedBadge(cnx, "Fidèle", "100 points accumulés", "⭐", "POINTS", 100);
                        seedBadge(cnx, "Légendaire", "1000 points accumulés", "👑", "POINTS", 1000);
                        seedBadge(cnx, "Main Verte", "5 plantes saines diagnostiquées", "🌿", "HEALTHY_PLANTS", 5);
                        seedBadge(cnx, "Premier Secours", "Première solution acceptée", "🚑", "SOLUTION", 1);
                        seedBadge(cnx, "Guru", "5 solutions acceptées", "🧘", "SOLUTION", 5);
                        seedBadge(cnx, "Sage du Village", "10 solutions acceptées", "🦉", "SOLUTION", 10);
                        // System.out.println(" ✓ Default badges seeded.");
                } catch (SQLException e) {
                        // System.out.println(" (badge seeding skipped: " + e.getMessage() + ")");
                }
        }

        private static void seedBadge(Connection cnx, String name, String desc, String icon, String type, int threshold)
                        throws SQLException {
                String check = "SELECT COUNT(*) FROM badge WHERE name = ? AND condition_type = ? AND threshold = ?";
                try (PreparedStatement pst = cnx.prepareStatement(check)) {
                        pst.setString(1, name);
                        pst.setString(2, type);
                        pst.setInt(3, threshold);
                        ResultSet rs = pst.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0)
                                return;
                }
                String insert = "INSERT INTO badge (name, description, icon, condition_type, threshold) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pst = cnx.prepareStatement(insert)) {
                        pst.setString(1, name);
                        pst.setString(2, desc);
                        pst.setString(3, icon);
                        pst.setString(4, type);
                        pst.setInt(5, threshold);
                        pst.executeUpdate();
                }
        }

        private static void seedDefaultOffers(Connection cnx) {
                try {
                        // 1. Offre Express (Basic)
                        seedOffer(cnx, "Express", "Pour une analyse rapide et ponctuelle", 4.99f,
                                        "5 diagnostics/heure|Accès standard|Support par email", "#95a5a6", "#7f8c8d", 5,
                                        false, false);

                        // 2. Offre Premium (Standard)
                        seedOffer(cnx, "Premium", "L'offre idéale pour les passionnés", 19.99f,
                                        "20 diagnostics/heure|Accès aux traitements détaillés|Support prioritaire",
                                        "#1abc9c", "#16a085",
                                        20, true, true);

                        // 3. Offre VIP (Pro)
                        seedOffer(cnx, "VIP", "Solution complète pour les experts", 49.99f,
                                        "Diagnostics illimités|Accès prioritaire aux nouvelles fonctionnalités|Support dédié 24/7",
                                        "#34495e", "#2c3e50", -1, true, true);

                        // System.out.println(" ✓ Default offers seeded.");
                } catch (SQLException e) {
                        // System.out.println(" (offer seeding skipped: " + e.getMessage() + ")");
                }
        }

        private static void seedOffer(Connection cnx, String nom, String description, float prix, String avantages,
                        String color1, String color2, int diagPerPro, boolean accesTraitement,
                        boolean accesPlanTraitement) throws SQLException {
                String check = "SELECT COUNT(*) FROM offre WHERE nom = ?";
                try (PreparedStatement pst = cnx.prepareStatement(check)) {
                        pst.setString(1, nom);
                        ResultSet rs = pst.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0)
                                return;
                }

                String insert = "INSERT INTO offre (nom, description, prix_mensuel, avantages, couleur_primaire, couleur_secondaire, diagnostics_par_heure, acces_traitement, acces_plan_traitement, est_active, est_recommandee) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pst = cnx.prepareStatement(insert)) {
                        pst.setString(1, nom);
                        pst.setString(2, description);
                        pst.setFloat(3, prix);
                        pst.setString(4, avantages);
                        pst.setString(5, color1);
                        pst.setString(6, color2);
                        pst.setInt(7, diagPerPro);
                        pst.setBoolean(8, accesTraitement);
                        pst.setBoolean(9, accesPlanTraitement);
                        pst.setBoolean(10, true); // Active par défaut
                        pst.setBoolean(11, "Premium".equals(nom)); // Recommande Premium par défaut
                        pst.executeUpdate();
                }
        }
}