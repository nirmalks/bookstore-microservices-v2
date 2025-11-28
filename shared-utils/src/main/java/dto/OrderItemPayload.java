package dto;

public record OrderItemPayload(
        Long bookId,
        Integer quantity
) {}