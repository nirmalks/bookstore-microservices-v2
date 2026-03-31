package com.nirmalks.catalog_service.author.controller;

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
import com.nirmalks.catalog_service.author.api.AuthorRequest;
import com.nirmalks.catalog_service.author.dto.AuthorDto;
import com.nirmalks.catalog_service.author.service.AuthorService;
import com.nirmalks.catalog_service.book.service.BookService;

import dto.PageRequestDto;
import exceptions.GlobalExceptionHandler;
import exceptions.ResourceNotFoundException;

import static org.hamcrest.Matchers.hasItems;
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

@WebMvcTest(AuthorController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthorControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthorService authorService;

	@MockitoBean
	private BookService bookService;

	private AuthorDto authorDto;

	private AuthorRequest authorRequest;

	@BeforeEach
	void setUp() {
		authorDto = new AuthorDto(1L, "John Doe", "Bio");
		authorRequest = new AuthorRequest();
		authorRequest.setName("John Doe");
		authorRequest.setBio("Bio");
	}

	@Test
	void getAllAuthors_will_return_all_authors() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
		Page<AuthorDto> page = new PageImpl<>(List.of(authorDto), pageable, 1);
		when(authorService.getAllAuthors(any(PageRequestDto.class))).thenReturn(page);
		mockMvc.perform(get("/api/v1/authors").param("page", "0").param("size", "10").param("sortKey", "id"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content[0].id").value(1))
			.andExpect(jsonPath("$.content[0].name").value("John Doe"))
			.andExpect(jsonPath("$.content[0].bio").value("Bio"))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.size").value(10))
			.andExpect(jsonPath("$.number").value(0))
			.andExpect(jsonPath("$.first").value(true))
			.andExpect(jsonPath("$.last").value(true));
		verify(authorService).getAllAuthors(any(PageRequestDto.class));
	}

	@Test
	void getAuthorById_will_return_author_dto_if_author_found_in_db() throws Exception {
		when(authorService.getAuthorById(any())).thenReturn(authorDto);
		mockMvc.perform(get("/api/v1/authors/{id}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.name").value("John Doe"))
			.andExpect(jsonPath("$.bio").value("Bio"));
		verify(authorService).getAuthorById(1L);
	}

	@Test
	void getAuthorById_will_return_404_if_author_not_found() throws Exception {
		when(authorService.getAuthorById(1L)).thenThrow(new ResourceNotFoundException("Not Found"));
		mockMvc.perform(get("/api/v1/authors/{id}", 1L)).andExpect(status().isNotFound());
		verify(authorService).getAuthorById(1L);
	}

	@Test
	void createAuthor_will_return_201_and_author_dto_if_created_successfully() throws Exception {
		when(authorService.createAuthor(any(AuthorRequest.class))).thenReturn(authorDto);
		mockMvc
			.perform(post("/api/v1/authors").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(authorRequest)))
			.andExpect(status().isCreated())
			.andExpect(header().string("location", "http://localhost/api/v1/authors/1"))
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.name").value("John Doe"))
			.andExpect(jsonPath("$.bio").value("Bio"));
		verify(authorService).createAuthor(any(AuthorRequest.class));
	}

	@Test
	void createAuthor_will_return_400_if_author_request_is_invalid() throws Exception {
		authorRequest.setName(null);
		authorRequest.setBio(null);
		mockMvc
			.perform(post("/api/v1/authors").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(authorRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors").isArray())
			.andExpect(jsonPath("$.errors", hasItems("Name is required", "Bio is required")));
		verifyNoInteractions(authorService);
	}

	@Test
	void updateAuthor_will_return_200_and_updated_author_dto_if_author_found_in_db() throws Exception {
		when(authorService.updateAuthor(any(), any())).thenReturn(authorDto);
		mockMvc
			.perform(put("/api/v1/authors/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(authorRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.name").value("John Doe"))
			.andExpect(jsonPath("$.bio").value("Bio"));
		verify(authorService).updateAuthor(any(), any());
	}

	@Test
	void updateAuthor_will_return_404_if_author_not_found() throws Exception {
		when(authorService.updateAuthor(any(), any())).thenThrow(new ResourceNotFoundException("Not Found"));
		mockMvc
			.perform(put("/api/v1/authors/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(authorRequest)))
			.andExpect(status().isNotFound());
		verify(authorService).updateAuthor(any(), any());
	}

	@Test
	void updateAuthor_will_return_400_if_author_request_is_invalid() throws Exception {
		authorRequest.setName(null);
		authorRequest.setBio(null);
		mockMvc
			.perform(put("/api/v1/authors/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(authorRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors").isArray())
			.andExpect(jsonPath("$.errors", hasItems("Name is required", "Bio is required")));
		verifyNoInteractions(authorService);
	}

	@Test
	void deleteAuthorById_will_return_204_if_author_found_in_db() throws Exception {
		mockMvc.perform(delete("/api/v1/authors/{id}", 1L)).andExpect(status().isNoContent());
		verify(authorService).deleteAuthorById(1L);
	}

	@Test
	void deleteAuthorById_will_return_404_if_author_not_found() throws Exception {
		doThrow(new ResourceNotFoundException("Not Found")).when(authorService).deleteAuthorById(1L);
		mockMvc.perform(delete("/api/v1/authors/{id}", 1L)).andExpect(status().isNotFound());
		verify(authorService).deleteAuthorById(1L);
	}

}
