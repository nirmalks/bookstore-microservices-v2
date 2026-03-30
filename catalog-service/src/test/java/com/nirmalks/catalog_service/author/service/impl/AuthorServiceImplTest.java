package com.nirmalks.catalog_service.author.service.impl;

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

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import com.nirmalks.catalog_service.author.api.AuthorRequest;
import com.nirmalks.catalog_service.author.entity.Author;
import com.nirmalks.catalog_service.author.repository.AuthorRepository;
import com.nirmalks.catalog_service.author.service.AuthorServiceImpl;

import dto.PageRequestDto;
import exceptions.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthorServiceImplTest {

	@Mock
	private AuthorRepository authorRepository;

	@InjectMocks
	private AuthorServiceImpl authorService;

	private Author author;

	private AuthorRequest authorRequest;

	@BeforeEach
	void setUp() {
		author = new Author();
		author.setId(1L);
		author.setName("John Doe");
		author.setBio("some bio");

		authorRequest = new AuthorRequest();
		authorRequest.setName("John Doe");
		authorRequest.setBio("some bio");
	}

	@Test
	void getAuthorById_will_throw_exception_if_author_not_found_in_db() {
		when(authorRepository.findById(any())).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> authorService.getAuthorById(null));
	}

	@Test
	void getAuthorById_will_return_author_dto_if_author_found_in_db() {
		when(authorRepository.findById(any())).thenReturn(Optional.of(author));
		var result = authorService.getAuthorById(1L);
		assertEquals(1l, result.id());
		assertEquals("John Doe", result.name());
		assertEquals("some bio", result.bio());
	}

	@Test
	void createAuthor_will_return_valid_author_dto() {
		when(authorRepository.save(any())).thenReturn(author);
		ArgumentCaptor<Author> authorArgumentCaptor = ArgumentCaptor.forClass(Author.class);
		var result = authorService.createAuthor(authorRequest);
		assertEquals(1l, result.id());
		assertEquals("John Doe", result.name());
		assertEquals("some bio", result.bio());
		verify(authorRepository).save(authorArgumentCaptor.capture());
		assertEquals("John Doe", authorArgumentCaptor.getValue().getName());
		assertEquals("some bio", authorArgumentCaptor.getValue().getBio());
	}

	@Test
	void updateAuthor_will_throw_exception_if_author_not_found_in_db() {
		when(authorRepository.findById(any())).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> authorService.updateAuthor(null, authorRequest));
	}

	@Test
	void updateAuthor_will_return_valid_author_dto() {
		when(authorRepository.findById(any())).thenReturn(Optional.of(author));
		when(authorRepository.save(any())).thenReturn(author);
		ArgumentCaptor<Author> authorArgumentCaptor = ArgumentCaptor.forClass(Author.class);
		var result = authorService.updateAuthor(1L, authorRequest);
		assertEquals(1l, result.id());
		assertEquals("John Doe", result.name());
		assertEquals("some bio", result.bio());
		verify(authorRepository).save(authorArgumentCaptor.capture());
		assertEquals("John Doe", authorArgumentCaptor.getValue().getName());
		assertEquals("some bio", authorArgumentCaptor.getValue().getBio());
	}

	@Test
	void getAllAuthors_will_return_valid_author_dto_page() {
		PageRequestDto pageRequestDto = new PageRequestDto();
		pageRequestDto.setPage(0);
		pageRequestDto.setSize(10);
		pageRequestDto.setSortOrder("asc");
		pageRequestDto.setSortKey("id");
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
		when(authorRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(author)));
		var result = authorService.getAllAuthors(pageRequestDto);
		assertEquals(1l, result.getContent().get(0).id());
		assertEquals("John Doe", result.getContent().get(0).name());
		assertEquals("some bio", result.getContent().get(0).bio());
	}

	@Test
	void deleteAuthorById_will_throw_exception_if_author_not_found_in_db() {
		when(authorRepository.existsById(any())).thenReturn(false);
		assertThrows(ResourceNotFoundException.class, () -> authorService.deleteAuthorById(null));
	}

	@Test
	void deleteAuthorById_will_delete_author_if_author_found_in_db() {
		when(authorRepository.existsById(any())).thenReturn(true);
		authorService.deleteAuthorById(1L);
		verify(authorRepository, times(1)).deleteById(1L);
	}

	@Test
	void getAuthorsByIds_will_return_exception_if_some_ids_are_not_found() {
		when(authorRepository.findAllById(any())).thenReturn(List.of(author));
		assertThrows(ResourceNotFoundException.class, () -> authorService.getAuthorsByIds(List.of(1L, 2L)));
	}

	@Test
	void getAuthorsByIds_will_return_valid_authors() {
		when(authorRepository.findAllById(any())).thenReturn(List.of(author));
		var result = authorService.getAuthorsByIds(List.of(1L));
		assertEquals(1l, result.get(0).getId());
		assertEquals("John Doe", result.get(0).getName());
		assertEquals("some bio", result.get(0).getBio());
	}

}
