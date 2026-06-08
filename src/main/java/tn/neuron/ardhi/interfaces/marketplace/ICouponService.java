package tn.neuron.ardhi.interfaces.marketplace;

import tn.neuron.ardhi.models.marketplace.Coupon;

import java.util.List;

public interface ICouponService {

    List<Coupon> getAllCoupons();

    boolean addCoupon(Coupon coupon);

    boolean updateCoupon(Coupon coupon);

    boolean deleteCoupon(int idCoupon);

    /**
     * Chercher un coupon par son code.
     * 
     * @return le Coupon trouvé, ou null si inexistant
     */
    Coupon findByCode(String code);

    /**
     * Valider un coupon pour un utilisateur et un montant donné.
     * 
     * @return null si le coupon est valide, sinon un message d'erreur explicatif
     */
    String validerCoupon(Coupon coupon, double montant, int idUser);

    /**
     * Calculer le montant de la réduction selon le type (pourcentage ou montant
     * fixe).
     */
    double calculateReduction(Coupon coupon, double montant);

    /**
     * Incrémenter les compteurs d'utilisation (global + par utilisateur).
     * 
     * @return true si l'incrémentation a réussi
     */
    boolean incrementUsage(Coupon coupon, int idUser);
}
