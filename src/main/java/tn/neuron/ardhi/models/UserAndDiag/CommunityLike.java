package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Timestamp;

/**
 * Représente un vote (like/dislike) sur un post ou commentaire.
 */
public class CommunityLike {

    private int id;
    private int userId;
    private Integer postId; // null if vote is on a comment
    private Integer commentId; // null if vote is on a post
    private VoteType voteType;
    private Timestamp createdAt;

    // For display purposes
    private String userName;

    public CommunityLike() {
    }

    public CommunityLike(int userId, Integer postId, Integer commentId, VoteType voteType) {
        this.userId = userId;
        this.postId = postId;
        this.commentId = commentId;
        this.voteType = voteType;
    }

    // Getters and Setters
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

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public VoteType getVoteType() {
        return voteType;
    }

    public void setVoteType(VoteType voteType) {
        this.voteType = voteType;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Returns a human-readable string for the vote target (Post or Comment).
     */
    public String getTargetDescription() {
        if (postId != null) {
            return "Post #" + postId;
        } else if (commentId != null) {
            return "Comment #" + commentId;
        }
        return "N/A";
    }
}
