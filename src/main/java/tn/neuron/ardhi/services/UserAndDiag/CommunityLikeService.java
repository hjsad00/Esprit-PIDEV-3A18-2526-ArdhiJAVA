package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.interfaces.IService;
import tn.neuron.ardhi.models.UserAndDiag.CommunityLike;
import tn.neuron.ardhi.models.UserAndDiag.VoteType;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing CommunityLike entities (votes on posts and comments).
 */
public class CommunityLikeService implements IService<CommunityLike> {

    private Connection cnx;

    public CommunityLikeService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void ajouter(CommunityLike like) throws SQLException {
        String sql = "INSERT INTO community_likes (user_id, post_id, comment_id, vote_type) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, like.getUserId());
            if (like.getPostId() != null) {
                ps.setInt(2, like.getPostId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            if (like.getCommentId() != null) {
                ps.setInt(3, like.getCommentId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, like.getVoteType().name());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                like.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public void modifier(CommunityLike like) throws SQLException {
        String sql = "UPDATE community_likes SET user_id = ?, post_id = ?, comment_id = ?, vote_type = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, like.getUserId());
            if (like.getPostId() != null) {
                ps.setInt(2, like.getPostId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            if (like.getCommentId() != null) {
                ps.setInt(3, like.getCommentId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, like.getVoteType().name());
            ps.setInt(5, like.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM community_likes WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Get a specific like by ID (not part of interface).
     */
    public CommunityLike afficher(int id) throws SQLException {
        String sql = "SELECT l.*, u.nom, u.prenom FROM community_likes l " +
                "JOIN user u ON l.user_id = u.id WHERE l.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToLike(rs);
            }
        }
        return null;
    }

    @Override
    public List<CommunityLike> recuperer() throws SQLException {
        List<CommunityLike> likes = new ArrayList<>();
        String sql = "SELECT l.*, u.nom, u.prenom FROM community_likes l " +
                "JOIN user u ON l.user_id = u.id " +
                "ORDER BY l.created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                likes.add(mapRowToLike(rs));
            }
        }
        return likes;
    }

    @Override
    public List<CommunityLike> rechercher(String keyword) throws SQLException {
        List<CommunityLike> likes = new ArrayList<>();
        String sql = "SELECT l.*, u.nom, u.prenom FROM community_likes l " +
                "JOIN user u ON l.user_id = u.id " +
                "WHERE LOWER(u.nom) LIKE ? OR LOWER(u.prenom) LIKE ? " +
                "OR l.vote_type LIKE ? " +
                "OR CAST(l.user_id AS CHAR) LIKE ? " +
                "OR CAST(l.post_id AS CHAR) LIKE ? " +
                "OR CAST(l.comment_id AS CHAR) LIKE ? " +
                "ORDER BY l.created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            for (int i = 1; i <= 6; i++) {
                ps.setString(i, pattern);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                likes.add(mapRowToLike(rs));
            }
        }
        return likes;
    }

    /**
     * Get all votes for a specific post.
     */
    public List<CommunityLike> getVotesForPost(int postId) throws SQLException {
        List<CommunityLike> likes = new ArrayList<>();
        String sql = "SELECT l.*, u.nom, u.prenom FROM community_likes l " +
                "JOIN user u ON l.user_id = u.id " +
                "WHERE l.post_id = ? ORDER BY l.created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                likes.add(mapRowToLike(rs));
            }
        }
        return likes;
    }

    /**
     * Get all votes for a specific comment.
     */
    public List<CommunityLike> getVotesForComment(int commentId) throws SQLException {
        List<CommunityLike> likes = new ArrayList<>();
        String sql = "SELECT l.*, u.nom, u.prenom FROM community_likes l " +
                "JOIN user u ON l.user_id = u.id " +
                "WHERE l.comment_id = ? ORDER BY l.created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                likes.add(mapRowToLike(rs));
            }
        }
        return likes;
    }

    /**
     * Get all votes by a specific user.
     */
    public List<CommunityLike> getVotesByUser(int userId) throws SQLException {
        List<CommunityLike> likes = new ArrayList<>();
        String sql = "SELECT l.*, u.nom, u.prenom FROM community_likes l " +
                "JOIN user u ON l.user_id = u.id " +
                "WHERE l.user_id = ? ORDER BY l.created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                likes.add(mapRowToLike(rs));
            }
        }
        return likes;
    }

    private CommunityLike mapRowToLike(ResultSet rs) throws SQLException {
        CommunityLike like = new CommunityLike();
        like.setId(rs.getInt("id"));
        like.setUserId(rs.getInt("user_id"));

        int postId = rs.getInt("post_id");
        like.setPostId(rs.wasNull() ? null : postId);

        int commentId = rs.getInt("comment_id");
        like.setCommentId(rs.wasNull() ? null : commentId);

        like.setVoteType(VoteType.fromString(rs.getString("vote_type")));
        like.setCreatedAt(rs.getTimestamp("created_at"));

        // User name
        String nom = rs.getString("nom");
        String prenom = rs.getString("prenom");
        like.setUserName((prenom != null ? prenom : "") + " " + (nom != null ? nom : ""));

        return like;
    }
}
