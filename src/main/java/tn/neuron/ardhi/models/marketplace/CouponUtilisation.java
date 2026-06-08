package tn.neuron.ardhi.models.marketplace;

public class CouponUtilisation {
    private int id;
    private int idCoupon;
    private int idUser;
    private int nombreUtilisation;


    public CouponUtilisation(int id, int idCoupon, int idUser, int nombreUtilisation) {
        this.id = id;
        this.idCoupon = idCoupon;
        this.idUser = idUser;
        this.nombreUtilisation = nombreUtilisation;
    }
    public CouponUtilisation() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdCoupon() {
        return idCoupon;
    }

    public void setIdCoupon(int idCoupon) {
        this.idCoupon = idCoupon;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public int getNombreUtilisation() {
        return nombreUtilisation;
    }

    public void setNombreUtilisation(int nombreUtilisation) {
        this.nombreUtilisation = nombreUtilisation;
    }

    @Override
    public String toString() {
        return "CouponUtilisation{" +
                "id=" + id +
                ", idCoupon=" + idCoupon +
                ", idUser=" + idUser +
                ", nombreUtilisation=" + nombreUtilisation +
                '}';
    }
}
