package dto;

public record StockReservationFailedEvent(String orderId, String reason) {
}
