package com.sapan.chat.auth.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sapan.chat.auth.entity.UserEntity;
import com.sapan.chat.auth.roles.UserRole;

@Service 
public class UserService {

    public List<UserEntity> findUsersByRole(UserRole role) {
        return List.of(
            new UserEntity("xyz", "XYZ", "***"), 
            new UserEntity("abc", "ABC", "***")
        );
    }
    
}
