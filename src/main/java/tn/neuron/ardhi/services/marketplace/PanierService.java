package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.interfaces.marketplace.IPanierService;
import tn.neuron.ardhi.interfaces.marketplace.IProduitService;
import tn.neuron.ardhi.models.marketplace.Panier;
import tn.neuron.ardhi.models.marketplace.PanierProduit;
import tn.neuron.ardhi.models.marketplace.Produit;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PanierService implements IPanierService {

    private Connection connection;
    private IProduitService produitService;

    public PanierService() {
        this.connection = MyDatabase.getInstance().getCnx();
        this.produitService = new ProduitService();
    }

    @Override
    public Panier getPanierActif(int idUser) {
        String querySelect = "SELECT * FROM panier WHERE id_user = ? ORDER BY dateCreation DESC LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(querySelect)) {
            pstmt.setInt(1, idUser);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractPanierFromResultSet(rs);
                } else {
                    return creerPanier(idUser);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du panier : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private Panier creerPanier(int idUser) {
        String query = "INSERT INTO panier (dateCreation, totalMontant, totalProduits, id_user) VALUES (?, 0, 0, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setDate(1, Date.valueOf(LocalDate.now()));
            pstmt.setInt(2, idUser);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Panier panier = new Panier();
                        panier.setIdPanier(generatedKeys.getInt(1));
                        panier.setIdUser(idUser);
                        panier.setDateCreation(LocalDate.now());
                        panier.setTotalMontant(0);
                        panier.setTotalProduits(0);
                        return panier;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création du panier : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean ajouterProduit(int idPanier, int idProduit, int quantite) {
        if (!produitService.verifierStock(idProduit, quantite)) {
            System.err.println("Stock insuffisant pour ce produit");
            return false;
        }

        String checkQuery = "SELECT quantite FROM panier_produits WHERE id_panier = ? AND id_produit = ?";

        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setInt(1, idPanier);
            checkStmt.setInt(2, idProduit);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    int nouvelleQuantite = rs.getInt("quantite") + quantite;
                    return updateQuantite(idPanier, idProduit, nouvelleQuantite);
                } else {
                    return insererProduitPanier(idPanier, idProduit, quantite);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout au panier : " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private boolean insererProduitPanier(int idPanier, int idProduit, int quantite) {
        String query = "INSERT INTO panier_produits (id_panier, id_produit, quantite) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idPanier);
            pstmt.setInt(2, idProduit);
            pstmt.setInt(3, quantite);

            boolean result = pstmt.executeUpdate() > 0;

            if (result) {
                mettreAJourTotaux(idPanier);
            }

            return result;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion du produit : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateQuantite(int idPanier, int idProduit, int nouvelleQuantite) {
        if (nouvelleQuantite <= 0) {
            return supprimerProduit(idPanier, idProduit);
        }

        if (!produitService.verifierStock(idProduit, nouvelleQuantite)) {
            System.err.println("Stock insuffisant");
            return false;
        }

        String query = "UPDATE panier_produits SET quantite = ? WHERE id_panier = ? AND id_produit = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, nouvelleQuantite);
            pstmt.setInt(2, idPanier);
            pstmt.setInt(3, idProduit);

            boolean result = pstmt.executeUpdate() > 0;

            if (result) {
                mettreAJourTotaux(idPanier);
            }

            return result;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<PanierProduit> getProduitsParPanier(int idPanier) {
        List<PanierProduit> produitsPanier = new ArrayList<>();
        String query = "SELECT * FROM panier_produits WHERE id_panier = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idPanier);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PanierProduit pp = new PanierProduit(
                            rs.getInt("id_panier"),
                            rs.getInt("id_produit"),
                            rs.getInt("quantite"));

                    Produit produit = produitService.getProduitById(pp.getIdProduit());
                    pp.setProduit(produit);

                    produitsPanier.add(pp);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des produits : " + e.getMessage());
            e.printStackTrace();
        }

        return produitsPanier;
    }

    @Override
    public boolean supprimerProduit(int idPanier, int idProduit) {
        String query = "DELETE FROM panier_produits WHERE id_panier = ? AND id_produit = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idPanier);
            pstmt.setInt(2, idProduit);

            boolean result = pstmt.executeUpdate() > 0;

            if (result) {
                mettreAJourTotaux(idPanier);
            }

            return result;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean viderPanier(int idPanier) {
        String query = "DELETE FROM panier_produits WHERE id_panier = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idPanier);
            boolean result = pstmt.executeUpdate() >= 0;

            if (result) {
                mettreAJourTotaux(idPanier);
            }

            return result;
        } catch (SQLException e) {
            System.err.println("Erreur lors du vidage du panier : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public float calculerTotal(int idPanier) {
        Panier panier = getPanierById(idPanier);
        return panier != null ? panier.getTotalMontant() : 0;
    }

    @Override
    public int compterProduits(int idPanier) {
        Panier panier = getPanierById(idPanier);
        return panier != null ? panier.getTotalProduits() : 0;
    }

    private void mettreAJourTotaux(int idPanier) {
        String queryCalcul = "SELECT " +
                "COALESCE(SUM(pp.quantite * p.prix), 0) as montant, " +
                "COALESCE(SUM(pp.quantite), 0) as nombre " +
                "FROM panier_produits pp " +
                "JOIN produits p ON pp.id_produit = p.idProduit " +
                "WHERE pp.id_panier = ?";

        String queryUpdate = "UPDATE panier SET totalMontant = ?, totalProduits = ? WHERE idPanier = ?";

        try (PreparedStatement pstmtCalcul = connection.prepareStatement(queryCalcul);
                PreparedStatement pstmtUpdate = connection.prepareStatement(queryUpdate)) {

            pstmtCalcul.setInt(1, idPanier);

            try (ResultSet rs = pstmtCalcul.executeQuery()) {
                if (rs.next()) {
                    float montant = rs.getFloat("montant");
                    int nombre = rs.getInt("nombre");

                    pstmtUpdate.setFloat(1, montant);
                    pstmtUpdate.setInt(2, nombre);
                    pstmtUpdate.setInt(3, idPanier);
                    pstmtUpdate.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour des totaux : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Panier getPanierById(int idPanier) {
        String query = "SELECT * FROM panier WHERE idPanier = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idPanier);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractPanierFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du panier : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private Panier extractPanierFromResultSet(ResultSet rs) throws SQLException {
        return new Panier(
                rs.getInt("idPanier"),
                rs.getDate("dateCreation").toLocalDate(),
                rs.getFloat("totalMontant"),
                rs.getInt("totalProduits"),
                rs.getInt("id_user"));
    }
}