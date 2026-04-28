package com.nirmalks.catalog_service.author.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorRequest(
		@NotBlank(message = "Name is required") @Size(max = 100,
				message = "Name must not exceed 100 characters") String name,
		@NotBlank(message = "Bio is required") @Size(max = 2000,
				message = "Bio must not exceed 2000 characters") String bio) {
}
