package dto;

import java.io.Serializable;
import java.util.List;

public record StockReleaseEvent(String eventId, String sagaId, String orderId, String reason,
		List<ReleaseItem> itemsToRelease) implements Serializable {

	public record ReleaseItem(Long bookId, int quantity) implements Serializable {
	}
}