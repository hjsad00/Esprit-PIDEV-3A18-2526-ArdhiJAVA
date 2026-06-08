package tn.neuron.ardhi.services.UserAndDiag;

import tn.neuron.ardhi.interfaces.IService;
import tn.neuron.ardhi.models.UserAndDiag.CommunityComment;
import tn.neuron.ardhi.models.UserAndDiag.VoteType;
import tn.neuron.ardhi.utils.UserAndDiag.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommunityCommentService implements IService<CommunityComment> {

    private Connection cnx;
    private final GamificationService gamificationService = new GamificationService();
    private final ProfanityFilterService profanityFilterService = new ProfanityFilterService();

    public CommunityCommentService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    /**
     * Converts a vote_type from the database (ENUM string) to an int value.
     * 
     * @param voteTypeStr The string from DB ('LIKE', 'DISLIKE', or null)
     * @return 1 for LIKE, -1 for DISLIKE, 0 for null/no vote
     */
    private int voteTypeToInt(String voteTypeStr) {
        if (voteTypeStr == null)
            return 0;
        VoteType vt = VoteType.fromString(voteTypeStr);
        if (vt == VoteType.LIKE)
            return 1;
        if (vt == VoteType.DISLIKE)
            return -1;
        return 0;
    }

    @Override
    public void ajouter(CommunityComment comment) throws SQLException {
        if (profanityFilterService.containsProfanity(comment.getContent())) {
            throw new IllegalArgumentException("Ce commentaire contient un langage inapproprié.");
        }

        String sql = "INSERT INTO community_comments (post_id, user_id, content, created_at, parent_comment_id) VALUES (?, ?, ?, NOW(), ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, comment.getPostId());
            ps.setInt(2, comment.getUserId());
            ps.setString(3, comment.getContent());
            if (comment.getParentCommentId() != null) {
                ps.setInt(4, comment.getParentCommentId());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        }

        // Gamification: Award points for commenting
        try {
            gamificationService.addPoints(comment.getUserId(), 5);
        } catch (Exception e) {
            // Non-critical: don't fail comment creation if gamification errors
        }
    }

    @Override
    public void modifier(CommunityComment comment) throws SQLException {
        if (profanityFilterService.containsProfanity(comment.getContent())) {
            throw new IllegalArgumentException("Ce commentaire contient un langage inapproprié.");
        }

        // Auto-sync Post ID with Parent Comment's Post ID if a parent is set
        if (comment.getParentCommentId() != null) {
            String parentSql = "SELECT post_id FROM community_comments WHERE id = ?";
            try (PreparedStatement psParent = cnx.prepareStatement(parentSql)) {
                psParent.setInt(1, comment.getParentCommentId());
                try (ResultSet rsParent = psParent.executeQuery()) {
                    if (rsParent.next()) {
                        int newPostId = rsParent.getInt("post_id");
                        comment.setPostId(newPostId); // Update object
                    }
                }
            }
        }

        String sql = "UPDATE community_comments SET content = ?, is_solution = ?, parent_comment_id = ?, post_id = ?, user_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, comment.getContent());
            ps.setBoolean(2, comment.isSolution());
            if (comment.getParentCommentId() != null) {
                ps.setInt(3, comment.getParentCommentId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setInt(4, comment.getPostId());
            ps.setInt(5, comment.getUserId());
            ps.setInt(6, comment.getId());
            ps.executeUpdate();
        }

        // Sync logic: Update post status based on this comment change
        syncPostResolution(comment);
    }

    private void syncPostResolution(CommunityComment comment) throws SQLException {
        // 1. Get Post ID if not set (might need to fetch it if object incomplete, but
        // usually we have it)
        // If comment doesn't have postId loaded, we need to fetch it.
        int postId = comment.getPostId();
        if (postId == 0) {
            String q = "SELECT post_id FROM community_comments WHERE id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(q)) {
                ps.setInt(1, comment.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    postId = rs.getInt("post_id");
            }
        }
        if (postId == 0)
            return;

        if (comment.isSolution()) {
            // Case A: This comment IS a solution -> Mark post as resolved
            // Optional: Unmark other solutions for this post? Usually only one solution
            // allowed.
            // Let's enforce single solution:
            String resetSql = "UPDATE community_comments SET is_solution = FALSE WHERE post_id = ? AND id != ?";
            try (PreparedStatement ps = cnx.prepareStatement(resetSql)) {
                ps.setInt(1, postId);
                ps.setInt(2, comment.getId());
                ps.executeUpdate();
            }

            String updatePost = "UPDATE community_posts SET is_resolved = TRUE, solution_comment_id = ? WHERE id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(updatePost)) {
                ps.setInt(1, comment.getId());
                ps.setInt(2, postId);
                ps.executeUpdate();
            }
        } else {
            // Case B: This comment is NOT a solution anymore -> Check if ANY other solution
            // exists
            String checkSql = "SELECT COUNT(*) FROM community_comments WHERE post_id = ? AND is_solution = TRUE";
            boolean hasOtherSolution = false;
            try (PreparedStatement ps = cnx.prepareStatement(checkSql)) {
                ps.setInt(1, postId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    hasOtherSolution = true;
                }
            }

            if (!hasOtherSolution) {
                // No solutions left -> Mark post as unresolved
                String updatePost = "UPDATE community_posts SET is_resolved = FALSE, solution_comment_id = NULL WHERE id = ?";
                try (PreparedStatement ps = cnx.prepareStatement(updatePost)) {
                    ps.setInt(1, postId);
                    ps.executeUpdate();
                }
            }
            // If there is another solution, we don't change post status (it remains
            // resolved by the other one)
            // But if we just unmarked the *current* solution, usually the post *should*
            // become unresolved unless another one was somehow already set.
            // The logic above covers it: if count > 0, it stays resolved. If count == 0, it
            // becomes unresolved.
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM community_comments WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<CommunityComment> recuperer() throws SQLException {
        // Warning: This could return huge amount of data. Usually we get by post.
        // For admin purposes, we return recent comments maybe?
        List<CommunityComment> comments = new ArrayList<>();
        String sql = "SELECT c.*, u.nom, u.prenom FROM community_comments c " +
                "JOIN user u ON c.user_id = u.id ORDER BY c.created_at DESC LIMIT 100";

        try (Statement st = cnx.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                comments.add(mapRowToComment(rs));
            }
        }
        return comments;
    }

    @Override
    public List<CommunityComment> rechercher(String keyword) throws SQLException {
        List<CommunityComment> comments = new ArrayList<>();
        String sql = "SELECT c.*, u.nom, u.prenom FROM community_comments c " +
                "JOIN user u ON c.user_id = u.id " +
                "WHERE LOWER(c.content) LIKE ? " +
                "ORDER BY c.created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            ps.setString(1, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comments.add(mapRowToComment(rs));
            }
        }
        return comments;
    }

    public List<CommunityComment> getCommentsForPost(int postId, int currentUserId) throws SQLException {
        List<CommunityComment> comments = new ArrayList<>();
        String sql = "SELECT c.*, u.nom, u.prenom, l.vote_type as my_vote " +
                "FROM community_comments c " +
                "JOIN user u ON c.user_id = u.id " +
                "LEFT JOIN community_likes l ON c.id = l.comment_id AND l.user_id = ? " +
                "WHERE c.post_id = ? " +
                "ORDER BY c.is_solution DESC, (c.likes - c.dislikes) DESC, c.created_at ASC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CommunityComment comment = mapRowToComment(rs);
                comment.setUserVote(voteTypeToInt(rs.getString("my_vote")));
                comments.add(comment);
            }
        }
        return comments;
    }

    private CommunityComment mapRowToComment(ResultSet rs) throws SQLException {
        int pid = rs.getInt("parent_comment_id");
        Integer parentId = rs.wasNull() ? null : pid;

        String userName = rs.getString("prenom") + " " + rs.getString("nom");

        return new CommunityComment(
                rs.getInt("id"),
                rs.getInt("post_id"),
                rs.getInt("user_id"),
                userName,
                rs.getString("content"),
                rs.getTimestamp("created_at"),
                rs.getInt("likes"),
                rs.getInt("dislikes"),
                rs.getBoolean("is_solution"),
                parentId);
    }

    // --- Like / Dislike Logic ---

    public int toggleLikeComment(int userId, int commentId) throws SQLException {
        return handleVote(userId, commentId, VoteType.LIKE);
    }

    public int toggleDislikeComment(int userId, int commentId) throws SQLException {
        return handleVote(userId, commentId, VoteType.DISLIKE);
    }

    private int handleVote(int userId, int commentId, VoteType newVoteType) throws SQLException {
        String checkSql = "SELECT vote_type FROM community_likes WHERE user_id = ? AND comment_id = ?";
        VoteType currentVote = null; // null = no vote exists

        try (PreparedStatement ps = cnx.prepareStatement(checkSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, commentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                currentVote = VoteType.fromString(rs.getString("vote_type"));
        }

        if (currentVote == newVoteType) {
            // User clicked same vote type again -> remove vote
            String delSql = "DELETE FROM community_likes WHERE user_id = ? AND comment_id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(delSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, commentId);
                ps.executeUpdate();
            }
            updateCount((newVoteType == VoteType.LIKE) ? "likes" : "dislikes", -1, commentId);
            return 0; // No vote

        } else if (currentVote == null) {
            // No existing vote -> insert new vote
            String insSql = "INSERT INTO community_likes (user_id, comment_id, vote_type) VALUES (?, ?, ?)";
            try (PreparedStatement ps = cnx.prepareStatement(insSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, commentId);
                ps.setString(3, newVoteType.name()); // Store as 'LIKE' or 'DISLIKE'
                ps.executeUpdate();
            }
            updateCount((newVoteType == VoteType.LIKE) ? "likes" : "dislikes", 1, commentId);
            return (newVoteType == VoteType.LIKE) ? 1 : -1;

        } else {
            // Switching vote type
            String updSql = "UPDATE community_likes SET vote_type = ? WHERE user_id = ? AND comment_id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(updSql)) {
                ps.setString(1, newVoteType.name()); // Store as 'LIKE' or 'DISLIKE'
                ps.setInt(2, userId);
                ps.setInt(3, commentId);
                ps.executeUpdate();
            }
            updateCount((currentVote == VoteType.LIKE) ? "likes" : "dislikes", -1, commentId);
            updateCount((newVoteType == VoteType.LIKE) ? "likes" : "dislikes", 1, commentId);
            return (newVoteType == VoteType.LIKE) ? 1 : -1;
        }
    }

    private void updateCount(String column, int delta, int id) throws SQLException {
        if (delta == 0)
            return;
        String sign = (delta > 0) ? "+" : "-";
        String absDelta = String.valueOf(Math.abs(delta));
        String sql = "UPDATE community_comments SET " + column + " = " + column + " " + sign + " " + absDelta
                + " WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void markCommentAsSolution(int commentId) throws SQLException {
        String sqlComment = "UPDATE community_comments SET is_solution = TRUE WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sqlComment)) {
            ps.setInt(1, commentId);
            ps.executeUpdate();
        }
    }
}
