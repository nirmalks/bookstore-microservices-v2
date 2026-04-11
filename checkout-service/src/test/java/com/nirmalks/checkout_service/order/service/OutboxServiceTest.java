package com.nirmalks.checkout_service.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nirmalks.checkout_service.order.entity.Outbox;
import com.nirmalks.checkout_service.order.repository.OutboxRepository;

import dto.OrderMessage;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

	@InjectMocks
	private OutboxService outboxService;

	@Mock
	private OutboxRepository outboxRepository;

	@Test
	void saveOrderCreatedEvent_should_save_order_created_event_successfully() {
		OrderMessage orderMessage = new OrderMessage("1", "1", "1", 1L, "user@example.com", 0.0, LocalDateTime.now(),
				new ArrayList<>());
		outboxService.saveOrderCreatedEvent("1", orderMessage);
		verify(outboxRepository, times(1)).save(any(Outbox.class));
	}

}
