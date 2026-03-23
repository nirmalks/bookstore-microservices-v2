package com.nirmalks.bookstore.notification_service.messaging;

import com.nirmalks.bookstore.notification_service.idempotency.entity.ProcessedEvent;
import com.nirmalks.bookstore.notification_service.idempotency.repository.ProcessedEventRepository;
import com.nirmalks.bookstore.notification_service.metrics.NotificationMetrics;
import com.nirmalks.bookstore.notification_service.service.NotificationService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.rabbitmq.client.Channel;
import dto.OrderMessage;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

	@Mock
	private NotificationService notificationService;

	@Mock
	private ProcessedEventRepository processedEventRepository;

	@Mock
	private NotificationMetrics notificationMetrics;

	@Mock
	private Channel channel;

	@Mock
	private Timer.Sample timerSample;

	@InjectMocks
	private OrderEventConsumer orderEventConsumer;

	private OrderMessage validMessage;

	private final long deliveryTag = 1L;

	@BeforeEach
	void setUp() {
		validMessage = new OrderMessage("evt-123", "saga-123", "order-123", 1L, "test@example.com", 100.0,
				java.time.LocalDateTime.now(), java.util.Collections.emptyList());
	}

	@Test
	void consumeOrderEvent_succeeds_when_event_is_valid_and_not_processed_already() throws Exception {
		when(processedEventRepository.existsByEventId(validMessage.eventId())).thenReturn(false);
		when(notificationMetrics.startEmailProcessingTimer()).thenReturn(timerSample);

		orderEventConsumer.consumeOrderEvent(validMessage, channel, deliveryTag);

		ArgumentCaptor<ProcessedEvent> processedEventCaptor = ArgumentCaptor.forClass(ProcessedEvent.class);

		verify(notificationMetrics).incrementEventsReceived();
		verify(processedEventRepository).existsByEventId(validMessage.eventId());
		verify(processedEventRepository).save(processedEventCaptor.capture());
		verify(notificationService).sendEmail(validMessage.orderId(), validMessage.email());
		verify(notificationMetrics).incrementEmailsSent();
		verify(notificationMetrics).recordEmailByType("ORDER_EMAIL_NOTIFICATION");
		verify(notificationMetrics).stopEmailProcessingTimer(timerSample);

		ProcessedEvent processedEvent = processedEventCaptor.getValue();
		assertEquals(validMessage.eventId(), processedEvent.getEventId());
		assertEquals("ORDER_EMAIL_NOTIFICATION", processedEvent.getEventType());
		assertNotNull(processedEvent.getProcessedAt());
	}

	@Test
	void consumeOrderEvent_skips_processing_when_event_is_already_processed() throws Exception {
		when(processedEventRepository.existsByEventId(validMessage.eventId())).thenReturn(true);

		orderEventConsumer.consumeOrderEvent(validMessage, channel, deliveryTag);

		verify(notificationMetrics).incrementEventsReceived();
		verify(processedEventRepository).existsByEventId(validMessage.eventId());
		verify(notificationMetrics).incrementDuplicateEventsSkipped();
		verify(notificationMetrics, never()).startEmailProcessingTimer();
		verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
		verify(notificationService, never()).sendEmail(anyString(), anyString());
	}

	@Test
	void consumeOrderEvent_throws_exception_when_email_sending_fails() throws Exception {
		when(processedEventRepository.existsByEventId(validMessage.eventId())).thenReturn(false);
		when(notificationMetrics.startEmailProcessingTimer()).thenReturn(timerSample);

		doThrow(new RuntimeException("Test exception")).when(notificationService).sendEmail(anyString(), anyString());

		Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(OrderEventConsumer.class);
		Level previousLevel = logger.getLevel();
		logger.setLevel(Level.OFF);
		try {
			assertThrows(RuntimeException.class,
					() -> orderEventConsumer.consumeOrderEvent(validMessage, channel, deliveryTag));
		}
		finally {
			logger.setLevel(previousLevel);
		}

		verify(notificationMetrics).incrementEventsReceived();
		verify(processedEventRepository).save(any(ProcessedEvent.class));
		verify(notificationService).sendEmail(validMessage.orderId(), validMessage.email());
		verify(notificationMetrics).incrementEmailsFailed();
		verify(notificationMetrics, never()).incrementEmailsSent();
		verify(notificationMetrics, never()).recordEmailByType(anyString());
		verify(notificationMetrics).stopEmailProcessingTimer(timerSample);
	}

	@Test
	void consumeOrderEvent_sends_email_without_idempotency_persistence_when_event_id_is_null() throws Exception {
		OrderMessage messageWithoutEventId = new OrderMessage(null, "saga-123", "order-123", 1L, "test@example.com",
				100.0, java.time.LocalDateTime.now(), java.util.Collections.emptyList());
		when(notificationMetrics.startEmailProcessingTimer()).thenReturn(timerSample);

		orderEventConsumer.consumeOrderEvent(messageWithoutEventId, channel, deliveryTag);

		verify(processedEventRepository, never()).existsByEventId(anyString());
		verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
		verify(notificationService).sendEmail(messageWithoutEventId.orderId(), messageWithoutEventId.email());
		verify(notificationMetrics).incrementEmailsSent();
		verify(notificationMetrics).recordEmailByType("ORDER_EMAIL_NOTIFICATION");
		verify(notificationMetrics).stopEmailProcessingTimer(timerSample);
	}

}
