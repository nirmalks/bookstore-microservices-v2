package com.nirmalks.catalog_service.messaging;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.nirmalks.catalog_service.book.entity.StockReservation;
import com.nirmalks.catalog_service.book.repository.StockReservationRepository;
import com.nirmalks.catalog_service.book.service.BookService;

import dto.StockReleaseEvent;
import jakarta.transaction.Transactional;

@Service
public class StockReleaseConsumer {

	private static final Logger logger = LoggerFactory.getLogger(StockReleaseConsumer.class);

	private final BookService bookService;

	private final StockReservationRepository stockReservationRepository;

	public StockReleaseConsumer(BookService bookService, StockReservationRepository stockReservationRepository) {
		this.bookService = bookService;
		this.stockReservationRepository = stockReservationRepository;
	}

	@RabbitListener(queues = RabbitMqConfig.QUEUE_STOCK_RELEASE)
	@Transactional
	public void handleStockRelease(StockReleaseEvent event) {
		String sagaId = event.sagaId();

		logger.info("Processing stock release for saga: {}, reason: {}", sagaId, event.reason());

		stockReservationRepository.findBySagaId(sagaId).ifPresentOrElse(reservation -> {
			if (reservation.getStatus() == StockReservation.ReservationStatus.RELEASED) {
				logger.info("Stock already released for saga: {}", sagaId);
				return;
			}

			for (StockReservation.ReservedItem item : reservation.getReservedItems()) {
				try {
					bookService.releaseStock(item.bookId(), item.quantity());
					logger.info("Released {} units of book {} for saga {}", item.quantity(), item.bookId(), sagaId);
				}
				catch (Exception e) {
					logger.error("Failed to release stock for book {}: {}", item.bookId(), e.getMessage());
				}
			}

			reservation.setStatus(StockReservation.ReservationStatus.RELEASED);
			reservation.setReleasedAt(LocalDateTime.now());
			stockReservationRepository.save(reservation);

			logger.info("Stock release completed for saga: {}", sagaId);
		}, () -> logger.warn("No reservation found for saga: {}", sagaId));
	}

}
