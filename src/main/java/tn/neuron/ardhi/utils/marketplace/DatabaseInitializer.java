package tn.neuron.ardhi.utils.marketplace;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    private DatabaseInitializer() {
        // Private constructor to prevent instantiation
    }

    public static void initialize(Connection cnx) {
        if (cnx == null)
            return;

        try (Statement st = cnx.createStatement()) {

            // ── Marketplace tables ──

            // 11. Marketplace: Produits
            executeCritical(st, "produits",
                    "CREATE TABLE IF NOT EXISTS produits (" +
                            "idProduit INT AUTO_INCREMENT PRIMARY KEY, " +
                            "nom VARCHAR(255) NOT NULL, " +
                            "description TEXT, " +
                            "prix DECIMAL(12,2) NOT NULL, " +
                            "quantiteStock INT NOT NULL DEFAULT 0, " +
                            "categorie VARCHAR(100) NOT NULL, " +
                            "id_user INT NOT NULL, " +
                            "device ENUM('TND', 'EUR', 'USD') DEFAULT 'TND', " +
                            "uniteMesure ENUM('KG', 'G', 'L', 'ML', 'UNITE', 'TONNE') DEFAULT 'KG', " +
                            "image VARCHAR(500), " +
                            "CONSTRAINT fk_produit_user FOREIGN KEY (id_user) REFERENCES `user`(id) ON DELETE CASCADE)");

            // 12. Marketplace: Commande
            executeCritical(st, "commande",
                    "CREATE TABLE IF NOT EXISTS commande (" +
                            "idCommande INT AUTO_INCREMENT PRIMARY KEY, " +
                            "date_commande DATE NOT NULL, " +
                            "etat VARCHAR(50) NOT NULL DEFAULT 'en_cours', " +
                            "total DECIMAL(12,2) NOT NULL DEFAULT 0.00, " +
                            "id_user INT NOT NULL, " +
                            "CONSTRAINT fk_commande_user FOREIGN KEY (id_user) REFERENCES `user`(id) ON DELETE CASCADE)");

            // 13. Marketplace: DetailsCommande
            executeCritical(st, "detailscommande",
                    "CREATE TABLE IF NOT EXISTS detailscommande (" +
                            "idDetails INT AUTO_INCREMENT PRIMARY KEY, " +
                            "id_commande INT NOT NULL, " +
                            "id_produit INT NOT NULL, " +
                            "quantite INT NOT NULL DEFAULT 1, " +
                            "prix_unitaire DECIMAL(12,2) NOT NULL, " +
                            "CONSTRAINT fk_details_commande FOREIGN KEY (id_commande) REFERENCES commande(idCommande) ON DELETE CASCADE, "
                            +
                            "CONSTRAINT fk_details_produit FOREIGN KEY (id_produit) REFERENCES produits(idProduit) ON DELETE CASCADE)");

            // 14. Marketplace: Avis
            executeCritical(st, "avis",
                    "CREATE TABLE IF NOT EXISTS avis (" +
                            "idAvis INT AUTO_INCREMENT PRIMARY KEY, " +
                            "id_user INT NOT NULL, " +
                            "id_produit INT NOT NULL, " +
                            "note INT NOT NULL CHECK (note BETWEEN 1 AND 5), " +
                            "commentaire TEXT, " +
                            "dateAvis TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "CONSTRAINT fk_avis_user FOREIGN KEY (id_user) REFERENCES `user`(id) ON DELETE CASCADE, " +
                            "CONSTRAINT fk_avis_produit FOREIGN KEY (id_produit) REFERENCES produits(idProduit) ON DELETE CASCADE)");

            // 15. Marketplace: Panier
            executeCritical(st, "panier",
                    "CREATE TABLE IF NOT EXISTS panier (" +
                            "idPanier INT AUTO_INCREMENT PRIMARY KEY, " +
                            "dateCreation DATE NOT NULL, " +
                            "totalMontant DECIMAL(12,2) NOT NULL DEFAULT 0.00, " +
                            "totalProduits INT NOT NULL DEFAULT 0, " +
                            "id_user INT NOT NULL, " +
                            "CONSTRAINT fk_panier_user FOREIGN KEY (id_user) REFERENCES `user`(id) ON DELETE CASCADE)");

            // 16. Marketplace: Panier_Produit
            executeCritical(st, "panier_produits",
                    "CREATE TABLE IF NOT EXISTS panier_produits (" +
                            "id_panier INT NOT NULL, " +
                            "id_produit INT NOT NULL, " +
                            "quantite INT NOT NULL DEFAULT 1, " +
                            "PRIMARY KEY (id_panier, id_produit), " +
                            "CONSTRAINT fk_panierproduit_panier FOREIGN KEY (id_panier) REFERENCES panier(idPanier) ON DELETE CASCADE, "
                            +
                            "CONSTRAINT fk_panierproduit_produit FOREIGN KEY (id_produit) REFERENCES produits(idProduit) ON DELETE CASCADE)");

            // 17. Marketplace: Coupon
            executeCritical(st, "coupon",
                    "CREATE TABLE IF NOT EXISTS coupon (" +
                            "idCoupon INT AUTO_INCREMENT PRIMARY KEY, " +
                            "code VARCHAR(50) NOT NULL UNIQUE, " +
                            "typeReduction ENUM('POURCENTAGE', 'FIXE') NOT NULL, " +
                            "valeur DOUBLE NOT NULL, " +
                            "dateDebut DATE NOT NULL, " +
                            "dateFin DATE NOT NULL, " +
                            "utilisationMax INT NOT NULL DEFAULT 0, " +
                            "utilisationActuelle INT NOT NULL DEFAULT 0, " +
                            "actif BOOLEAN NOT NULL DEFAULT TRUE, " +
                            "montantMin DOUBLE NOT NULL DEFAULT 0, " +
                            "limiteParUser INT NOT NULL DEFAULT 1)");

            // 18. Marketplace: Coupon_Utilisation
            executeCritical(st, "coupon_utilisation",
                    "CREATE TABLE IF NOT EXISTS coupon_utilisation (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "id_coupon INT NOT NULL, " +
                            "id_user INT NOT NULL, " +
                            "nombreUtilisation INT NOT NULL DEFAULT 0, " +
                            "CONSTRAINT fk_couponutilisation_coupon FOREIGN KEY (id_coupon) REFERENCES coupon(idCoupon) ON DELETE CASCADE, "
                            +
                            "CONSTRAINT fk_couponutilisation_user FOREIGN KEY (id_user) REFERENCES `user`(id) ON DELETE CASCADE, " +
                            "UNIQUE KEY unique_coupon_user (id_coupon, id_user))");

            // 19. Marketplace: Reclamation
            executeCritical(st, "reclamation",
                    "CREATE TABLE IF NOT EXISTS reclamation (" +
                            "idReclamation INT AUTO_INCREMENT PRIMARY KEY, " +
                            "description TEXT NOT NULL, " +
                            "type ENUM('QUALITE_RECOLTE', 'PRODUIT_AVARIE', 'QUANTITE_INCORRECTE', 'PRIX_NON_CONFORME', 'PRODUIT_NON_CONFORME', 'RETARD_LIVRAISON', 'AUTRE') NOT NULL, "
                            +
                            "statut ENUM('EN_ATTENTE', 'EN_COURS', 'RESOLUE', 'REJETEE') NOT NULL DEFAULT 'EN_ATTENTE', "
                            +
                            "dateReclamation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "nomProduit VARCHAR(255), " +
                            "id_produit INT NOT NULL, " +
                            "id_user INT NOT NULL, " +
                            "CONSTRAINT fk_reclamation_produit FOREIGN KEY (id_produit) REFERENCES produits(idProduit) ON DELETE CASCADE, "
                            +
                            "CONSTRAINT fk_reclamation_user FOREIGN KEY (id_user) REFERENCES `user`(id) ON DELETE CASCADE)");

            // ── Index pour optimisation des performances (Marketplace) ──

            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_details_id_commande ON detailscommande(id_commande)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_details_id_produit ON detailscommande(id_produit)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_avis_id_produit ON avis(id_produit)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_reclamation_id_produit ON reclamation(id_produit)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_produit_id_user ON produits(id_user)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_commande_id_user ON commande(id_user)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_avis_id_user ON avis(id_user)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_panier_id_user ON panier(id_user)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_reclamation_id_user ON reclamation(id_user)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_produit_categorie ON produits(categorie)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_coupon_actif ON coupon(actif)");
            executeQuietly(st, "CREATE INDEX IF NOT EXISTS idx_coupon_code ON coupon(code)");

            // System.out.println("Marketplace database schema initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Error initializing marketplace database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeCritical(Statement st, String tableName, String sql) {
        try {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("  ✗ FAILED to create table '" + tableName + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeQuietly(Statement st, String sql) {
        try {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            // Expected for migrations (column/constraint already exists) — silently ignore
        }
    }
}
