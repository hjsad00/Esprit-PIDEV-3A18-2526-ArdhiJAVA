package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.interfaces.IService;
import tn.neuron.ardhi.models.UserAndDiag.CommunityPost;
import tn.neuron.ardhi.models.UserAndDiag.VoteType;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;
import tn.neuron.ardhi.utils.UserAndDiag.UserSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommunityPostService implements IService<CommunityPost> {

    private Connection cnx;
    private final GamificationService gamificationService = new GamificationService();
    private final ProfanityFilterService profanityFilterService = new ProfanityFilterService();

    public CommunityPostService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    /**
     * Converts a vote_type from the database (ENUM string) to an int value.
     * 
     * @param voteTypeStr The string from DB ('LIKE', 'DISLIKE', or null)
     * @return 1 for LIKE, -1 for DISLIKE, 0 for null/no vote
     */
    private int voteTypeToInt(String voteTypeStr) {
        if (voteTypeStr == null || voteTypeStr.trim().isEmpty())
            return 0;
        try {
            VoteType vt = VoteType.fromString(voteTypeStr.trim());
            if (vt == VoteType.LIKE)
                return 1;
            if (vt == VoteType.DISLIKE)
                return -1;
        } catch (Exception e) {
            // Log error or ignore
        }
        return 0;
    }

    @Override
    public void ajouter(CommunityPost post) throws SQLException {
        if (profanityFilterService.containsProfanity(post.getTitle()) ||
                profanityFilterService.containsProfanity(post.getDescription())) {
            throw new IllegalArgumentException("Ce contenu contient un langage inapproprié.");
        }

        String sql = "INSERT INTO community_posts (user_id, title, description, image_url) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, post.getUserId());
            ps.setString(2, post.getTitle());
            ps.setString(3, post.getDescription());
            ps.setString(4, post.getImageUrl());
            ps.executeUpdate();
        }

        // Gamification: Award points for creating a post
        try {
            gamificationService.addPoints(post.getUserId(), 10);
        } catch (Exception e) {
            // Non-critical: don't fail post creation if gamification errors
        }
    }

    @Override
    public void modifier(CommunityPost post) throws SQLException {
        if (profanityFilterService.containsProfanity(post.getTitle()) ||
                profanityFilterService.containsProfanity(post.getDescription())) {
            throw new IllegalArgumentException("Ce contenu contient un langage inapproprié.");
        }

        // First, check if the post is being set to unresolved
        boolean wasResolved = isPostResolved(post.getId());
        boolean willBeUnresolved = !post.isResolved();

        String sql = "UPDATE community_posts SET title = ?, description = ?, image_url = ?, is_resolved = ?, user_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getDescription());
            ps.setString(3, post.getImageUrl());
            ps.setBoolean(4, post.isResolved());
            ps.setInt(5, post.getUserId());
            ps.setInt(6, post.getId());
            ps.executeUpdate();
        }

        // If the post was resolved and is now being set to unresolved,
        // clear all solution comments and reset solution_comment_id
        if (wasResolved && willBeUnresolved) {
            clearAllSolutionComments(post.getId());
        }
    }

    /**
     * Checks if a post is currently marked as resolved.
     */
    private boolean isPostResolved(int postId) throws SQLException {
        String sql = "SELECT is_resolved FROM community_posts WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_resolved");
            }
        }
        return false;
    }

    /**
     * Clears all solution comments for a post and resets the solution_comment_id.
     * Called when a post is manually set to unresolved.
     */
    private void clearAllSolutionComments(int postId) throws SQLException {
        // 1. Unmark all solution comments for this post
        String clearComments = "UPDATE community_comments SET is_solution = FALSE WHERE post_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(clearComments)) {
            ps.setInt(1, postId);
            ps.executeUpdate();
        }

        // 2. Clear the solution_comment_id on the post
        String clearPost = "UPDATE community_posts SET solution_comment_id = NULL WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(clearPost)) {
            ps.setInt(1, postId);
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM community_posts WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<CommunityPost> recuperer() throws SQLException {
        int userId = 0;
        try {
            tn.neuron.ardhi.models.UserAndDiag.User u = UserSession.getInstance().getUser();
            if (u != null)
                userId = u.getId();
        } catch (Exception e) {
            // Ignore if no session
        }
        return getAllPosts(userId);
    }

    // Custom method to get posts with context (likes/comments count)
    public List<CommunityPost> getAllPosts(int currentUserId) throws SQLException {
        List<CommunityPost> posts = new ArrayList<>();
        String sql = "SELECT p.*, u.nom, u.prenom, " +
                "(SELECT COUNT(*) FROM community_comments c WHERE c.post_id = p.id) as comment_count, " +
                "l.vote_type as my_vote " +
                "FROM community_posts p " +
                "JOIN user u ON p.user_id = u.id " +
                "LEFT JOIN community_likes l ON p.id = l.post_id AND l.user_id = ? " +
                "ORDER BY p.created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CommunityPost post = mapRowToPost(rs);
                post.setUserVote(voteTypeToInt(rs.getString("my_vote")));
                posts.add(post);
            }
        }
        return posts;
    }

    @Override
    public List<CommunityPost> rechercher(String keyword) throws SQLException {
        int currentUserId = 0;
        try {
            tn.neuron.ardhi.models.UserAndDiag.User u = UserSession.getInstance().getUser();
            if (u != null)
                currentUserId = u.getId();
        } catch (Exception e) {
        }

        List<CommunityPost> posts = new ArrayList<>();
        String sql = "SELECT p.*, u.nom, u.prenom, " +
                "(SELECT COUNT(*) FROM community_comments c WHERE c.post_id = p.id) as comment_count, " +
                "l.vote_type as my_vote " +
                "FROM community_posts p " +
                "JOIN user u ON p.user_id = u.id " +
                "LEFT JOIN community_likes l ON p.id = l.post_id AND l.user_id = ? " +
                "WHERE LOWER(p.title) LIKE ? OR LOWER(p.description) LIKE ? " +
                "ORDER BY p.created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            String pattern = "%" + keyword.toLowerCase() + "%";
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CommunityPost post = mapRowToPost(rs);
                post.setUserVote(voteTypeToInt(rs.getString("my_vote")));
                posts.add(post);
            }
        }
        return posts;
    }

    private CommunityPost mapRowToPost(ResultSet rs) throws SQLException {
        CommunityPost post = new CommunityPost(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("prenom") + " " + rs.getString("nom"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("image_url"),
                rs.getTimestamp("created_at"),
                rs.getInt("likes"),
                rs.getInt("dislikes"),
                rs.getBoolean("is_resolved"),
                rs.getInt("solution_comment_id"));
        post.setCommentCount(rs.getInt("comment_count"));
        return post;
    }

    // --- Voting Logic ---
    public int toggleLikePost(int userId, int postId) throws SQLException {
        return handleVote(userId, postId, VoteType.LIKE);
    }

    public int toggleDislikePost(int userId, int postId) throws SQLException {
        return handleVote(userId, postId, VoteType.DISLIKE);
    }

    private int handleVote(int userId, int postId, VoteType newVoteType) throws SQLException {
        String checkSql = "SELECT vote_type FROM community_likes WHERE user_id = ? AND post_id = ?";
        VoteType currentVote = null; // null = no vote exists

        try (PreparedStatement ps = cnx.prepareStatement(checkSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, postId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentVote = VoteType.fromString(rs.getString("vote_type"));
            }
        }

        if (currentVote == newVoteType) {
            // User clicked same vote type again -> remove vote
            String delSql = "DELETE FROM community_likes WHERE user_id = ? AND post_id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(delSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, postId);
                ps.executeUpdate();
            }

            // Decrement the appropriate counter
            String colToDec = (newVoteType == VoteType.LIKE) ? "likes" : "dislikes";
            updateCount(colToDec, -1, postId);
            return 0; // No vote

        } else if (currentVote == null) {
            // No existing vote -> insert new vote
            String insSql = "INSERT INTO community_likes (user_id, post_id, vote_type) VALUES (?, ?, ?)";
            try (PreparedStatement ps = cnx.prepareStatement(insSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, postId);
                ps.setString(3, newVoteType.name()); // Store as 'LIKE' or 'DISLIKE'
                ps.executeUpdate();
            }
            String colToInc = (newVoteType == VoteType.LIKE) ? "likes" : "dislikes";
            updateCount(colToInc, 1, postId);
            return (newVoteType == VoteType.LIKE) ? 1 : -1;

        } else {
            // Switching vote type
            String updSql = "UPDATE community_likes SET vote_type = ? WHERE user_id = ? AND post_id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(updSql)) {
                ps.setString(1, newVoteType.name()); // Store as 'LIKE' or 'DISLIKE'
                ps.setInt(2, userId);
                ps.setInt(3, postId);
                ps.executeUpdate();
            }
            String oldCol = (currentVote == VoteType.LIKE) ? "likes" : "dislikes";
            String newCol = (newVoteType == VoteType.LIKE) ? "likes" : "dislikes";
            updateCount(oldCol, -1, postId);
            updateCount(newCol, 1, postId);
            return (newVoteType == VoteType.LIKE) ? 1 : -1;
        }
    }

    private void updateCount(String column, int delta, int id) throws SQLException {
        if (delta == 0)
            return;
        String sign = (delta > 0) ? "+" : "-";
        String absDelta = String.valueOf(Math.abs(delta));
        String sql = "UPDATE community_posts SET " + column + " = " + column + " " + sign + " " + absDelta
                + " WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void markPostAsResolved(int postId, int commentId) throws SQLException {
        // 1. Get author of the solution comment
        int authorId = 0;
        String getAuthor = "SELECT user_id FROM community_comments WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(getAuthor)) {
            ps.setInt(1, commentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                authorId = rs.getInt("user_id");
        }

        String sqlPost = "UPDATE community_posts SET is_resolved = TRUE, solution_comment_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sqlPost)) {
            ps.setInt(1, commentId);
            ps.setInt(2, postId);
            ps.executeUpdate();
        }

        // 3. Award points if author found
        if (authorId > 0) {
            gamificationService.addPoints(authorId, 50);
            gamificationService.checkSolutionBadges(authorId);
        }
    }
}
