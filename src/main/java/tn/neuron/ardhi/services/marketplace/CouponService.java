package tn.neuron.ardhi.services.marketplace;

import tn.neuron.ardhi.interfaces.marketplace.ICouponService;
import tn.neuron.ardhi.models.marketplace.Coupon;
import tn.neuron.ardhi.models.marketplace.CouponUtilisation;
import tn.neuron.ardhi.models.marketplace.TypeReduction;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CouponService implements ICouponService {

    private Connection connection;

    public CouponService() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    @Override
    public List<Coupon> getAllCoupons() {
        List<Coupon> coupons = new ArrayList<>();
        String query = "SELECT * FROM coupon";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                coupons.add(extractCouponFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des coupons : " + e.getMessage());
            e.printStackTrace();
        }
        return coupons;
    }

    @Override
    public boolean addCoupon(Coupon coupon) {
        String query = "INSERT INTO coupon (code, typeReduction, valeur, dateDebut, dateFin, utilisationMax, utilisationActuelle, actif, montantMin, limiteParUser) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, coupon.getCode());
            pstmt.setString(2, coupon.getTypeReduction().name());
            pstmt.setDouble(3, coupon.getValeur());
            pstmt.setDate(4, coupon.getDateDebut());
            pstmt.setDate(5, coupon.getDateFin());
            pstmt.setInt(6, coupon.getUtilisationMax());
            pstmt.setInt(7, coupon.getUtilisationActuelle());
            pstmt.setBoolean(8, coupon.isActif());
            pstmt.setDouble(9, coupon.getMontantMin());
            pstmt.setInt(10, coupon.getLimiteParUser());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du coupon : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateCoupon(Coupon coupon) {
        String query = "UPDATE coupon SET code = ?, typeReduction = ?, valeur = ?, dateDebut = ?, dateFin = ?, utilisationMax = ?, actif = ?, montantMin = ?, limiteParUser = ? WHERE idCoupon = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, coupon.getCode());
            pstmt.setString(2, coupon.getTypeReduction().name());
            pstmt.setDouble(3, coupon.getValeur());
            pstmt.setDate(4, coupon.getDateDebut());
            pstmt.setDate(5, coupon.getDateFin());
            pstmt.setInt(6, coupon.getUtilisationMax());
            pstmt.setBoolean(7, coupon.isActif());
            pstmt.setDouble(8, coupon.getMontantMin());
            pstmt.setInt(9, coupon.getLimiteParUser());
            pstmt.setInt(10, coupon.getIdCoupon());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du coupon : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteCoupon(int idCoupon) {
        String query = "DELETE FROM coupon WHERE idCoupon = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idCoupon);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du coupon : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Coupon findByCode(String code) {
        String query = "SELECT * FROM coupon WHERE code = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractCouponFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche du coupon : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String validerCoupon(Coupon coupon, double montant, int idUser) {
        // 1. Vérifier que le coupon est actif
        if (!coupon.isActif()) {
            return "Ce coupon est inactif.";
        }

        // 2. Vérifier les dates de validité
        LocalDate aujourdHui = LocalDate.now();
        LocalDate dateDebut = coupon.getDateDebut().toLocalDate();
        LocalDate dateFin = coupon.getDateFin().toLocalDate();

        if (aujourdHui.isBefore(dateDebut)) {
            return "Ce coupon n'est pas encore valide. Il sera actif à partir du " + dateDebut + ".";
        }
        if (aujourdHui.isAfter(dateFin)) {
            return "Ce coupon a expiré le " + dateFin + ".";
        }

        // 3. Vérifier le montant minimum
        if (montant < coupon.getMontantMin()) {
            return String.format(
                    "Le montant minimum requis pour ce coupon est de %.2f DT. Votre total actuel est de %.2f DT.",
                    coupon.getMontantMin(), montant);
        }

        // 4. Vérifier la limite globale d'utilisation
        if (coupon.getUtilisationActuelle() >= coupon.getUtilisationMax()) {
            return "Ce coupon a atteint sa limite maximale d'utilisation.";
        }

        // 5. Vérifier la limite par utilisateur
        if (coupon.getLimiteParUser() > 0) {
            int utilisationsUser = getNombreUtilisationsUser(coupon.getIdCoupon(), idUser);
            if (utilisationsUser >= coupon.getLimiteParUser()) {
                return "Vous avez déjà utilisé ce coupon le nombre maximum de fois autorisé ("
                        + coupon.getLimiteParUser() + ").";
            }
        }

        // Toutes les conditions sont validées
        return null;
    }

    @Override
    public double calculateReduction(Coupon coupon, double montant) {
        if (coupon.getTypeReduction() == TypeReduction.POURCENTAGE) {
            return montant * (coupon.getValeur() / 100.0);
        } else if (coupon.getTypeReduction() == TypeReduction.MONTANT_FIXE) {
            // La réduction ne peut pas dépasser le montant total
            return Math.min(coupon.getValeur(), montant);
        }
        return 0;
    }

    @Override
    public boolean incrementUsage(Coupon coupon, int idUser) {
        try {
            // 1. Incrémenter le compteur global dans la table coupon
            String updateCoupon = "UPDATE coupon SET utilisationActuelle = utilisationActuelle + 1 WHERE idCoupon = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateCoupon)) {
                pstmt.setInt(1, coupon.getIdCoupon());
                int rows = pstmt.executeUpdate();
                if (rows == 0) {
                    return false;
                }
            }

            // 2. Incrémenter ou créer l'entrée dans coupon_utilisation
            CouponUtilisation utilisation = getUtilisation(coupon.getIdCoupon(), idUser);

            if (utilisation != null) {
                // L'entrée existe déjà, incrémenter
                String updateUtil = "UPDATE coupon_utilisation SET nombreUtilisation = nombreUtilisation + 1 WHERE id_coupon = ? AND id_user = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(updateUtil)) {
                    pstmt.setInt(1, coupon.getIdCoupon());
                    pstmt.setInt(2, idUser);
                    pstmt.executeUpdate();
                }
            } else {
                // Créer une nouvelle entrée
                String insertUtil = "INSERT INTO coupon_utilisation (id_coupon, id_user, nombreUtilisation) VALUES (?, ?, 1)";
                try (PreparedStatement pstmt = connection.prepareStatement(insertUtil)) {
                    pstmt.setInt(1, coupon.getIdCoupon());
                    pstmt.setInt(2, idUser);
                    pstmt.executeUpdate();
                }
            }

            return true;

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'incrémentation de l'utilisation du coupon : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ─── Méthodes utilitaires privées ──────────────────────────────────

    private int getNombreUtilisationsUser(int idCoupon, int idUser) {
        String query = "SELECT nombreUtilisation FROM coupon_utilisation WHERE id_coupon = ? AND id_user = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idCoupon);
            pstmt.setInt(2, idUser);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("nombreUtilisation");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification des utilisations : " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    private CouponUtilisation getUtilisation(int idCoupon, int idUser) {
        String query = "SELECT * FROM coupon_utilisation WHERE id_coupon = ? AND id_user = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, idCoupon);
            pstmt.setInt(2, idUser);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new CouponUtilisation(
                            rs.getInt("id"),
                            rs.getInt("id_coupon"),
                            rs.getInt("id_user"),
                            rs.getInt("nombreUtilisation"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'utilisation : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private Coupon extractCouponFromResultSet(ResultSet rs) throws SQLException {
        Coupon coupon = new Coupon();
        coupon.setIdCoupon(rs.getInt("idCoupon"));
        coupon.setCode(rs.getString("code"));
        coupon.setTypeReduction(TypeReduction.valueOf(rs.getString("typeReduction")));
        coupon.setValeur(rs.getDouble("valeur"));
        coupon.setDateDebut(rs.getDate("dateDebut"));
        coupon.setDateFin(rs.getDate("dateFin"));
        coupon.setUtilisationMax(rs.getInt("utilisationMax"));
        coupon.setUtilisationActuelle(rs.getInt("utilisationActuelle"));
        coupon.setActif(rs.getBoolean("actif"));
        coupon.setMontantMin(rs.getDouble("montantMin"));
        coupon.setLimiteParUser(rs.getInt("limiteParUser"));
        return coupon;
    }
}
