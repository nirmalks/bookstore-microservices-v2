package com.nirmalks.catalog_service.book.dto;

import java.time.LocalDate;
import java.util.List;

public record BookDto(Long id, String title, List<Long> authorIds, Double price, int stock, String isbn,
		LocalDate publishedDate, List<Long> genreIds, String description, String imagePath) {
}
