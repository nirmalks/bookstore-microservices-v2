package dto;

import java.io.Serializable;
import java.util.List;

/* 
 * Event to notify that stock has been reserved successfully
 */
public record StockReservationSuccessEvent(String eventId, String sagaId, String orderId,
		List<ReservedItem> reservedItems) implements Serializable {
	public record ReservedItem(Long bookId, int quantity) implements Serializable {
	}
}
