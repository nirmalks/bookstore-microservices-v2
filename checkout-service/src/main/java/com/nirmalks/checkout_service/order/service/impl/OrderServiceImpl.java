package com.nirmalks.checkout_service.order.service.impl;

import com.nirmalks.checkout_service.cart.entity.Cart;
import com.nirmalks.checkout_service.cart.entity.CartItem;
import com.nirmalks.checkout_service.cart.repository.CartRepository;
import com.nirmalks.checkout_service.client.CatalogServiceClient;
import com.nirmalks.checkout_service.client.UserServiceClient;
import com.nirmalks.checkout_service.common.BookDto;
import com.nirmalks.checkout_service.common.UserDto;
import com.nirmalks.checkout_service.metrics.OrderMetrics;
import com.nirmalks.checkout_service.order.api.DirectOrderRequest;
import com.nirmalks.checkout_service.order.api.OrderFromCartRequest;
import com.nirmalks.checkout_service.order.api.OrderResponse;
import com.nirmalks.checkout_service.order.dto.OrderMapper;
import com.nirmalks.checkout_service.order.dto.OrderSummaryDto;
import com.nirmalks.checkout_service.order.entity.Order;
import com.nirmalks.checkout_service.order.entity.OrderItem;
import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.repository.OrderItemRepository;
import com.nirmalks.checkout_service.order.repository.OrderRepository;
import com.nirmalks.checkout_service.order.service.OrderService;
import com.nirmalks.checkout_service.order.service.OutboxService;
import com.nirmalks.checkout_service.saga.SagaMetrics;

import common.RequestUtils;
import dto.OrderItemPayload;
import dto.OrderMessage;
import dto.PageRequestDto;
import exceptions.ResourceNotFoundException;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import locking.DistributedLockService;
import locking.LockKeys;
import saga.SagaState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Order service implementation that handles order creation and management.
 */
@Service
public class OrderServiceImpl implements OrderService {

	private final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

	private final OrderRepository orderRepository;

	private final OrderItemRepository orderItemRepository;

	private final CartRepository cartRepository;

	private final CatalogServiceClient catalogServiceClient;

	private final UserServiceClient userServiceClient;

	private final OutboxService outboxService;

	private final OrderMetrics orderMetrics;

	private final DistributedLockService distributedLockService;

	private final SagaMetrics sagaMetrics;

	@Autowired
	public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
			CartRepository cartRepository, CatalogServiceClient catalogServiceClient,
			UserServiceClient userServiceClient, OutboxService outboxService, OrderMetrics orderMetrics,
			DistributedLockService distributedLockService, SagaMetrics sagaMetrics) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.cartRepository = cartRepository;
		this.catalogServiceClient = catalogServiceClient;
		this.userServiceClient = userServiceClient;
		this.outboxService = outboxService;
		this.orderMetrics = orderMetrics;
		this.distributedLockService = distributedLockService;
		this.sagaMetrics = sagaMetrics;
	}

	@Override
	@Transactional
	public OrderResponse createOrder(DirectOrderRequest directOrderRequest) {
		Timer.Sample sample = orderMetrics.startOrderCreationTimer();
		try {
			var user = userServiceClient.getUser(directOrderRequest.getUserId());

			var itemDtos = directOrderRequest.getItems();
			var order = OrderMapper.toOrderEntity(user, directOrderRequest.getAddress());

			// Initialize Saga State
			String sagaId = UUID.randomUUID().toString();
			order.setSagaId(sagaId);
			order.setSagaState(SagaState.SAGA_STARTED);
			order.setSagaStartedAt(LocalDateTime.now());

			ExecutorService executor = Executors.newFixedThreadPool(8);
			List<CompletableFuture<OrderItem>> orderItemFutures = itemDtos.stream()
				.map(itemDto -> CompletableFuture.supplyAsync(() -> {
					var book = catalogServiceClient.getBook(itemDto.getBookId());
					return OrderMapper.toOrderItemEntity(book, itemDto, order);
				}, executor))
				.toList();
			List<OrderItem> orderItems = orderItemFutures.stream().map(CompletableFuture::join).toList();
			executor.shutdown();
			order.setItems(orderItems);
			order.setTotalCost(order.calculateTotalCost());

			var savedOrder = orderRepository.save(order);
			// Update saga state before publishing event
			savedOrder.setSagaState(SagaState.STOCK_RESERVATION_PENDING);

			orderItemRepository.saveAll(orderItems);

			List<OrderItemPayload> itemPayloads = orderItems.stream()
				.map(item -> new OrderItemPayload(item.getBookId(), item.getQuantity()))
				.toList();

			OrderMessage message = new OrderMessage(null, sagaId, savedOrder.getId().toString(), user.getId(),
					user.getEmail(), savedOrder.getTotalCost(), savedOrder.getPlacedDate(), itemPayloads);
			outboxService.saveOrderCreatedEvent(savedOrder.getId().toString(), message);

			// Record metrics
			orderMetrics.incrementOrdersCreated();
			sagaMetrics.incrementSagasStarted();
			orderMetrics.recordOrderByStatus(savedOrder.getOrderStatus().name());
			orderMetrics.recordRevenue(savedOrder.getTotalCost());

			return OrderMapper.toResponse(user, savedOrder, "Order placed successfully.");
		}
		catch (Exception e) {
			orderMetrics.incrementOrdersFailed();
			sagaMetrics.incrementSagasFailed();
			throw e;
		}
		finally {
			orderMetrics.stopOrderCreationTimer(sample);
		}
	}

	@Override
	public OrderResponse createOrder(OrderFromCartRequest orderFromCartRequest) {
		Cart cart = cartRepository.findById(orderFromCartRequest.getCartId())
			.orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
		List<OrderItem> orderItems = new ArrayList<>();

		UserDto user = userServiceClient.getUser(orderFromCartRequest.getUserId());
		Order order = OrderMapper.toOrderEntity(user, orderFromCartRequest.getShippingAddress());

		for (CartItem cartItem : cart.getCartItems()) {
			BookDto book = catalogServiceClient.getBook(cartItem.getBookId());

			var orderItem = OrderMapper.toOrderItemEntity(book, cartItem, order);
			orderItems.add(orderItem);
		}

		order.setUserId(orderFromCartRequest.getUserId());
		order.setItems(orderItems);
		order.setTotalCost(cart.getTotalPrice());
		order.setOrderStatus(OrderStatus.PENDING);
		order.setPlacedDate(LocalDateTime.now());
		Order savedOrder = orderRepository.save(order);

		for (OrderItem item : orderItems) {
			item.setOrder(savedOrder);
			orderItemRepository.save(item);
		}
		return OrderMapper.toResponse(user, savedOrder, "Order placed successfully.");
	}

	public Page<OrderSummaryDto> getOrdersByUser(Long userId, PageRequestDto pageRequestDto) {
		var user = userServiceClient.getUser(userId);
		var pageable = RequestUtils.getPageable(pageRequestDto);
		var orders = orderRepository.findAllByUserId(userId, pageable);
		return orders.map(order -> OrderMapper.toOrderSummary(order, user));
	}

	@Override
	public Order getOrder(Long orderId) {
		return orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
	}

	public void updateOrderStatus(Long orderId, OrderStatus status) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("Order not found"));
		order.setOrderStatus(status);
		orderRepository.save(order);
	}

	@Transactional
	public void updateOrderStatusByEvent(String orderIdString, OrderStatus newStatus, String reason) {
		String lockKey = LockKeys.orderStatus(orderIdString);

		distributedLockService.executeWithLock(lockKey, 5, 30, TimeUnit.SECONDS,
				() -> doUpdateOrderStatus(orderIdString, newStatus, reason));
	}

	private void doUpdateOrderStatus(String orderIdString, OrderStatus newStatus, String reason) {
		try {
			Long orderId = Long.valueOf(orderIdString);
			Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

			if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.CONFIRMED) {
				logger.warn("Order {} is already {}, ignoring update to {}", orderId, order.getOrderStatus(),
						newStatus);
				return;
			}

			order.setOrderStatus(newStatus);
			orderRepository.save(order);
			logger.info("Updated Order {} status to {}. Reason: {}", orderId, newStatus, reason);
		}
		catch (Exception e) {
			logger.error("Failed to update order status for ID: {}", orderIdString, e);
			throw e;
		}
	}

}
