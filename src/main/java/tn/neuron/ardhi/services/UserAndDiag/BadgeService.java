package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.interfaces.IService;
import tn.neuron.ardhi.models.UserAndDiag.Badge;
import tn.neuron.ardhi.models.UserAndDiag.BadgeType;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BadgeService implements IService<Badge> {

    private final Connection cnx;

    public BadgeService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void ajouter(Badge b) throws SQLException {
        String query = "INSERT INTO badge (name, description, icon, condition_type, threshold) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, b.getName());
            pst.setString(2, b.getDescription());
            pst.setString(3, b.getIcon());
            pst.setString(4, b.getConditionType().name());
            pst.setInt(5, b.getThreshold());
            pst.executeUpdate();
        }
    }

    @Override
    public void modifier(Badge b) throws SQLException {
        String query = "UPDATE badge SET name = ?, description = ?, icon = ?, condition_type = ?, threshold = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, b.getName());
            pst.setString(2, b.getDescription());
            pst.setString(3, b.getIcon());
            pst.setString(4, b.getConditionType().name());
            pst.setInt(5, b.getThreshold());
            pst.setInt(6, b.getId());
            pst.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // First delete entries in user_badge to maintain referential integrity
        String deleteRef = "DELETE FROM user_badge WHERE badge_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(deleteRef)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }

        String query = "DELETE FROM badge WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        }
    }

    @Override
    public List<Badge> recuperer() throws SQLException {
        List<Badge> badges = new ArrayList<>();
        String query = "SELECT * FROM badge";
        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Badge b = new Badge();
                b.setId(rs.getInt("id"));
                b.setName(rs.getString("name"));
                b.setDescription(rs.getString("description"));
                b.setIcon(rs.getString("icon"));
                try {
                    b.setConditionType(BadgeType.valueOf(rs.getString("condition_type")));
                } catch (IllegalArgumentException e) {
                    // Fallback or log error for invalid enum value
                    b.setConditionType(BadgeType.POINTS);
                }
                b.setThreshold(rs.getInt("threshold"));
                badges.add(b);
            }
        }
        return badges;
    }

    @Override
    public List<Badge> rechercher(String nom) throws SQLException {
        List<Badge> badges = new ArrayList<>();
        String query = "SELECT * FROM badge WHERE name LIKE ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, "%" + nom + "%");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Badge b = new Badge();
                b.setId(rs.getInt("id"));
                b.setName(rs.getString("name"));
                b.setDescription(rs.getString("description"));
                b.setIcon(rs.getString("icon"));
                try {
                    b.setConditionType(BadgeType.valueOf(rs.getString("condition_type")));
                } catch (IllegalArgumentException e) {
                    b.setConditionType(BadgeType.POINTS);
                }
                b.setThreshold(rs.getInt("threshold"));
                badges.add(b);
            }
        }
        return badges;
    }

    // New search by Type method
    public List<Badge> rechercherParType(BadgeType type) throws SQLException {
        List<Badge> badges = new ArrayList<>();
        String query = "SELECT * FROM badge WHERE condition_type = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, type.name());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Badge b = new Badge();
                b.setId(rs.getInt("id"));
                b.setName(rs.getString("name"));
                b.setDescription(rs.getString("description"));
                b.setIcon(rs.getString("icon"));
                b.setConditionType(BadgeType.valueOf(rs.getString("condition_type")));
                b.setThreshold(rs.getInt("threshold"));
                badges.add(b);
            }
        }
        return badges;
    }

    // Helper to check for default badges (migration logic)
    public void initDefaultBadges() {
        try {
            ensureBadge("Bienvenue", "Premier diagnostic réalisé", "🌱", BadgeType.DIAGNOSTIC, 1);
            ensureBadge("Explorateur", "10 diagnostics réalisés", "🔍", BadgeType.DIAGNOSTIC, 10);
            ensureBadge("Expert", "50 diagnostics réalisés", "🎓", BadgeType.DIAGNOSTIC, 50);
            ensureBadge("Fidèle", "100 points accumulés", "⭐", BadgeType.POINTS, 100);
            ensureBadge("Légendaire", "1000 points accumulés", "👑", BadgeType.POINTS, 1000);
            ensureBadge("Main Verte", "5 plantes saines diagnostiquées", "🌿", BadgeType.HEALTHY_PLANTS, 5);

            // Badges Solutions
            ensureBadge("Premier Secours", "Première solution acceptée", "🚑", BadgeType.SOLUTION, 1);
            ensureBadge("Guru", "5 solutions acceptées", "🧘", BadgeType.SOLUTION, 5);
            ensureBadge("Sage du Village", "10 solutions acceptées", "🦉", BadgeType.SOLUTION, 10);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ensureBadge(String name, String desc, String icon, BadgeType type, int threshold)
            throws SQLException {
        // Check if badge exists
        String checkQuery = "SELECT COUNT(*) FROM badge WHERE name = ? AND condition_type = ? AND threshold = ?";
        try (PreparedStatement pst = cnx.prepareStatement(checkQuery)) {
            pst.setString(1, name);
            pst.setString(2, type.name());
            pst.setInt(3, threshold);
            ResultSet rs = pst.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Badge exists
            }
        }

        // Add if missing
        Badge b = new Badge(name, desc, icon, type, threshold);
        ajouter(b);
    }
}
