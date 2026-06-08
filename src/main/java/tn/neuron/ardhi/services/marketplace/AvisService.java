package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.models.marketplace.Avis;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import tn.neuron.ardhi.interfaces.marketplace.IAvisService;

public class AvisService implements IAvisService {

    private Connection connection;
    private NotificationMarketService notificationService; // ✅ AJOUT

    public AvisService() {
        connection = MyDatabase.getInstance().getCnx();
        notificationService = new NotificationMarketService(); // ✅ AJOUT
    }

    public boolean ajouterAvis(Avis avis) {
        String query = "INSERT INTO avis (id_user, id_produit, note, commentaire) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, avis.getIdUser());
            pstmt.setInt(2, avis.getIdProduit());
            pstmt.setInt(3, avis.getNote());
            pstmt.setString(4, avis.getCommentaire());

            boolean succes = pstmt.executeUpdate() > 0;

            // ✅ AJOUT : notifier le vendeur du produit si l'avis est inséré
            if (succes) {
                notifierVendeurAvis(avis);
            }

            return succes;

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de l'avis : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupère le vendeur du produit et envoie la notification d'avis.
     */
    private void notifierVendeurAvis(Avis avis) {
        try {
            // 1. Récupérer idUser (vendeur) et nom du produit
            String sqlProduit = "SELECT id_user, nom FROM produits WHERE idProduit = ?";
            try (PreparedStatement pst = connection.prepareStatement(sqlProduit)) {
                pst.setInt(1, avis.getIdProduit());
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) return;

                    int idVendeur   = rs.getInt("id_user");
                    String nomProduit = rs.getString("nom");

                    // 2. Ne pas notifier si l'auteur est le vendeur lui-même
                    if (idVendeur == avis.getIdUser()) return;

                    // 3. Récupérer le nom de l'auteur de l'avis
                    String nomAuteur = getNomUser(avis.getIdUser());

                    // 4. Envoyer la notification
                    notificationService.notifierNouvelAvis(
                            idVendeur,
                            avis.getIdProduit(),
                            nomProduit,
                            nomAuteur,
                            avis.getNote()
                    );
                }
            }
        } catch (Exception e) {
            // La notification ne doit jamais faire échouer l'ajout de l'avis
            System.err.println("[AvisService] Erreur notification avis : " + e.getMessage());
        }
    }

    /**
     * Retourne le prénom + nom d'un utilisateur depuis son id.
     */
    private String getNomUser(int idUser) {
        String sql = "SELECT nom, prenom FROM user WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, idUser);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("prenom") + " " + rs.getString("nom");
                }
            }
        } catch (SQLException e) {
            System.err.println("[AvisService] getNomUser : " + e.getMessage());
        }
        return "Un utilisateur";
    }

    // ── Méthodes existantes inchangées ───────────────────────────────────────

    public List<Avis> getAvisByProduit(int idProduit) {
        List<Avis> avisList = new ArrayList<>();
        String query = "SELECT a.*, u.nom, u.prenom FROM avis a " +
                "JOIN user u ON a.id_user = u.id " +
                "WHERE a.id_produit = ? ORDER BY a.dateAvis DESC";

        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, idProduit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Avis avis = new Avis(
                        rs.getInt("idAvis"),
                        rs.getInt("id_user"),
                        rs.getInt("id_produit"),
                        rs.getInt("note"),
                        rs.getString("commentaire"),
                        rs.getTimestamp("dateAvis"));
                String nomComplet = rs.getString("nom") + " " + rs.getString("prenom");
                avis.setNomUser(nomComplet);
                avisList.add(avis);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des avis : " + e.getMessage());
            e.printStackTrace();
        }
        return avisList;
    }

    public double getNoteMoyenne(int idProduit) {
        String query = "SELECT AVG(note) as moyenne FROM avis WHERE id_produit = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, idProduit);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("moyenne");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du calcul de la moyenne : " + e.getMessage());
        }
        return 0.0;
    }

    public int getNombreAvis(int idProduit) {
        String query = "SELECT COUNT(*) as nombre FROM avis WHERE id_produit = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, idProduit);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("nombre");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage des avis : " + e.getMessage());
        }
        return 0;
    }
}