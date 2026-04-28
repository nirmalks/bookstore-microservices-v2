package com.nirmalks.catalog_service.genre.dto;

import com.nirmalks.catalog_service.genre.api.GenreRequest;
import com.nirmalks.catalog_service.genre.entity.Genre;

public class GenreMapper {

	public static GenreDto toDto(Genre genre) {
		return new GenreDto(genre.getId(), genre.getName());
	}

	public static Genre toEntity(Genre genre, GenreRequest genreRequest) {
		genre.setName(genreRequest.name());
		return genre;
	}

	public static Genre toEntity(GenreRequest genreRequest) {
		Genre genre = new Genre();
		genre.setName(genreRequest.name());
		return genre;
	}

}
