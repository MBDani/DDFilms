package com.merino.ddfilms.model;

import lombok.Data;

@Data
public class User {

    String user;
    String email;

    public User(String user, String email) {
        this.user = user;
        this.email = email;
    }

}
