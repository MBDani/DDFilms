package com.merino.ddfilms.model;

import java.util.List;

import lombok.Data;

@Data
public class MovieLists {
    String id;
    String name;
    String description;
    List<Movie> movies;

}
