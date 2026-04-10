package com.nirmalks.catalog_service;

import com.nirmalks.catalog_service.book.repository.StockReservationRepository;
import com.nirmalks.catalog_service.book.service.BookService;
import com.nirmalks.catalog_service.idempotency.repository.ProcessedEventRepository;
import com.nirmalks.catalog_service.messaging.InventoryEventPublisher;
import com.nirmalks.catalog_service.messaging.OrderCreatedEventConsumer;
import com.nirmalks.catalog_service.metrics.BookMetrics;
import dto.OrderItemPayload;
import dto.OrderMessage;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreatedEventConsumerTest {

	@Mock
	private BookService bookService;

	@Mock
	private InventoryEventPublisher inventoryEventPublisher;

	@Mock
	private ProcessedEventRepository processedEventRepository;

	@Mock
	private StockReservationRepository stockReservationRepository;

	@Mock
	private BookMetrics bookMetrics;

	@Mock
	private Timer.Sample timerSample;

	@InjectMocks
	private OrderCreatedEventConsumer orderCreatedEventConsumer;

	private OrderMessage orderMessage;

	@BeforeEach
	void setUp() {
		orderMessage = new OrderMessage("event-1", "saga-1", "order-1", 1L, "user@example.com", 99.0,
				LocalDateTime.now(), List.of(new OrderItemPayload(10L, 2)));
	}

	@Test
	void consumeOrderCreatedEvent_records_duplicate_metrics_and_skips_work() {
		when(bookMetrics.startInventoryProcessingTimer()).thenReturn(timerSample);
		when(processedEventRepository.existsByEventId("event-1")).thenReturn(true);

		orderCreatedEventConsumer.consumeOrderCreatedEvent(orderMessage, null, 1L);

		verify(bookMetrics).incrementInventoryEventsReceived();
		verify(bookMetrics).incrementInventoryDuplicateEventsSkipped();
		verify(bookMetrics).stopInventoryProcessingTimer(timerSample);
		verify(bookService, never()).reserveStock(any(), anyInt());
		verify(inventoryEventPublisher, never()).publishStockReservedEvent(any(), any(), any());
		verifyNoInteractions(stockReservationRepository);
	}

	@Test
	void consumeOrderCreatedEvent_records_publish_success_for_successful_reservation() {
		when(bookMetrics.startInventoryProcessingTimer()).thenReturn(timerSample);
		when(processedEventRepository.existsByEventId("event-1")).thenReturn(false);

		orderCreatedEventConsumer.consumeOrderCreatedEvent(orderMessage, null, 1L);

		verify(bookMetrics).incrementInventoryEventsReceived();
		verify(bookMetrics).incrementInventoryPublishSuccess();
		verify(bookMetrics).stopInventoryProcessingTimer(timerSample);
		verify(stockReservationRepository).save(any());
		verify(inventoryEventPublisher).publishStockReservedEvent(eq("saga-1"), eq("order-1"), any());
		verify(processedEventRepository).save(any());
	}

}
