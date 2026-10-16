package com.sapan.chat.auth.entity;

import lombok.Data;

@Data
public class UserEntity {
    private String username;
    private String name;
    private String password;

    public UserEntity(String username, String name, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
    }
}
