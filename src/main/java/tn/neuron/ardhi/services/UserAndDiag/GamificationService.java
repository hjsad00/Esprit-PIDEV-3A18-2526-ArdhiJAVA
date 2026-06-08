package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.User;
import tn.neuron.ardhi.models.UserAndDiag.BadgeType;
import tn.neuron.ardhi.utils.UserAndDiag.LogUtils;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GamificationService {

    private final Connection cnx;

    public GamificationService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    public void addPoints(int userId, int amount) {
        try {
            // 1. Update Points
            String updateQuery = "UPDATE user SET points = points + ? WHERE id = ?";
            try (PreparedStatement pst = cnx.prepareStatement(updateQuery)) {
                pst.setInt(1, amount);
                pst.setInt(2, userId);
                pst.executeUpdate();
            }

            // 2. Check Level Up (Every 1000 points = 1 level, broadly)
            // Or simpler: Level = 1 + Points / 500
            String levelQuery = "UPDATE user SET level = 1 + FLOOR(points / 500) WHERE id = ?";
            try (PreparedStatement pst = cnx.prepareStatement(levelQuery)) {
                pst.setInt(1, userId);
                pst.executeUpdate();
            }

            // 3. Check Point-based Badges
            checkPointBadges(userId);

        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error adding points", e);
        }
    }

    public void checkDiagnosticBadges(int userId) {
        try {
            // Count diagnostics
            String countQuery = "SELECT COUNT(*) FROM diagnostic WHERE user_id = ?";
            int count = 0;
            try (PreparedStatement pst = cnx.prepareStatement(countQuery)) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if (rs.next())
                    count = rs.getInt(1);
            }

            checkBadges(userId, BadgeType.DIAGNOSTIC.name(), count);

        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error checking diagnostic badges", e);
        }
    }

    private void checkPointBadges(int userId) {
        try {
            // Get current points
            String ptQuery = "SELECT points FROM user WHERE id = ?";
            int points = 0;
            try (PreparedStatement pst = cnx.prepareStatement(ptQuery)) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if (rs.next())
                    points = rs.getInt(1);
            }

            checkBadges(userId, BadgeType.POINTS.name(), points);
        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error checking point badges", e);
        }
    }

    public void checkHealthyBadges(int userId) {
        try {
            // Count healthy plants
            // Assuming 'saine' is the keyword in resultat_ia for healthy plants
            String countQuery = "SELECT COUNT(*) FROM diagnostic WHERE user_id = ? AND LOWER(resultat_ia) LIKE '%saine%'";
            int count = 0;
            try (PreparedStatement pst = cnx.prepareStatement(countQuery)) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if (rs.next())
                    count = rs.getInt(1);
            }

            checkBadges(userId, BadgeType.HEALTHY_PLANTS.name(), count);
        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error checking healthy badges", e);
        }
    }

    public void checkSolutionBadges(int userId) {
        try {
            // Count accepted solutions
            String countQuery = "SELECT COUNT(*) FROM community_comments WHERE user_id = ?";
            int count = 0;
            try (PreparedStatement pst = cnx.prepareStatement(countQuery)) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if (rs.next())
                    count = rs.getInt(1);
            }

            checkBadges(userId, BadgeType.SOLUTION.name(), count);
        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error checking solution badges", e);
        }
    }

    private void checkBadges(int userId, String type, int value) throws SQLException {
        // Find badges of this type that the user meets threshold for BUT doesn't have
        // yet
        String query = "SELECT id FROM badge " +
                "WHERE condition_type = ? AND threshold <= ? " +
                "AND id NOT IN (SELECT badge_id FROM user_badge WHERE user_id = ?)";

        List<Integer> newBadges = new ArrayList<>();
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, type);
            pst.setInt(2, value);
            pst.setInt(3, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                newBadges.add(rs.getInt("id"));
            }
        }

        // Award new badges
        for (int badgeId : newBadges) {
            // Double-check to prevent race conditions or duplicates
            if (!hasBadge(userId, badgeId)) {
                String insert = "INSERT INTO user_badge (user_id, badge_id, acquired_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
                try (PreparedStatement pst = cnx.prepareStatement(insert)) {
                    pst.setInt(1, userId);
                    pst.setInt(2, badgeId);
                    pst.executeUpdate();

                    // Fetch badge name for clearer logging
                    String nameQuery = "SELECT label FROM badge WHERE id = ?";
                    String badgeName = "Unknown";
                    try (PreparedStatement namePst = cnx.prepareStatement(nameQuery)) {
                        namePst.setInt(1, badgeId);
                        ResultSet nameRs = namePst.executeQuery();
                        if (nameRs.next())
                            badgeName = nameRs.getString("label");
                    }
                    LogUtils.info(this.getClass(),
                            "Badge Unlocked! User: " + userId + " Badge: " + badgeName + " (ID: " + badgeId + ")");
                }
            }
        }
    }

    // Helper to check if user already has a specific badge
    public boolean hasBadge(int userId, int badgeId) {
        String query = "SELECT 1 FROM user_badge WHERE user_id = ? AND badge_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, badgeId);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error checking badge existence", e);
            return false;
        }
    }

    public List<User> getLeaderboard(int limit) {
        List<User> topUsers = new ArrayList<>();
        String query = "SELECT id, nom, prenom, points, level FROM user WHERE role != 'ADMIN' ORDER BY points DESC LIMIT ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, limit);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setPoints(rs.getInt("points"));
                u.setLevel(rs.getInt("level"));
                topUsers.add(u);
            }
        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error fetching leaderboard", e);
        }
        return topUsers;
    }

    public User getUserStats(int userId) {
        User u = new User();
        u.setId(userId);
        String query = "SELECT points, level FROM user WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                u.setPoints(rs.getInt("points"));
                u.setLevel(rs.getInt("level"));
            }
        } catch (SQLException e) {
            LogUtils.error(this.getClass(), "Error fetching user stats", e);
        }
        return u;
    }
}
