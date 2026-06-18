package com.merino.ddfilms.model;

import java.util.List;

import lombok.Data;

@Data
public class MovieLists {
    String id;
    String name;
    String description;
    List<String> userID;
    List<Movie> movies;

    // Denormalized fields
    List<String> coverPreviews;
    List<String> memberAvatarsPreview;
    String lastUpdated;
    int moviesCount;
}
