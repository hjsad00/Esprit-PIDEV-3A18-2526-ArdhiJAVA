package tn.neuron.ardhi.models.UserAndDiag;

import java.sql.Timestamp;

public class CommunityPost {
    private int id;
    private int userId;
    private String userName; // Helper for display
    private String title;
    private String description;
    private String imageUrl; // Can be null
    private Timestamp createdAt;
    private int commentCount; // Helper for display
    private int likes; // Restored
    private int dislikes;
    private boolean isResolved; // Restored
    private int solutionCommentId; // Restored
    private int userVote; // 0=None, 1=Like, -1=Dislike

    public CommunityPost(int id, int userId, String userName, String title, String description, String imageUrl,
            Timestamp createdAt, int likes, int dislikes, boolean isResolved, int solutionCommentId) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.likes = likes;
        this.dislikes = dislikes;
        this.isResolved = isResolved;
        this.solutionCommentId = solutionCommentId;
    }

    public CommunityPost(int userId, String title, String description, String imageUrl) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
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

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    public int getSolutionCommentId() {
        return solutionCommentId;
    }

    public void setSolutionCommentId(int solutionCommentId) {
        this.solutionCommentId = solutionCommentId;
    }

    public boolean isLikedByCurrentUser() {
        return userVote == 1;
    }

    public boolean isDislikedByCurrentUser() {
        return userVote == -1;
    }

    public int getUserVote() {
        return userVote;
    }

    public void setUserVote(int userVote) {
        this.userVote = userVote;
    }
}
