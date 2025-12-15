package com.nirmalks.checkout_service.order.messaging;

import com.nirmalks.checkout_service.config.RabbitMqConfig;
import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.repository.OrderRepository;
import com.nirmalks.checkout_service.order.service.OrderService;
import dto.StockReservationFailedEvent;
import dto.StockReservationSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RabbitListener(queues = RabbitMqConfig.ORDER_INVENTORY_RESULT_QUEUE)
public class InventoryResultConsumer {

	private final OrderService orderService;

	private final Logger logger = LoggerFactory.getLogger(InventoryResultConsumer.class);

	public InventoryResultConsumer(OrderService orderService) {
		this.orderService = orderService;
	}

	@RabbitHandler
	public void handleSuccess(StockReservationSuccessEvent event) {
		logger.info("updated success stock reservation event: {}", event);
		orderService.updateOrderStatusByEvent(event.orderId(), OrderStatus.CONFIRMED, "Stock confirmed.");
	}

	@RabbitHandler
	public void handleFailure(StockReservationFailedEvent event) {
		logger.info("updated failed stock reservation event: {}", event);
		orderService.updateOrderStatusByEvent(event.orderId(), OrderStatus.CANCELLED,
				"Stock reservation failed: " + event.reason());
	}

}
