package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.models.marketplace.NotificationMarket;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationMarketService {

    private final Connection connection;

    public NotificationMarketService() {
        this.connection = MyDatabase.getInstance().getCnx();
        creerTableSiAbsente();
    }

    // ── Création de la table si elle n'existe pas ────────────────────────────
    private void creerTableSiAbsente() {
        String sql = """
                CREATE TABLE IF NOT EXISTS notif_market (
                    id_notif      INT AUTO_INCREMENT PRIMARY KEY,
                    id_user       INT          NOT NULL,
                    type          VARCHAR(50)  NOT NULL,
                    titre         VARCHAR(255) NOT NULL,
                    message       TEXT         NOT NULL,
                    id_produit    INT,
                    id_commande   INT,
                    lue           BOOLEAN      DEFAULT FALSE,
                    date_creation TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (id_user)    REFERENCES user(id)                   ON DELETE CASCADE,
                    FOREIGN KEY (id_produit) REFERENCES produits(idProduit)         ON DELETE SET NULL,
                    FOREIGN KEY (id_commande) REFERENCES commande(idCommande)       ON DELETE SET NULL
                )
                """;
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("[NotificationMarketService] creerTableSiAbsente : " + e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // CRÉER une notification
    // ────────────────────────────────────────────────────────────────────────
    public boolean creerNotification(NotificationMarket notif) {
        String sql = """
                INSERT INTO notif_market (id_user, type, titre, message, id_produit, id_commande)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, notif.getIdUser());
            pst.setString(2, notif.getType());
            pst.setString(3, notif.getTitre());
            pst.setString(4, notif.getMessage());

            if (notif.getIdProduit() != null) pst.setInt(5, notif.getIdProduit());
            else                               pst.setNull(5, Types.INTEGER);

            if (notif.getIdCommande() != null) pst.setInt(6, notif.getIdCommande());
            else                               pst.setNull(6, Types.INTEGER);

            int rows = pst.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) notif.setIdNotif(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[NotificationMarketService] creerNotification : " + e.getMessage());
        }
        return false;
    }

    // ────────────────────────────────────────────────────────────────────────
    // RACCOURCIS MÉTIER
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Notifie un vendeur qu'une commande vient d'être passée sur ses produits.
     *
     * @param idVendeur   id du vendeur destinataire
     * @param idCommande  id de la commande créée
     * @param idProduit   id d'un produit représentatif (premier produit du vendeur)
     * @param nomAcheteur prénom ou nom de l'acheteur (pour personnaliser le message)
     * @param total       montant total de la sous-commande
     */
    public void notifierNouvelleCommande(int idVendeur, int idCommande,
                                         int idProduit, String nomAcheteur, float total) {
        NotificationMarket notif = new NotificationMarket(
                idVendeur,
                NotificationMarket.TYPE_ACHAT,
                "🛒 Nouvelle commande reçue",
                String.format("%s vient de passer une commande (#%d) pour un total de %.2f DT.",
                        nomAcheteur, idCommande, total),
                idProduit,
                idCommande
        );
        creerNotification(notif);
    }

    /**
     * Notifie un vendeur qu'un avis a été déposé sur l'un de ses produits.
     *
     * @param idVendeur    id du vendeur destinataire
     * @param idProduit    id du produit concerné
     * @param nomProduit   nom du produit (pour le message)
     * @param nomAuteur    prénom/nom de l'auteur de l'avis
     * @param note         note donnée (sur 5)
     */
    public void notifierNouvelAvis(int idVendeur, int idProduit,
                                   String nomProduit, String nomAuteur, int note) {
        String etoiles = "⭐".repeat(Math.max(1, Math.min(note, 5)));
        NotificationMarket notif = new NotificationMarket(
                idVendeur,
                NotificationMarket.TYPE_AVIS,
                "⭐ Nouvel avis sur votre produit",
                String.format("%s a laissé un avis %s sur \"%s\".", nomAuteur, etoiles, nomProduit),
                idProduit
        );
        creerNotification(notif);
    }

    // ────────────────────────────────────────────────────────────────────────
    // LIRE les notifications d'un vendeur
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Retourne toutes les notifications d'un vendeur, les non-lues en premier.
     */
    public List<NotificationMarket> getNotificationsParVendeur(int idVendeur) {
        List<NotificationMarket> liste = new ArrayList<>();
        String sql = """
                SELECT * FROM notif_market
                WHERE id_user = ?
                ORDER BY lue ASC, date_creation DESC
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) liste.add(extraire(rs));
            }
        } catch (SQLException e) {
            System.err.println("[NotificationMarketService] getNotificationsParVendeur : " + e.getMessage());
        }
        return liste;
    }

    /**
     * Retourne uniquement les notifications non lues d'un vendeur.
     */
    public List<NotificationMarket> getNonLuesParVendeur(int idVendeur) {
        List<NotificationMarket> liste = new ArrayList<>();
        String sql = """
                SELECT * FROM notif_market
                WHERE id_user = ? AND lue = FALSE
                ORDER BY date_creation DESC
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) liste.add(extraire(rs));
            }
        } catch (SQLException e) {
            System.err.println("[NotificationMarketService] getNonLuesParVendeur : " + e.getMessage());
        }
        return liste;
    }

    /**
     * Compte les notifications non lues d'un vendeur (pour badge UI).
     */
    public int compterNonLues(int idVendeur) {
        String sql = "SELECT COUNT(*) FROM notif_market WHERE id_user = ? AND lue = FALSE";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[NotificationMarketService] compterNonLues : " + e.getMessage());
        }
        return 0;
    }

    // ────────────────────────────────────────────────────────────────────────
    // MARQUER comme lue
    // ────────────────────────────────────────────────────────────────────────

    /** Marque une notification précise comme lue. */
    public boolean marquerCommeLue(int idNotif) {
        String sql = "UPDATE notif_market SET lue = TRUE WHERE id_notif = ?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idNotif);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NotificationMarketService] marquerCommeLue : " + e.getMessage());
        }
        return false;
    }

    /** Marque toutes les notifications d'un vendeur comme lues. */
    public boolean marquerToutesCommeLues(int idVendeur) {
        String sql = "UPDATE notif_market SET lue = TRUE WHERE id_user = ?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idVendeur);
            return pst.executeUpdate() >= 0;
        } catch (SQLException e) {
            System.err.println("[NotificationMarketService] marquerToutesCommeLues : " + e.getMessage());
        }
        return false;
    }

    // ────────────────────────────────────────────────────────────────────────
    // SUPPRIMER
    // ────────────────────────────────────────────────────────────────────────

    public boolean supprimerNotification(int idNotif) {
        String sql = "DELETE FROM notif_market WHERE id_notif = ?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idNotif);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NotificationMarketService] supprimerNotification : " + e.getMessage());
        }
        return false;
    }

    // ────────────────────────────────────────────────────────────────────────
    // HELPER : extraction ResultSet → objet
    // ────────────────────────────────────────────────────────────────────────
    private NotificationMarket extraire(ResultSet rs) throws SQLException {
        NotificationMarket n = new NotificationMarket();
        n.setIdNotif(rs.getInt("id_notif"));
        n.setIdUser(rs.getInt("id_user"));
        n.setType(rs.getString("type"));
        n.setTitre(rs.getString("titre"));
        n.setMessage(rs.getString("message"));
        n.setLue(rs.getBoolean("lue"));

        int idProduit = rs.getInt("id_produit");
        if (!rs.wasNull()) n.setIdProduit(idProduit);

        int idCommande = rs.getInt("id_commande");
        if (!rs.wasNull()) n.setIdCommande(idCommande);

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) n.setDateCreation(ts.toLocalDateTime());

        return n;
    }
}