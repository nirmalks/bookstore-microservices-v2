package com.nirmalks.bookstore.notification_service.messaging;

import com.nirmalks.bookstore.notification_service.idempotency.entity.ProcessedEvent;
import com.nirmalks.bookstore.notification_service.idempotency.repository.ProcessedEventRepository;
import com.nirmalks.bookstore.notification_service.metrics.NotificationMetrics;
import com.nirmalks.bookstore.notification_service.service.NotificationService;
import com.rabbitmq.client.Channel;
import dto.OrderMessage;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OrderEventConsumer {

	private static final String EVENT_TYPE = "ORDER_EMAIL_NOTIFICATION";

	private final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

	private final NotificationService notificationService;

	private final ProcessedEventRepository processedEventRepository;

	private final NotificationMetrics notificationMetrics;

	OrderEventConsumer(NotificationService notificationService, ProcessedEventRepository processedEventRepository,
			NotificationMetrics notificationMetrics) {
		this.notificationService = notificationService;
		this.processedEventRepository = processedEventRepository;
		this.notificationMetrics = notificationMetrics;
	}

	@RabbitListener(queues = RabbitMqConfig.QUEUE_EMAIL)
	@Transactional
	public void consumeOrderEvent(OrderMessage message, Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws Exception {
		String eventId = message.eventId();
		notificationMetrics.incrementEventsReceived();

		// skip processing if event is already processed to ensure consumer idempotency
		if (eventId != null && processedEventRepository.existsByEventId(eventId)) {
			logger.info("Duplicate event detected, skipping email. eventId={}, orderId={}", eventId, message.orderId());
			notificationMetrics.incrementDuplicateEventsSkipped();
			return;
		}

		// mark event as processed to ensure consumer idempotency
		if (eventId != null) {
			processedEventRepository.save(new ProcessedEvent(eventId, EVENT_TYPE, Instant.now()));
		}

		Timer.Sample timerSample = notificationMetrics.startEmailProcessingTimer();
		try {
			notificationService.sendEmail(message.orderId(), message.email());
			notificationMetrics.incrementEmailsSent();
			notificationMetrics.recordEmailByType(EVENT_TYPE);
			logger.info("Processing order event for email notification: eventId={}, orderId={}", eventId,
					message.orderId());
			logger.info("Successfully processed email notification: eventId={}, orderId={}", eventId,
					message.orderId());
		}
		catch (Exception e) {
			notificationMetrics.incrementEmailsFailed();
			logger.error("Failed to send email notification: eventId={}, orderId={}", eventId, message.orderId(), e);
			throw e;
		}
		finally {
			notificationMetrics.stopEmailProcessingTimer(timerSample);
		}
	}

}
