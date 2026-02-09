package exceptions;

public class InsufficientStockException extends RuntimeException {

	private final Long bookId;

	private final int requestedQuantity;

	private final int availableQuantity;

	public InsufficientStockException(Long bookId, int requestedQuantity, int availableQuantity, String message) {
		super(message);
		this.bookId = bookId;
		this.requestedQuantity = requestedQuantity;
		this.availableQuantity = availableQuantity;
	}

	public Long getBookId() {
		return bookId;
	}

	public int getRequestedQuantity() {
		return requestedQuantity;
	}

	public int getAvailableQuantity() {
		return availableQuantity;
	}

}
