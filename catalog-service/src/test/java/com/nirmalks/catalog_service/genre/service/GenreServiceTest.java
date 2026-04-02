package com.nirmalks.catalog_service.genre.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import com.nirmalks.catalog_service.genre.api.GenreRequest;
import com.nirmalks.catalog_service.genre.entity.Genre;
import com.nirmalks.catalog_service.genre.repository.GenreRepository;

import dto.PageRequestDto;
import exceptions.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GenreServiceTest {

	@Mock
	private GenreRepository genreRepository;

	@InjectMocks
	private GenreServiceImpl genreService;

	private Genre genre;

	private GenreRequest genreRequest;

	private PageRequestDto pageRequestDto;

	@BeforeEach
	void setUp() {
		genre = new Genre();
		genre.setId(1L);
		genre.setName("Fiction");

		genreRequest = new GenreRequest();
		genreRequest.setName("Fiction");

		pageRequestDto = new PageRequestDto(0, 10, "id", "asc");
	}

	@Test
	void getGenreById_will_throw_exception_if_genre_not_found_in_db() {
		when(genreRepository.findById(any())).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> genreService.getGenreById(null));
	}

	@Test
	void getGenreById_will_return_genre_dto_if_genre_found_in_db() {
		when(genreRepository.findById(any())).thenReturn(Optional.of(genre));
		var result = genreService.getGenreById(1L);
		assertEquals(1L, result.getId());
		assertEquals("Fiction", result.getName());
	}

	@Test
	void createGenre_will_return_valid_genre_dto() {
		when(genreRepository.save(any())).thenReturn(genre);
		ArgumentCaptor<Genre> genreArgumentCaptor = ArgumentCaptor.forClass(Genre.class);
		var result = genreService.createGenre(genreRequest);
		assertEquals(1L, result.getId());
		assertEquals("Fiction", result.getName());
		verify(genreRepository).save(genreArgumentCaptor.capture());
		assertEquals("Fiction", genreArgumentCaptor.getValue().getName());
	}

	@Test
	void updateGenre_will_throw_exception_if_genre_not_found_in_db() {
		when(genreRepository.findById(any())).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> genreService.updateGenre(1L, genreRequest));
	}

	@Test
	void updateGenre_will_return_valid_genre_dto() {
		when(genreRepository.findById(any())).thenReturn(Optional.of(genre));
		when(genreRepository.save(any())).thenReturn(genre);
		ArgumentCaptor<Genre> genreArgumentCaptor = ArgumentCaptor.forClass(Genre.class);
		var result = genreService.updateGenre(1L, genreRequest);
		assertEquals(1L, result.getId());
		assertEquals("Fiction", result.getName());
		verify(genreRepository).save(genreArgumentCaptor.capture());
		assertEquals("Fiction", genreArgumentCaptor.getValue().getName());
	}

	@Test
	void getAllGenres_will_return_valid_genre_dto_page() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
		when(genreRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(genre)));
		var result = genreService.getAllGenres(pageRequestDto);
		assertEquals(1L, result.getContent().get(0).getId());
		assertEquals("Fiction", result.getContent().get(0).getName());
	}

	@Test
	void deleteGenreById_will_throw_exception_if_genre_not_found_in_db() {
		when(genreRepository.findById(any())).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> genreService.deleteGenreById(1L));
	}

	@Test
	void deleteGenreById_will_delete_genre_if_genre_found_in_db() {
		when(genreRepository.findById(any())).thenReturn(Optional.of(genre));
		genreService.deleteGenreById(1L);
		verify(genreRepository, times(1)).deleteById(1L);
	}

	@Test
	void getGenresByIds_will_return_exception_if_some_ids_are_not_found() {
		when(genreRepository.findAllById(any())).thenReturn(List.of(genre));
		assertThrows(ResourceNotFoundException.class, () -> genreService.getGenresByIds(List.of(1L, 2L)));
	}

	@Test
	void getGenresByIds_will_return_valid_genres() {
		when(genreRepository.findAllById(any())).thenReturn(List.of(genre));
		var result = genreService.getGenresByIds(List.of(1L));
		assertEquals(1L, result.get(0).getId());
		assertEquals("Fiction", result.get(0).getName());
	}

	@Test
	void validateGenreName_will_throw_exception_if_not_found() {
		when(genreRepository.findByName(any())).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> genreService.validateGenreName("Fiction"));
	}

	@Test
	void validateGenreName_will_not_throw_if_found() {
		when(genreRepository.findByName(any())).thenReturn(Optional.of(genre));
		genreService.validateGenreName("Fiction");
		verify(genreRepository, times(1)).findByName("Fiction");
	}

}
