package com.sapan.chat.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sapan.chat.auth.entity.UserEntity;
import com.sapan.chat.auth.roles.UserRole;
import com.sapan.chat.auth.services.UserService;

@RestController
@RequestMapping("api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }
    
    @GetMapping(value = "/{version}/role", version = "1.0")
    public ResponseEntity<?> getUsersByRole(@RequestParam("role") UserRole role) {
        List<UserEntity> users = service.findUsersByRole(role);
        if (role == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(users);
    }
}
