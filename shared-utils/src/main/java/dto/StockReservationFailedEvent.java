package dto;

import java.io.Serializable;

public record StockReservationFailedEvent(String eventId, String orderId, String reason) implements Serializable {
}
