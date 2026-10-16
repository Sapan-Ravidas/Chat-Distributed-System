package com.sapan.chat.auth.converter;

import org.springframework.core.convert.converter.Converter;

import com.sapan.chat.auth.roles.UserRole;

public class StringToUserRoleConverter implements Converter<String, UserRole> {
    
    @Override
    public UserRole convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return UserRole.valueOf(source.trim().toUpperCase());
    }
}
