package com.merino.ddfilms.model;


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
    private float rating; // De 0.5 a 5.0 (incrementos de 0.5)
    private String reviewText;
    private String reviewDate;
    private List<String> likeCount;
    private List<String> dislikeCount;
    private boolean isLikedByCurrentUser;
    private boolean isDislikedByCurrentUser;

    public String getFormattedDate() {
        DateFormatter formatter = new DateFormatter(this.reviewDate);
        return formatter.getFormattedDate();
    }

}


