package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Timestamp;

public class CommunityComment {
    private int id;
    private int postId;
    private int userId;
    private String userName; // Helper
    private String content;
    private Timestamp createdAt;
    private int likes; // Restored
    private int dislikes;
    private boolean isSolution; // Restored
    private Integer parentCommentId; // Nullable
    private int userVote;

    public CommunityComment(int id, int postId, int userId, String userName, String content, Timestamp createdAt,
            int likes, int dislikes, boolean isSolution, Integer parentCommentId) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.createdAt = createdAt;
        this.likes = likes;
        this.dislikes = dislikes;
        this.isSolution = isSolution;
        this.parentCommentId = parentCommentId;
    }

    public CommunityComment(int postId, int userId, String content, Integer parentCommentId) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.parentCommentId = parentCommentId;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public boolean isSolution() {
        return isSolution;
    }

    public void setSolution(boolean solution) {
        isSolution = solution;
    }

    public boolean isLikedByCurrentUser() {
        return userVote == 1;
    }

    public boolean isDislikedByCurrentUser() {
        return userVote == -1;
    }

    public Integer getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Integer parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public int getUserVote() {
        return userVote;
    }

    public void setUserVote(int userVote) {
        this.userVote = userVote;
    }
}
