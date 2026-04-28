package com.nirmalks.catalog_service.book.api;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record BookRequest(
		@NotBlank(message = "Title is required") @Size(max = 255,
				message = "Title must not exceed 255 characters") String title,

		@NotEmpty(message = "At least one author must be specified") List<Long> authorIds,

		@NotNull(message = "Price is required") @Positive(message = "Price must be positive") Double price,

		@PositiveOrZero(message = "Stock cannot be negative") int stock,

		@NotBlank(message = "ISBN is required") @Pattern(regexp = "^(97(8|9))?\\d{9}(\\d|X)$",
				message = "Invalid ISBN format") String isbn,

		@NotNull(message = "Published date is required") @PastOrPresent(
				message = "Published date cannot be in the future") LocalDate publishedDate,

		@NotEmpty(message = "At least one genre must be specified") List<Long> genreIds,

		@Size(max = 1000, message = "Description must not exceed 1000 characters") String description,

		@Size(max = 500, message = "Image path must not exceed 500 characters") String imagePath) {
}
