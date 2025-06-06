package com.merino.ddfilms.model;


import com.google.firebase.firestore.Exclude;
import com.merino.ddfilms.utils.DateFormatter;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
    @Getter(onMethod_={@Exclude})
    @Setter(onMethod_={@Exclude})
    private transient boolean isLikedByCurrentUser;
    @Getter(onMethod_={@Exclude})
    @Setter(onMethod_={@Exclude})
    private transient boolean isDislikedByCurrentUser;
    @Exclude
    public String getFormattedDate() {
        DateFormatter formatter = new DateFormatter(this.reviewDate);
        return formatter.getFormattedDate();
    }

}


