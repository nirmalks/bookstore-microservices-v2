package com.nirmalks.catalog_service.actuator;

import com.nirmalks.catalog_service.book.repository.BookRepository;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom actuator endpoint for inventory management insights Accessible at
 * /actuator/inventory
 */
@Component
@Endpoint(id = "inventory")
public class InventoryManagementEndpoint {

	private final BookRepository bookRepository;

	public InventoryManagementEndpoint(BookRepository bookRepository) {
		this.bookRepository = bookRepository;
	}

	@ReadOperation
	public Map<String, Object> getInventoryStats() {
		Map<String, Object> stats = new HashMap<>();
		long totalBooks = bookRepository.count();
		stats.put("totalBooks", totalBooks);
		stats.put("status", "operational");
		return stats;
	}

	@ReadOperation
	public Map<String, Object> getBookInventory(@Selector Long bookId) {
		Map<String, Object> result = new HashMap<>();

		return bookRepository.findById(bookId).map(book -> {
			result.put("bookId", book.getId());
			result.put("title", book.getTitle());
			result.put("availableQuantity", book.getStock());
			result.put("status", book.getStock() > 0 ? "IN_STOCK" : "OUT_OF_STOCK");
			return result;
		}).orElseGet(() -> {
			result.put("error", "Book not found");
			result.put("bookId", bookId);
			return result;
		});
	}

}
