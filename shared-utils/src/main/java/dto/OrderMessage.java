package dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record OrderMessage(String eventId, String sagaId, String orderId, Long userId, String email, double totalCost,
		LocalDateTime placedAt, List<OrderItemPayload> items) implements Serializable {
}
