package com.nirmalks.catalog_service.book.service;

import com.nirmalks.catalog_service.author.service.AuthorService;
import com.nirmalks.catalog_service.book.BookSpecification;
import com.nirmalks.catalog_service.book.api.BookRequest;
import com.nirmalks.catalog_service.book.dto.BookDto;
import com.nirmalks.catalog_service.book.dto.BookMapper;
import com.nirmalks.catalog_service.book.entity.Book;
import com.nirmalks.catalog_service.book.repository.BookRepository;
import com.nirmalks.catalog_service.genre.service.GenreService;
import com.nirmalks.catalog_service.metrics.BookMetrics;
import common.RequestUtils;
import common.RestPage;
import dto.OrderMessage;
import dto.PageRequestDto;
import exceptions.InsufficientStockException;
import exceptions.ResourceNotFoundException;
import io.micrometer.core.instrument.Timer;
import locking.DistributedLockService;
import locking.LockKeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Service
public class BookServiceImpl implements BookService {

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private AuthorService authorService;

	@Autowired
	private GenreService genreService;

	@Autowired
	private BookMetrics bookMetrics;

	@Autowired
	private DistributedLockService distributedLockService;

	private final Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);

	@Override
	@Cacheable(value = "books", key = "#pageRequestDto.page")
	public RestPage<BookDto> getAllBooks(PageRequestDto pageRequestDto) {
		Timer.Sample sample = bookMetrics.startBookQueryTimer();
		try {
			Pageable pageable = RequestUtils.getPageable(pageRequestDto);
			return new RestPage<>(bookRepository.findAll(pageable).map(BookMapper::toDTO));
		}
		finally {
			bookMetrics.stopBookQueryTimer(sample);
		}
	}

	@Override
	@Cacheable(value = "book", key = "#id")
	public BookDto getBookById(Long id) {
		bookMetrics.incrementBookViews();
		return bookRepository.findById(id)
			.map(BookMapper::toDTO)
			.orElseThrow(() -> new ResourceNotFoundException("Book not found"));
	}

	@Override
	@CacheEvict(value = { "books", "book" }, allEntries = true)
	public BookDto createBook(BookRequest bookRequest) {
		var authors = authorService.getAuthorsByIds(bookRequest.getAuthorIds());
		var genres = genreService.getGenresByIds(bookRequest.getGenreIds());
		return BookMapper.toDTO(bookRepository.save(BookMapper.toEntity(bookRequest, authors, genres)));
	}

	@Override
	@CacheEvict(value = { "books", "book" }, key = "#id")
	public BookDto updateBook(Long id, BookRequest bookRequest) {
		var existingBook = bookRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("book not found"));
		var authors = authorService.getAuthorsByIds(bookRequest.getAuthorIds());
		var genres = genreService.getGenresByIds(bookRequest.getGenreIds());
		return BookMapper.toDTO(bookRepository.save(BookMapper.toEntity(existingBook, bookRequest, authors, genres)));
	}

	@Override
	@CacheEvict(value = { "books", "book" }, key = "#id")
	public void deleteBookById(Long id) {
		bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("book not found"));
		bookRepository.deleteById(id);
	}

	@Override
	public Page<BookDto> getBooksByGenre(String genre, PageRequestDto pageRequestDto) {
		genreService.validateGenreName(genre);
		var pageable = RequestUtils.getPageable(pageRequestDto);
		var books = bookRepository.findAllByGenresName(genre, pageable);
		return books.map(BookMapper::toDTO);
	}

	@Override
	public Page<BookDto> getBooksByAuthor(Long authorId, PageRequestDto pageRequestDto) {
		authorService.getAuthorById(authorId);
		var pageable = RequestUtils.getPageable(pageRequestDto);
		var books = bookRepository.findAllByAuthorsId(authorId, pageable);
		return books.map(BookMapper::toDTO);
	}

	@Override
	public void updateBookStock(Long bookId, int quantity) {
		var book = bookRepository.findById(bookId)
			.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
		book.setStock(quantity);
		bookRepository.save(book);

		// Alert if stock is low (threshold: 10)
		if (quantity < 10) {
			bookMetrics.recordLowStockAlert(bookId, quantity);
		}
	}

	@Override
	public Page<BookDto> getFilteredBooks(String search, String genre, LocalDate startDate, LocalDate endDate,
			Double minPrice, Double maxPrice, String sortBy, String sortOrder, int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		Specification<Book> specification = BookSpecification.filterBy(search, genre, startDate, endDate, minPrice,
				maxPrice, sortBy, sortOrder);
		return bookRepository.findAll(specification, pageable).map(BookMapper::toDTO);

	}

	/**
	 * @deprecated Use {@link #reserveStock(Long, int)} instead for saga support. This
	 * method will be removed in a future release.
	 */
	@Deprecated
	@Transactional
	@Caching(
			evict = { @CacheEvict(value = "books", allEntries = true), @CacheEvict(value = "book", allEntries = true) })
	public void updateStock(OrderMessage orderMessage) {
		orderMessage.items().forEach(item -> {

			String lockKey = LockKeys.bookStock(item.bookId());

			distributedLockService.executeWithLock(lockKey, 5, 30, TimeUnit.SECONDS, () -> {
				int rowsUpdated = bookRepository.decrementStock(item.bookId(), item.quantity());

				if (rowsUpdated == 0) {
					bookMetrics.incrementStockReservationFailure();
					throw new RuntimeException("Stock update failed for Book ID: " + item.bookId());
				}

				bookMetrics.incrementStockReservationSuccess();

				// Check if stock is low after decrement
				bookRepository.findById(item.bookId()).ifPresent(book -> {
					if (book.getStock() < 10) {
						bookMetrics.recordLowStockAlert(item.bookId(), book.getStock());
					}
				});
			});

		});
	}

	@Override
	@Transactional
	@CacheEvict(value = { "books", "book" }, allEntries = true)
	public void reserveStock(Long bookId, int quantity) throws InsufficientStockException {
		String lockKey = LockKeys.bookStock(bookId);

		distributedLockService.executeWithLock(lockKey, 5, 30, TimeUnit.SECONDS, () -> {
			Book book = bookRepository.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

			if (book.getStock() < quantity) {
				bookMetrics.incrementStockReservationFailure();
				throw new InsufficientStockException(bookId, quantity, book.getStock(),
						"Insufficient stock for book: " + book.getTitle());
			}

			int rowsUpdated = bookRepository.decrementStock(bookId, quantity);

			if (rowsUpdated == 0) {
				bookMetrics.incrementStockReservationFailure();
				throw new InsufficientStockException(bookId, quantity, 0, "Stock reservation race condition");
			}

			bookMetrics.incrementStockReservationSuccess();

			// Check if stock is low after reservation
			int newStock = book.getStock() - quantity;
			if (newStock < 10) {
				bookMetrics.recordLowStockAlert(bookId, newStock);
			}

			logger.info("Reserved {} units of book {} (remaining: {})", quantity, bookId, newStock);
		});
	}

	@Override
	@Transactional
	@CacheEvict(value = { "books", "book" }, allEntries = true)
	public void releaseStock(Long bookId, int quantity) {
		String lockKey = LockKeys.bookStock(bookId);

		distributedLockService.executeWithLock(lockKey, 5, 30, TimeUnit.SECONDS, () -> {
			int rowsUpdated = bookRepository.incrementStock(bookId, quantity);

			if (rowsUpdated == 0) {
				logger.warn("Failed to release stock for book {} - book may not exist", bookId);
				return;
			}

			bookMetrics.incrementStockReleased();
			logger.info("Released {} units of book {} (compensation)", quantity, bookId);
		});
	}

}
