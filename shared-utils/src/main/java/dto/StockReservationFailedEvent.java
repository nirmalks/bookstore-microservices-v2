package dto;

import java.io.Serializable;
import java.util.List;

public record StockReservationFailedEvent(String eventId, String sagaId, String orderId, String reason,
		List<FailedItem> failedItems) implements Serializable {
	public record FailedItem(Long bookId, int requestedQuantity, int availableQuantity) implements Serializable {
	}
}
