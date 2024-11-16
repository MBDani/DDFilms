package com.merino.ddfilms.model;

import java.util.List;

import lombok.Data;

@Data
public class User {

    String user;
    String email;
    List<Movie> movies;

    public User(String user, String email) {
        this.user = user;
        this.email = email;
    }

}
