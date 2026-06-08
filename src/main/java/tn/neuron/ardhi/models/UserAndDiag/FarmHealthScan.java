package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Date;
import java.sql.Timestamp;

public class FarmHealthScan {
    private int id;
    private int userId;
    private String cropType;
    private Date plantingDate;
    private String growthStage;
    private Double latitude;
    private Double longitude;
    private String concerns;
    // 6 photo URLs (uploaded to ImgBB)
    private String photoCrops;
    private String photoSoil;
    private String photoEdges;
    private String photoInsects;
    private String photoSpacing;
    private String photoOverview;
    private Timestamp scanDate;
    private ScanStatus status;

    public FarmHealthScan() {
        this.status = ScanStatus.PENDING;
    }

    public FarmHealthScan(int userId, String cropType, Date plantingDate, String growthStage) {
        this.userId = userId;
        this.cropType = cropType;
        this.plantingDate = plantingDate;
        this.growthStage = growthStage;
        this.status = ScanStatus.PENDING;
    }

    // Getters & Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCropType() {
        return cropType;
    }

    public void setCropType(String cropType) {
        this.cropType = cropType;
    }

    public Date getPlantingDate() {
        return plantingDate;
    }

    public void setPlantingDate(Date plantingDate) {
        this.plantingDate = plantingDate;
    }

    public String getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getConcerns() {
        return concerns;
    }

    public void setConcerns(String concerns) {
        this.concerns = concerns;
    }

    public String getPhotoCrops() {
        return photoCrops;
    }

    public void setPhotoCrops(String photoCrops) {
        this.photoCrops = photoCrops;
    }

    public String getPhotoSoil() {
        return photoSoil;
    }

    public void setPhotoSoil(String photoSoil) {
        this.photoSoil = photoSoil;
    }

    public String getPhotoEdges() {
        return photoEdges;
    }

    public void setPhotoEdges(String photoEdges) {
        this.photoEdges = photoEdges;
    }

    public String getPhotoInsects() {
        return photoInsects;
    }

    public void setPhotoInsects(String photoInsects) {
        this.photoInsects = photoInsects;
    }

    public String getPhotoSpacing() {
        return photoSpacing;
    }

    public void setPhotoSpacing(String photoSpacing) {
        this.photoSpacing = photoSpacing;
    }

    public String getPhotoOverview() {
        return photoOverview;
    }

    public void setPhotoOverview(String photoOverview) {
        this.photoOverview = photoOverview;
    }

    public Timestamp getScanDate() {
        return scanDate;
    }

    public void setScanDate(Timestamp scanDate) {
        this.scanDate = scanDate;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public void setStatus(ScanStatus status) {
        this.status = status;
    }
}
