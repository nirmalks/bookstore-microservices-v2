package com.nirmalks.catalog_service.book.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import exceptions.InsufficientStockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import com.nirmalks.catalog_service.author.entity.Author;
import com.nirmalks.catalog_service.author.service.AuthorService;
import com.nirmalks.catalog_service.book.api.BookRequest;
import com.nirmalks.catalog_service.book.dto.BookDto;
import com.nirmalks.catalog_service.book.entity.Book;
import com.nirmalks.catalog_service.book.repository.BookRepository;
import com.nirmalks.catalog_service.book.repository.StockReservationRepository;
import com.nirmalks.catalog_service.book.service.BookServiceImpl;
import com.nirmalks.catalog_service.genre.entity.Genre;
import com.nirmalks.catalog_service.genre.service.GenreService;
import com.nirmalks.catalog_service.metrics.BookMetrics;

import common.RestPage;
import dto.PageRequestDto;
import exceptions.ResourceNotFoundException;
import com.nirmalks.catalog_service.idempotency.repository.ProcessedEventRepository;
import locking.DistributedLockService;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private StockReservationRepository stockReservationRepository;

	@InjectMocks
	private BookServiceImpl bookService;

	@Mock
	private AuthorService authorService;

	@Mock
	private GenreService genreService;

	@Mock
	private BookMetrics bookMetrics;

	@Mock
	private DistributedLockService distributedLockService;

	@Mock
	private ProcessedEventRepository processedEventRepository;

	private Book book;

	private Author author;

	private Genre genre;

	private BookRequest bookRequest;

	private PageRequestDto pageRequestDto;

	@BeforeEach
	void setup() {
		author = new Author();
		author.setId(1L);
		author.setName("Author 1");
		author.setBio("Bio 1");

		genre = new Genre();
		genre.setId(1L);
		genre.setName("Genre 1");
		book = new Book();
		book.setId(1L);
		book.setTitle("Book 1");
		book.setPrice(10.0);
		book.setStock(10);
		book.setIsbn("1234567890");
		book.setPublishedDate(LocalDate.now());
		book.setDescription("Description 1");
		book.setAuthors(List.of(author));
		book.setGenres(List.of(genre));

		bookRequest = new BookRequest();
		bookRequest.setTitle("Book 1");
		bookRequest.setPrice(10.0);
		bookRequest.setStock(10);
		bookRequest.setIsbn("1234567890");
		bookRequest.setPublishedDate(LocalDate.now());
		bookRequest.setAuthorIds(List.of(1L));
		bookRequest.setGenreIds(List.of(1L));
		bookRequest.setDescription("Description 1");
		bookRequest.setImagePath("image.jpg");
		pageRequestDto = new PageRequestDto(0, 10, "id", "asc");
	}

	private void stubLockService() {
		// Default behavior for lock service: execute the runnable/supplier
		lenient().doAnswer(invocation -> {
			Runnable runnable = invocation.getArgument(4);
			runnable.run();
			return null;
		})
			.when(distributedLockService)
			.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

		lenient().doAnswer(invocation -> {
			Supplier<?> supplier = invocation.getArgument(4);
			return supplier.get();
		})
			.when(distributedLockService)
			.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Supplier.class));
	}

	@Test
	void getAllBooks_will_return_all_books() {
		when(bookRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(book)));
		when(bookMetrics.startBookQueryTimer()).thenReturn(null);
		RestPage<BookDto> restPage = bookService.getAllBooks(pageRequestDto);
		assertEquals(1l, restPage.getContent().get(0).id());
		assertEquals("Book 1", restPage.getContent().get(0).title());
		assertEquals(10.0, restPage.getContent().get(0).price());
		assertEquals(10, restPage.getContent().get(0).stock());
		assertEquals("1234567890", restPage.getContent().get(0).isbn());
		assertEquals(LocalDate.now(), restPage.getContent().get(0).publishedDate());
		assertEquals(List.of(1L), restPage.getContent().get(0).authorIds());
		assertEquals("Description 1", restPage.getContent().get(0).description());
	}

	@Test
	void getBookById_will_return_exception_if_book_not_found_in_db() {
		when(bookRepository.findById(1L)).thenReturn(Optional.empty());

		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
			bookService.getBookById(1L);
		});

		assertEquals("Book not found", exception.getMessage());
	}

	@Test
	void getBookById_will_return_book_dto_if_book_found_in_db() {
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
		BookDto bookDto = bookService.getBookById(1L);
		assertEquals(1l, bookDto.id());
		assertEquals("Book 1", bookDto.title());
		assertEquals(10.0, bookDto.price());
		assertEquals(10, bookDto.stock());
		assertEquals("1234567890", bookDto.isbn());
		assertEquals(LocalDate.now(), bookDto.publishedDate());
		assertEquals(List.of(1L), bookDto.authorIds());
		assertEquals("Description 1", bookDto.description());
	}

	@Test
	void createBook_will_return_book_dto_if_book_created_successfully() {
		when(authorService.getAuthorsByIds(anyList())).thenReturn(List.of(author));
		when(genreService.getGenresByIds(anyList())).thenReturn(List.of(genre));
		when(bookRepository.save(any(Book.class))).thenReturn(book);

		BookDto bookDto = bookService.createBook(bookRequest);
		assertEquals(1l, bookDto.id());
		assertEquals("Book 1", bookDto.title());
		assertEquals(10.0, bookDto.price());
		assertEquals(10, bookDto.stock());
		assertEquals("1234567890", bookDto.isbn());
		assertEquals(LocalDate.now(), bookDto.publishedDate());
		assertEquals(List.of(1L), bookDto.authorIds());
		assertEquals("Description 1", bookDto.description());
		verify(bookRepository, times(1)).save(any());
	}

	@Test
	void updateBook_will_return_book_dto_if_book_updated_successfully() {
		when(authorService.getAuthorsByIds(anyList())).thenReturn(List.of(author));
		when(genreService.getGenresByIds(anyList())).thenReturn(List.of(genre));
		when(bookRepository.save(any(Book.class))).thenReturn(book);
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
		BookDto bookDto = bookService.updateBook(1L, bookRequest);
		assertEquals(1l, bookDto.id());
		assertEquals("Book 1", bookDto.title());
		assertEquals(10.0, bookDto.price());
		assertEquals(10, bookDto.stock());
		assertEquals("1234567890", bookDto.isbn());
		assertEquals(LocalDate.now(), bookDto.publishedDate());
		assertEquals(List.of(1L), bookDto.authorIds());
		assertEquals("Description 1", bookDto.description());
		verify(bookRepository, times(1)).save(any());
	}

	@Test
	void updateBook_will_throw_exception_if_book_not_found_in_db() {
		when(bookRepository.findById(1L)).thenReturn(Optional.empty());
		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
			bookService.updateBook(1L, bookRequest);
		});
		assertEquals("book not found", exception.getMessage());
	}

	@Test
	void updateBookStock_will_return_book_dto_if_book_stock_updated_successfully() {
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
		bookService.updateBookStock(1L, 10);
		verify(bookRepository, times(1)).save(any());
	}

	@Test
	void updateBookStock_will_record_low_stock_alert_if_book_stock_less_than_10() {
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
		doNothing().when(bookMetrics).recordLowStockAlert(1L, 9);
		bookService.updateBookStock(1L, 9);
		verify(bookRepository, times(1)).save(any());
		verify(bookMetrics, times(1)).recordLowStockAlert(1L, 9);
	}

	@Test
	void updateBookStock_will_throw_exception_if_book_not_found_in_db() {
		when(bookRepository.findById(1L)).thenReturn(Optional.empty());
		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
			bookService.updateBookStock(1L, 10);
		});
		assertEquals("Book not found with ID: 1", exception.getMessage());
	}

	@Test
	void deleteBook_will_return_book_dto_if_book_deleted_successfully() {
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
		bookService.deleteBookById(1L);
		verify(bookRepository, times(1)).deleteById(1L);
	}

	@Test
	void deleteBook_will_throw_exception_if_book_not_found_in_db() {
		when(bookRepository.findById(1L)).thenReturn(Optional.empty());
		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
			bookService.deleteBookById(1L);
		});
		assertEquals("book not found", exception.getMessage());
	}

	@Test
	void getFilteredBooks_will_return_filtered_books_successfully() {
		when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(book)));
		Page<BookDto> bookDto = bookService.getFilteredBooks("search", "genre", LocalDate.now(), LocalDate.now(), 10.0,
				10.0, "id", "asc", 0, 10);
		assertEquals(1l, bookDto.getContent().get(0).id());
		assertEquals("Book 1", bookDto.getContent().get(0).title());
		assertEquals(10.0, bookDto.getContent().get(0).price());
		assertEquals(10, bookDto.getContent().get(0).stock());
		assertEquals("1234567890", bookDto.getContent().get(0).isbn());
		assertEquals(LocalDate.now(), bookDto.getContent().get(0).publishedDate());
		assertEquals(List.of(1L), bookDto.getContent().get(0).authorIds());
		assertEquals("Description 1", bookDto.getContent().get(0).description());
	}

	@Test
	void getBooksByGenre_will_return_filtered_books_successfully() {
		when(bookRepository.findAllByGenresName(any(String.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(book)));
		Page<BookDto> bookDto = bookService.getBooksByGenre("genre", pageRequestDto);
		assertEquals(1l, bookDto.getContent().get(0).id());
		assertEquals("Book 1", bookDto.getContent().get(0).title());
		assertEquals(10.0, bookDto.getContent().get(0).price());
		assertEquals(10, bookDto.getContent().get(0).stock());
		assertEquals("1234567890", bookDto.getContent().get(0).isbn());
		assertEquals(LocalDate.now(), bookDto.getContent().get(0).publishedDate());
		assertEquals(List.of(1L), bookDto.getContent().get(0).authorIds());
		assertEquals("Description 1", bookDto.getContent().get(0).description());
	}

	@Test
	void getBooksByAuthor_will_return_filtered_books_successfully() {
		when(bookRepository.findAllByAuthorsId(any(Long.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(book)));
		Page<BookDto> bookDto = bookService.getBooksByAuthor(1L, pageRequestDto);
		assertEquals(1l, bookDto.getContent().get(0).id());
		assertEquals("Book 1", bookDto.getContent().get(0).title());
		assertEquals(10.0, bookDto.getContent().get(0).price());
		assertEquals(10, bookDto.getContent().get(0).stock());
		assertEquals("1234567890", bookDto.getContent().get(0).isbn());
		assertEquals(LocalDate.now(), bookDto.getContent().get(0).publishedDate());
		assertEquals(List.of(1L), bookDto.getContent().get(0).authorIds());
		assertEquals("Description 1", bookDto.getContent().get(0).description());
	}

	@Test
	void reserveStock_will_reserve_stock_successfully() throws InsufficientStockException {
		stubLockService();
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
		when(bookRepository.decrementStock(1L, 10)).thenReturn(1);

		bookService.reserveStock(1L, 10);

		verify(bookRepository).decrementStock(1L, 10);
		verify(bookMetrics).incrementStockReservationSuccess();
	}

	@Test
	void reserveStock_will_throw_exception_if_book_not_found_in_db() {
		stubLockService();
		when(bookRepository.findById(1L)).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> {
			bookService.reserveStock(1L, 10);
		});
	}

	@Test
	void reserveStock_will_throw_exception_if_book_stock_insufficient() {
		stubLockService();
		book.setStock(5);
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

		assertThrows(InsufficientStockException.class, () -> {
			bookService.reserveStock(1L, 10);
		});

		verify(bookRepository, times(0)).decrementStock(1L, 10);
		verify(bookMetrics).incrementStockReservationFailure();
	}

	@Test
	void reserveStock_will_throw_exception_on_race_condition() {
		stubLockService();
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
		when(bookRepository.decrementStock(1L, 10)).thenReturn(0);

		assertThrows(InsufficientStockException.class, () -> {
			bookService.reserveStock(1L, 10);
		});
		verify(bookRepository, times(1)).decrementStock(1L, 10);
		verify(bookMetrics).incrementStockReservationFailure();
	}

	@Test
	void releaseStock_will_increment_stock_successfully() {
		stubLockService();
		String sagaId = "saga-1";
		Long bookId = 1L;
		int quantity = 10;
		String idempotencyKey = "release-" + sagaId + "-" + bookId;

		when(processedEventRepository.existsByEventId(idempotencyKey)).thenReturn(false);
		when(bookRepository.incrementStock(bookId, quantity)).thenReturn(1);

		bookService.releaseStock(sagaId, bookId, quantity);

		verify(bookRepository).incrementStock(bookId, quantity);
		verify(processedEventRepository).save(any());
		verify(bookMetrics).incrementStockReleased();
	}

	@Test
	void releaseStock_will_not_increment_stock_if_already_processed() {
		stubLockService();
		String sagaId = "saga-1";
		Long bookId = 1L;
		int quantity = 10;
		String idempotencyKey = "release-" + sagaId + "-" + bookId;

		when(processedEventRepository.existsByEventId(idempotencyKey)).thenReturn(true);

		bookService.releaseStock(sagaId, bookId, quantity);

		verify(bookRepository, times(0)).incrementStock(anyLong(), anyInt());
		verify(processedEventRepository, times(0)).save(any());
	}

}
