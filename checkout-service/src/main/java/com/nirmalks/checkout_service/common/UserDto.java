package com.nirmalks.checkout_service.common;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserDto(@NotBlank(message = "Username is required") String username, @NotNull Long id,
		@NotBlank(message = "Email is required") @Email(message = "Invalid email address") String email) {
}
