package com.nirmalks.catalog_service.genre.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenreRequest(@NotBlank(message = "Genre name is required") @Size(max = 50,
		message = "Genre name must not exceed 50 characters") String name) {
}
