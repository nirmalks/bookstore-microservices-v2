package com.nirmalks.catalog_service.genre.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirmalks.catalog_service.genre.api.GenreRequest;
import com.nirmalks.catalog_service.genre.dto.GenreDto;
import com.nirmalks.catalog_service.genre.service.GenreService;

import dto.PageRequestDto;
import exceptions.GlobalExceptionHandler;
import exceptions.ResourceNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@WebMvcTest(GenreController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GenreControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private GenreService genreService;

	private GenreDto genreDto;

	private GenreRequest genreRequest;

	@BeforeEach
	void setUp() {
		genreDto = new GenreDto();
		genreDto.setId(1L);
		genreDto.setName("Fiction");
		genreRequest = new GenreRequest();
		genreRequest.setName("Fiction");
	}

	@Test
	void getAllGenres_will_return_all_genres() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
		Page<GenreDto> page = new PageImpl<>(List.of(genreDto), pageable, 1);
		when(genreService.getAllGenres(any(PageRequestDto.class))).thenReturn(page);
		mockMvc.perform(get("/api/v1/genres").param("page", "0").param("size", "10").param("sortKey", "id"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content[0].id").value(1))
			.andExpect(jsonPath("$.content[0].name").value("Fiction"))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.size").value(10))
			.andExpect(jsonPath("$.number").value(0))
			.andExpect(jsonPath("$.first").value(true))
			.andExpect(jsonPath("$.last").value(true));
		verify(genreService).getAllGenres(any(PageRequestDto.class));
	}

	@Test
	void getGenreById_will_return_genre_dto_if_genre_found_in_db() throws Exception {
		when(genreService.getGenreById(any())).thenReturn(genreDto);
		mockMvc.perform(get("/api/v1/genres/{id}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.name").value("Fiction"));
		verify(genreService).getGenreById(1L);
	}

	@Test
	void getGenreById_will_return_404_if_genre_not_found() throws Exception {
		when(genreService.getGenreById(1L)).thenThrow(new ResourceNotFoundException("genre not found"));
		mockMvc.perform(get("/api/v1/genres/{id}", 1L)).andExpect(status().isNotFound());
		verify(genreService).getGenreById(1L);
	}

	@Test
	void createGenre_will_return_201_and_genre_dto_if_created_successfully() throws Exception {
		when(genreService.createGenre(any(GenreRequest.class))).thenReturn(genreDto);
		mockMvc
			.perform(post("/api/v1/genres").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(genreRequest)))
			.andExpect(status().isCreated())
			.andExpect(header().string("location", "http://localhost/api/v1/genres/1"))
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.name").value("Fiction"));
		verify(genreService).createGenre(any(GenreRequest.class));
	}

	@Test
	void updateGenre_will_return_200_and_updated_genre_dto_if_genre_found_in_db() throws Exception {
		when(genreService.updateGenre(any(), any())).thenReturn(genreDto);
		mockMvc
			.perform(put("/api/v1/genres/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(genreRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.name").value("Fiction"));
		verify(genreService).updateGenre(any(), any());
	}

	@Test
	void updateGenre_will_return_404_if_genre_not_found() throws Exception {
		when(genreService.updateGenre(any(), any())).thenThrow(new ResourceNotFoundException("genre not found"));
		mockMvc
			.perform(put("/api/v1/genres/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(genreRequest)))
			.andExpect(status().isNotFound());
		verify(genreService).updateGenre(any(), any());
	}

	@Test
	void deleteGenreById_will_return_200_if_genre_found_in_db() throws Exception {
		mockMvc.perform(delete("/api/v1/genres/{id}", 1L)).andExpect(status().isOk());
		verify(genreService).deleteGenreById(1L);
	}

	@Test
	void deleteGenreById_will_return_404_if_genre_not_found() throws Exception {
		doThrow(new ResourceNotFoundException("Not Found")).when(genreService).deleteGenreById(1L);
		mockMvc.perform(delete("/api/v1/genres/{id}", 1L)).andExpect(status().isNotFound());
		verify(genreService).deleteGenreById(1L);
	}

}
