package com.merino.ddfilms.model;


import com.google.firebase.firestore.Exclude;
import com.merino.ddfilms.utils.DateFormatter;

import java.util.List;

import lombok.Data;

@Data
public class Review {
    private String id;
    private String userId;
    private String userName;
    private String userProfileImageUrl;
    private int movieId;
    private String movieTitle;
    private String posterPath;
    private String backdropPath;
    private float rating; // De 0.5 a 5.0 (incrementos de 0.5)
    private String reviewText;
    private String reviewDate;
    private List<String> likeCount;
    private List<String> dislikeCount;
    private transient boolean isLikedByCurrentUser;
    private transient boolean isDislikedByCurrentUser;

    @Exclude
    public boolean isLikedByCurrentUser() {
        return isLikedByCurrentUser;
    }

    @Exclude
    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        isLikedByCurrentUser = likedByCurrentUser;
    }

    @Exclude
    public boolean isDislikedByCurrentUser() {
        return isDislikedByCurrentUser;
    }

    @Exclude
    public void setDislikedByCurrentUser(boolean dislikedByCurrentUser) {
        isDislikedByCurrentUser = dislikedByCurrentUser;
    }

    @Exclude
    public String getFormattedDate() {
        DateFormatter formatter = new DateFormatter();
        return formatter.getFormattedDate(this.reviewDate);
    }

}


