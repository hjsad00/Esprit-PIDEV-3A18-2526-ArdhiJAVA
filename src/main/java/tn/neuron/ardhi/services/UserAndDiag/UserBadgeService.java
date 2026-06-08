package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.models.UserAndDiag.UserBadge;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserBadgeService {

    private final Connection cnx;

    public UserBadgeService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    public void attribuerBadge(int userId, int badgeId) throws SQLException {
        // Check if already exists
        if (aDejaBadge(userId, badgeId)) {
            throw new SQLException("Cet utilisateur possède déjà ce badge.");
        }

        String query = "INSERT INTO user_badge (user_id, badge_id, acquired_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, badgeId);
            pst.executeUpdate();
        }
    }

    public void retirerBadge(int userId, int badgeId) throws SQLException {
        String query = "DELETE FROM user_badge WHERE user_id = ? AND badge_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, badgeId);
            pst.executeUpdate();
        }
    }

    public boolean aDejaBadge(int userId, int badgeId) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_badge WHERE user_id = ? AND badge_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, badgeId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public List<UserBadge> recupererTout() throws SQLException {
        List<UserBadge> list = new ArrayList<>();
        String query = "SELECT ub.user_id, ub.badge_id, ub.acquired_at, " +
                "u.nom, u.prenom, u.email, " +
                "b.name, b.icon, b.description " +
                "FROM user_badge ub " +
                "JOIN user u ON ub.user_id = u.id " +
                "JOIN badge b ON ub.badge_id = b.id " +
                "ORDER BY ub.acquired_at DESC";

        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                String fullName = rs.getString("nom") + " " + rs.getString("prenom");
                list.add(new UserBadge(
                        rs.getInt("user_id"),
                        fullName,
                        rs.getString("email"),
                        rs.getInt("badge_id"),
                        rs.getString("name"),
                        rs.getString("icon"),
                        rs.getString("description"),
                        rs.getTimestamp("acquired_at")));
            }
        }
        return list;
    }

    public List<UserBadge> rechercher(String keyword) throws SQLException {
        List<UserBadge> list = new ArrayList<>();
        String query = "SELECT ub.user_id, ub.badge_id, ub.acquired_at, " +
                "u.nom, u.prenom, u.email, " +
                "b.name, b.icon, b.description " +
                "FROM user_badge ub " +
                "JOIN user u ON ub.user_id = u.id " +
                "JOIN badge b ON ub.badge_id = b.id " +
                "WHERE u.email LIKE ? OR b.name LIKE ? " +
                "ORDER BY ub.acquired_at DESC";

        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            String pattern = "%" + keyword + "%";
            pst.setString(1, pattern);
            pst.setString(2, pattern);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String fullName = rs.getString("nom") + " " + rs.getString("prenom");
                    list.add(new UserBadge(
                            rs.getInt("user_id"),
                            fullName,
                            rs.getString("email"),
                            rs.getInt("badge_id"),
                            rs.getString("name"),
                            rs.getString("icon"),
                            rs.getString("description"),
                            rs.getTimestamp("acquired_at")));
                }
            }
        }
        return list;
    }

    public List<UserBadge> getBadgesForUser(int userId) throws SQLException {
        List<UserBadge> list = new ArrayList<>();
        String query = "SELECT ub.user_id, ub.badge_id, ub.acquired_at, " +
                "u.nom, u.prenom, u.email, " +
                "b.name, b.icon, b.description " +
                "FROM user_badge ub " +
                "JOIN user u ON ub.user_id = u.id " +
                "JOIN badge b ON ub.badge_id = b.id " +
                "WHERE ub.user_id = ? " +
                "ORDER BY ub.acquired_at DESC";

        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String fullName = rs.getString("nom") + " " + rs.getString("prenom");
                    list.add(new UserBadge(
                            rs.getInt("user_id"),
                            fullName,
                            rs.getString("email"),
                            rs.getInt("badge_id"),
                            rs.getString("name"),
                            rs.getString("icon"),
                            rs.getString("description"),
                            rs.getTimestamp("acquired_at")));
                }
            }
        }
        return list;
    }
}
