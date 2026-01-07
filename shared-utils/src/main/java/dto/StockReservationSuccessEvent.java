package dto;

import java.io.Serializable;

public record StockReservationSuccessEvent(String eventId, String orderId) implements Serializable {
}
