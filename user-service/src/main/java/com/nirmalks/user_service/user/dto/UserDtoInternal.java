package com.nirmalks.user_service.user.dto;

import dto.UserRole;

public record UserDtoInternal(Long id, String username, String hashedPassword, UserRole role) {
}
