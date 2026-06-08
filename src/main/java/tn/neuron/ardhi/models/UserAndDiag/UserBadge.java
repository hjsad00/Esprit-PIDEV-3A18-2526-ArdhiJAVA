package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Timestamp;

public class UserBadge {
    private int userId;
    private int badgeId;
    private Timestamp acquiredAt;

    // Transient fields for display (fetched via JOIN)
    private String userName;
    private String userEmail;
    private String badgeName;
    private String badgeIcon;
    private String badgeDescription;

    public UserBadge() {
    }

    public UserBadge(int userId, int badgeId, Timestamp acquiredAt) {
        this.userId = userId;
        this.badgeId = badgeId;
        this.acquiredAt = acquiredAt;
    }

    // Constructor with details
    public UserBadge(int userId, String userName, String userEmail, int badgeId, String badgeName, String badgeIcon,
            String badgeDescription,
            Timestamp acquiredAt) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.badgeId = badgeId;
        this.badgeName = badgeName;
        this.badgeIcon = badgeIcon;
        this.badgeDescription = badgeDescription;
        this.acquiredAt = acquiredAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(int badgeId) {
        this.badgeId = badgeId;
    }

    public Timestamp getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(Timestamp acquiredAt) {
        this.acquiredAt = acquiredAt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getBadgeName() {
        return badgeName;
    }

    public void setBadgeName(String badgeName) {
        this.badgeName = badgeName;
    }

    public String getBadgeIcon() {
        return badgeIcon;
    }

    public void setBadgeIcon(String badgeIcon) {
        this.badgeIcon = badgeIcon;
    }

    public String getBadgeDescription() {
        return badgeDescription;
    }

    public void setBadgeDescription(String badgeDescription) {
        this.badgeDescription = badgeDescription;
    }
}
