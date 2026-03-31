package com.nirmalks.catalog_service.book.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirmalks.catalog_service.book.api.BookRequest;
import com.nirmalks.catalog_service.book.dto.BookDto;
import com.nirmalks.catalog_service.book.service.BookService;

import common.RestPage;
import dto.PageRequestDto;
import exceptions.GlobalExceptionHandler;
import exceptions.ResourceNotFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.hamcrest.Matchers.hasItem;

@WebMvcTest(BookController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BookControllerTest {

	@MockitoBean
	private BookService bookService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private BookDto bookDto;

	private PageRequestDto pageRequestDto;

	@BeforeEach
	void setup() {
		bookDto = new BookDto(1L, "Book 1", List.of(1L), 10.0, 10, "1234567890", LocalDate.now(), List.of(1L),
				"Description 1", "image.jpg");

		pageRequestDto = new PageRequestDto(0, 10, "id", "asc");
	}

	@Test
	void getAllBooks_will_return_all_books() throws Exception {

		when(bookService.getAllBooks(any(PageRequestDto.class))).thenReturn(new RestPage<>(List.of(bookDto), 1, 10, 1));
		ArgumentCaptor<PageRequestDto> pageRequestDtoCaptor = ArgumentCaptor.forClass(PageRequestDto.class);
		mockMvc
			.perform(get("/api/v1/books").param("page", "0")
				.param("size", "10")
				.param("sortOrder", "asc")
				.param("sortKey", "id"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(1))
			.andExpect(jsonPath("$.content[0].title").value("Book 1"))
			.andExpect(jsonPath("$.content[0].price").value(10.0))
			.andExpect(jsonPath("$.content[0].stock").value(10))
			.andExpect(jsonPath("$.content[0].isbn").value("1234567890"))
			.andExpect(jsonPath("$.content[0].publishedDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.content[0].authorIds", hasItem(1)))
			.andExpect(jsonPath("$.content[0].genreIds", hasItem(1)))
			.andExpect(jsonPath("$.content[0].description").value("Description 1"))
			.andExpect(jsonPath("$.content[0].imagePath").value("image.jpg"));

		verify(bookService).getAllBooks(pageRequestDtoCaptor.capture());
		PageRequestDto capturedPageRequestDto = pageRequestDtoCaptor.getValue();
		assertEquals(0, capturedPageRequestDto.page());
		assertEquals(10, capturedPageRequestDto.size());
		assertEquals("asc", capturedPageRequestDto.sortOrder());
		assertEquals("id", capturedPageRequestDto.sortKey());
	}

	@Test
	void getBookById_will_return_book_dto_if_book_found_in_db() throws Exception {
		when(bookService.getBookById(any())).thenReturn(bookDto);
		mockMvc.perform(get("/api/v1/books/{id}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.title").value("Book 1"))
			.andExpect(jsonPath("$.price").value(10.0))
			.andExpect(jsonPath("$.stock").value(10))
			.andExpect(jsonPath("$.isbn").value("1234567890"))
			.andExpect(jsonPath("$.publishedDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.authorIds", hasItem(1)))
			.andExpect(jsonPath("$.genreIds", hasItem(1)))
			.andExpect(jsonPath("$.description").value("Description 1"))
			.andExpect(jsonPath("$.imagePath").value("image.jpg"));
		verify(bookService).getBookById(1L);
	}

	@Test
	void getBookById_will_return_404_if_book_not_found() throws Exception {
		when(bookService.getBookById(any())).thenThrow(new ResourceNotFoundException("Book not found"));
		mockMvc.perform(get("/api/v1/books/{id}", 1L)).andExpect(status().isNotFound());
		verify(bookService).getBookById(1L);
	}

	@Test
	void createBook_will_return_201_if_book_created_successfully() throws Exception {
		when(bookService.createBook(any())).thenReturn(bookDto);
		mockMvc
			.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(bookDto)))
			.andExpect(status().isCreated())
			.andExpect(header().string("location", "http://localhost/api/v1/books/1"))
			.andExpect(jsonPath("$.id").value(1));
		verify(bookService).createBook(any());
	}

	@Test
	void createBook_will_return_400_if_book_request_is_invalid() throws Exception {
		BookRequest invalidRequest = new BookRequest(); // Empty request will fail
														// validation

		mockMvc
			.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Validation failed"));

		verifyNoInteractions(bookService);
	}

	@Test
	void updateBook_will_return_200_and_book_dto_if_book_updated_successfully() throws Exception {
		when(bookService.updateBook(any(), any())).thenReturn(bookDto);
		mockMvc
			.perform(put("/api/v1/books/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(bookDto)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.title").value("Book 1"))
			.andExpect(jsonPath("$.price").value(10.0))
			.andExpect(jsonPath("$.stock").value(10))
			.andExpect(jsonPath("$.isbn").value("1234567890"))
			.andExpect(jsonPath("$.publishedDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.authorIds", hasItem(1)))
			.andExpect(jsonPath("$.genreIds", hasItem(1)))
			.andExpect(jsonPath("$.description").value("Description 1"))
			.andExpect(jsonPath("$.imagePath").value("image.jpg"));
		ArgumentCaptor<BookRequest> bookDtoCaptor = ArgumentCaptor.forClass(BookRequest.class);
		verify(bookService).updateBook(any(), bookDtoCaptor.capture());
		BookRequest capturedBookRequest = bookDtoCaptor.getValue();
		assertEquals("Book 1", capturedBookRequest.getTitle());
		assertEquals(10.0, capturedBookRequest.getPrice());
		assertEquals(10, capturedBookRequest.getStock());
		assertEquals("1234567890", capturedBookRequest.getIsbn());
		assertEquals(LocalDate.now(), capturedBookRequest.getPublishedDate());
		assertEquals(List.of(1L), capturedBookRequest.getAuthorIds());
		assertEquals(List.of(1L), capturedBookRequest.getGenreIds());
		assertEquals("Description 1", capturedBookRequest.getDescription());
		assertEquals("image.jpg", capturedBookRequest.getImagePath());
	}

	@Test
	void updateBook_will_return_404_if_book_not_found() throws Exception {
		when(bookService.updateBook(any(), any())).thenThrow(new ResourceNotFoundException("Book not found"));
		mockMvc
			.perform(put("/api/v1/books/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(bookDto)))
			.andExpect(status().isNotFound());
		verify(bookService).updateBook(any(), any());
	}

	@Test
	void updateBook_will_return_400_if_book_request_is_invalid() throws Exception {
		BookRequest bookRequest = new BookRequest();

		mockMvc
			.perform(put("/api/v1/books/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(bookRequest)))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(bookService);
	}

	@Test
	void deleteBook_will_return_204_if_book_deleted_successfully() throws Exception {
		mockMvc.perform(delete("/api/v1/books/{id}", 1L)).andExpect(status().isNoContent());
		verify(bookService).deleteBookById(1L);
	}

	@Test
	void deleteBook_will_return_404_if_book_not_found() throws Exception {
		doThrow(new ResourceNotFoundException("Book not found")).when(bookService).deleteBookById(any());
		mockMvc.perform(delete("/api/v1/books/{id}", 1L)).andExpect(status().isNotFound());
		verify(bookService).deleteBookById(1L);
	}

	@Test
	void getBooksByAuthorId_will_return_200_and_list_of_books_if_author_has_books() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
		List<BookDto> books = List.of(bookDto);
		Page<BookDto> page = new PageImpl<>(books, pageable, books.size());
		when(bookService.getBooksByAuthor(any(Long.class), any(PageRequestDto.class))).thenReturn(page);
		mockMvc
			.perform(get("/api/v1/books/author/{id}", 1L).param("page", "0")
				.param("size", "10")
				.param("sortOrder", "asc")
				.param("sortKey", "id"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(1))
			.andExpect(jsonPath("$.content[0].title").value("Book 1"))
			.andExpect(jsonPath("$.content[0].price").value(10.0))
			.andExpect(jsonPath("$.content[0].stock").value(10))
			.andExpect(jsonPath("$.content[0].isbn").value("1234567890"))
			.andExpect(jsonPath("$.content[0].publishedDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.content[0].authorIds", hasItem(1)))
			.andExpect(jsonPath("$.content[0].genreIds", hasItem(1)))
			.andExpect(jsonPath("$.content[0].description").value("Description 1"))
			.andExpect(jsonPath("$.content[0].imagePath").value("image.jpg"));
		verify(bookService).getBooksByAuthor(1L, pageRequestDto);
	}

	@Test
	void getBooksByAuthorId_will_return_404_if_author_not_found() throws Exception {
		doThrow(new ResourceNotFoundException("Author not found")).when(bookService).getBooksByAuthor(any(), any());
		mockMvc
			.perform(get("/api/v1/books/author/{id}", 1L).param("page", "0")
				.param("size", "10")
				.param("sortOrder", "asc")
				.param("sortKey", "id"))
			.andExpect(status().isNotFound());
		verify(bookService).getBooksByAuthor(1L, pageRequestDto);
	}

	@Test
	void getBooksByGenreId_will_return_200_and_list_of_books_if_genre_has_books() throws Exception {
		List<BookDto> books = List.of(bookDto);
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
		Page<BookDto> page = new PageImpl<>(books, pageable, books.size());
		when(bookService.getBooksByGenre(any(), any())).thenReturn(page);
		mockMvc
			.perform(get("/api/v1/books/genre/{name}", "Fantasy").param("page", "0")
				.param("size", "10")
				.param("sortOrder", "asc")
				.param("sortKey", "id"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.size()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(1))
			.andExpect(jsonPath("$.content[0].title").value("Book 1"))
			.andExpect(jsonPath("$.content[0].price").value(10.0))
			.andExpect(jsonPath("$.content[0].stock").value(10))
			.andExpect(jsonPath("$.content[0].isbn").value("1234567890"))
			.andExpect(jsonPath("$.content[0].publishedDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.content[0].authorIds", hasItem(1)))
			.andExpect(jsonPath("$.content[0].genreIds", hasItem(1)))
			.andExpect(jsonPath("$.content[0].description").value("Description 1"))
			.andExpect(jsonPath("$.content[0].imagePath").value("image.jpg"));
		verify(bookService).getBooksByGenre(any(), any());
	}

	@Test
	void getBooksByGenreId_will_return_404_if_genre_not_found() throws Exception {
		doThrow(new ResourceNotFoundException("Genre not found")).when(bookService).getBooksByGenre(any(), any());
		mockMvc
			.perform(get("/api/v1/books/genre/{name}", "Fantasy").param("page", "0")
				.param("size", "10")
				.param("sortOrder", "asc")
				.param("sortKey", "id"))

			.andExpect(status().isNotFound());
		verify(bookService).getBooksByGenre(any(), any());
	}

	@Test
	void getBooksByAuthorId_will_return_400_if_author_id_is_invalid() throws Exception {
		mockMvc
			.perform(get("/api/v1/books/author/{id}", "abc").param("page", "0")
				.param("size", "10")
				.param("sortOrder", "asc")
				.param("sortKey", "id"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Type mismatch"));
		verifyNoInteractions(bookService);
	}

	@Test
	void updateBookStock_will_return_200_if_book_stock_updated_successfully() throws Exception {
		mockMvc.perform(put("/api/v1/books/{id}/{quantity}", 1L, 10)).andExpect(status().isOk());
		verify(bookService).updateBookStock(1L, 10);
	}

	@Test
	void updateBookStock_will_return_404_if_book_not_found() throws Exception {
		doThrow(new ResourceNotFoundException("Book not found")).when(bookService).updateBookStock(anyLong(), anyInt());
		mockMvc.perform(put("/api/v1/books/{id}/{quantity}", 1L, 10)).andExpect(status().isNotFound());
		verify(bookService).updateBookStock(1L, 10);
	}

	@Test
	void updateBookStock_will_return_400_if_book_id_is_invalid() throws Exception {
		mockMvc.perform(put("/api/v1/books/{id}/{quantity}", "abc", 10))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Type mismatch"));
		verifyNoInteractions(bookService);
	}

	@Test
	void updateBookStock_will_return_400_if_quantity_is_invalid() throws Exception {
		mockMvc.perform(put("/api/v1/books/{id}/{quantity}", 1L, "abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Type mismatch"));
		verifyNoInteractions(bookService);
	}

	@Test
	void searchBooks_will_return_200_and_list_of_books_if_books_are_found() throws Exception {
		List<BookDto> books = List.of(bookDto);
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
		Page<BookDto> page = new PageImpl<>(books, pageable, books.size());
		when(bookService.getFilteredBooks(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
			.thenReturn(page);
		mockMvc
			.perform(get("/api/v1/books/search").param("search", "Book 1")
				.param("page", "0")
				.param("size", "10")
				.param("sortOrder", "asc")
				.param("sortBy", "id"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.size()").value(1))
			.andExpect(jsonPath("$.content[0].id").value(1))
			.andExpect(jsonPath("$.content[0].title").value("Book 1"))
			.andExpect(jsonPath("$.content[0].price").value(10.0))
			.andExpect(jsonPath("$.content[0].stock").value(10))
			.andExpect(jsonPath("$.content[0].isbn").value("1234567890"))
			.andExpect(jsonPath("$.content[0].publishedDate").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.content[0].authorIds", hasItem(1)))
			.andExpect(jsonPath("$.content[0].genreIds", hasItem(1)))
			.andExpect(jsonPath("$.content[0].description").value("Description 1"))
			.andExpect(jsonPath("$.content[0].imagePath").value("image.jpg"));
		verify(bookService).getFilteredBooks("Book 1", null, null, null, null, null, "id", "asc", 0, 10);
	}

}
