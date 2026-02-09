package com.nirmalks.catalog_service.book.service;

import com.nirmalks.catalog_service.book.api.BookRequest;
import com.nirmalks.catalog_service.book.dto.BookDto;
import common.RestPage;
import dto.OrderMessage;
import dto.PageRequestDto;
import exceptions.InsufficientStockException;

import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface BookService {

	RestPage<BookDto> getAllBooks(PageRequestDto pageRequestDto);

	BookDto getBookById(Long id);

	BookDto createBook(BookRequest bookRequest);

	BookDto updateBook(Long id, BookRequest bookRequest);

	void deleteBookById(Long id);

	Page<BookDto> getBooksByGenre(String genre, PageRequestDto pageRequestDto);

	Page<BookDto> getBooksByAuthor(Long authorId, PageRequestDto pageRequestDto);

	void updateBookStock(Long bookId, int quantity);

	Page<BookDto> getFilteredBooks(String search, String genre, LocalDate startDate, LocalDate endDate, Double minPrice,
			Double maxPrice, String sortBy, String sortOrder, int page, int size);

	void updateStock(OrderMessage orderMessage);

	/**
	 * Reserve stock for a book. Used by saga for inventory reservation step.
	 * @throws InsufficientStockException if not enough stock available
	 */
	void reserveStock(Long bookId, int quantity) throws InsufficientStockException;

	/**
	 * Release previously reserved stock. Used for saga compensation.
	 */
	void releaseStock(Long bookId, int quantity);

}
