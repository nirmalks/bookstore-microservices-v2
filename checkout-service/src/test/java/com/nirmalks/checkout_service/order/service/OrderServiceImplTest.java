package com.nirmalks.checkout_service.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.nirmalks.checkout_service.client.CatalogServiceClient;
import com.nirmalks.checkout_service.client.UserServiceClient;
import com.nirmalks.checkout_service.common.UserDto;
import com.nirmalks.checkout_service.metrics.OrderMetrics;
import com.nirmalks.checkout_service.order.api.DirectOrderRequest;
import com.nirmalks.checkout_service.order.api.OrderResponse;
import com.nirmalks.checkout_service.order.dto.AddressRequest;
import com.nirmalks.checkout_service.order.dto.OrderSummaryDto;
import com.nirmalks.checkout_service.order.entity.Order;
import com.nirmalks.checkout_service.order.entity.OrderItem;
import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.repository.OrderItemRepository;
import com.nirmalks.checkout_service.order.repository.OrderRepository;
import com.nirmalks.checkout_service.order.service.impl.OrderServiceImpl;
import com.nirmalks.checkout_service.saga.SagaMetrics;

import common.RequestUtils;
import dto.OrderMessage;
import dto.PageRequestDto;
import io.micrometer.core.instrument.Timer;
import locking.DistributedLockService;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

	@InjectMocks
	private OrderServiceImpl orderService;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private CatalogServiceClient catalogServiceClient;

	@Mock
	private UserServiceClient userServiceClient;

	@Mock
	private OutboxService outboxService;

	@Mock
	private OrderMetrics orderMetrics;

	@Mock
	private DistributedLockService distributedLockService;

	@Mock
	private SagaMetrics sagaMetrics;

	AddressRequest addressRequest;

	UserDto userDto;

	Order order;

	OrderItem orderItem;

	@BeforeEach
	void setup() {
		addressRequest = new AddressRequest("City", "State", "Country", "123456", false, "Address");
		userDto = new UserDto("user@example.com", 1L, "username");
		order = new Order();
		order.setId(100L);
		order.setTotalCost(0.0);
		order.setOrderStatus(OrderStatus.PENDING);

		orderItem = new OrderItem();
		orderItem.setId(100L);
		orderItem.setQuantity(1);
		orderItem.setPrice(1);
		orderItem.setBookId(1L);
		orderItem.setOrder(order);
	}

	@Test
	void createOrder_should_create_order_successfully_with_valid_request() {
		DirectOrderRequest directOrderRequest = new DirectOrderRequest(1L, new ArrayList<>(), addressRequest);
		when(userServiceClient.getUser(1L)).thenReturn(userDto);

		List<OrderItem> orderItems = List.of(orderItem);
		order.setItems(orderItems);
		when(orderMetrics.startOrderCreationTimer()).thenReturn(Timer.start());
		when(orderRepository.save(any(Order.class))).thenReturn(order);
		when(orderItemRepository.saveAll(any(List.class))).thenReturn(orderItems);
		doNothing().when(outboxService).saveOrderCreatedEvent(any(String.class), any(OrderMessage.class));
		OrderResponse response = orderService.createOrder(directOrderRequest);
		assertNotNull(response);
		assertEquals(order.getId(), response.order().id());
		assertEquals(order.getTotalCost(), response.order().totalCost());
		assertEquals("PENDING", response.order().status());
		assertEquals("Order placed successfully.", response.message());
		assertEquals(1, response.order().items().size());
		assertEquals(1, response.order().items().get(0).quantity());
		assertEquals(1, response.order().items().get(0).bookId());
		assertEquals(1, response.order().items().get(0).price());

		verify(outboxService, times(1)).saveOrderCreatedEvent(any(String.class), any(OrderMessage.class));
		verify(orderMetrics, times(1)).incrementOrdersCreated();
		verify(sagaMetrics, times(1)).incrementSagasStarted();
		verify(orderMetrics, times(1)).recordOrderByStatus(any(String.class));
		verify(orderMetrics, times(1)).recordRevenue(any(Double.class));
		verify(orderMetrics, times(1)).stopOrderCreationTimer(any(Timer.Sample.class));

	}

	@Test
	void createOrder_should_throw_exception_when_user_is_not_found() {
		DirectOrderRequest directOrderRequest = new DirectOrderRequest(1L, new ArrayList<>(), addressRequest);
		when(userServiceClient.getUser(1L)).thenThrow(new RuntimeException("User not found"));
		assertThrows(RuntimeException.class, () -> orderService.createOrder(directOrderRequest));
	}

	@Test
	void createOrder_should_throw_exception_when_cart_is_empty() {
		DirectOrderRequest directOrderRequest = new DirectOrderRequest(1L, new ArrayList<>(), addressRequest);
		UserDto userDto = new UserDto("user@example.com", 1L, "username");
		when(userServiceClient.getUser(1L)).thenReturn(userDto);
		assertThrows(RuntimeException.class, () -> orderService.createOrder(directOrderRequest));
	}

	@Test
	void getOrder_should_return_order_when_found() {
		List<OrderItem> orderItems = List.of(orderItem);
		order.setItems(orderItems);
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		Order response = orderService.getOrder(100L);
		assertNotNull(response);
		assertEquals(order.getId(), response.getId());
		assertEquals(order.getTotalCost(), response.getTotalCost());
		assertEquals(OrderStatus.PENDING, response.getOrderStatus());
		assertEquals(1, response.getItems().size());
		assertEquals(1, response.getItems().get(0).getQuantity());
		assertEquals(1, response.getItems().get(0).getBookId());
		assertEquals(1, response.getItems().get(0).getPrice());
	}

	@Test
	void getOrder_should_throw_exception_when_order_is_not_found() {
		when(orderRepository.findById(100L)).thenReturn(Optional.empty());
		assertThrows(RuntimeException.class, () -> orderService.getOrder(100L));
	}

	@Test
	void getOrdersByUser_should_return_orders_when_found() {
		List<OrderItem> orderItems = List.of(orderItem);
		order.setItems(orderItems);
		PageRequestDto pageRequestDto = new PageRequestDto(0, 10, "id", "asc");

		when(userServiceClient.getUser(1L)).thenReturn(userDto);
		List<Order> orders = List.of(order);
		when(orderRepository.findAllByUserId(1L, RequestUtils.getPageable(pageRequestDto)))
			.thenReturn(new PageImpl<>(orders));

		Page<OrderSummaryDto> response = orderService.getOrdersByUser(1L, pageRequestDto);
		assertNotNull(response);
		assertEquals(1, response.getSize());
		assertEquals(order.getId(), response.getContent().get(0).id());
		assertEquals(order.getTotalCost(), response.getContent().get(0).totalCost());
		assertEquals(OrderStatus.PENDING.name(), response.getContent().get(0).status());
		assertEquals(1, response.getContent().get(0).items().size());
		assertEquals(1, response.getContent().get(0).items().get(0).quantity());
		assertEquals(1, response.getContent().get(0).items().get(0).bookId());
		assertEquals(1, response.getContent().get(0).items().get(0).price());
	}

	@Test
	void getOrdersByUser_should_throw_exception_when_user_is_not_found() {
		when(userServiceClient.getUser(1L)).thenThrow(new RuntimeException("User not found"));
		assertThrows(RuntimeException.class,
				() -> orderService.getOrdersByUser(1L, new PageRequestDto(0, 10, "id", "asc")));
	}

	@Test
	void updateOrderStatus_should_update_order_status_successfully_when_found() {
		List<OrderItem> orderItems = List.of(orderItem);
		order.setItems(orderItems);
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		orderService.updateOrderStatus(100L, OrderStatus.SHIPPED);
		assertEquals(OrderStatus.SHIPPED, order.getOrderStatus());
		verify(orderRepository, times(1)).save(order);
	}

	@Test
	void updateOrderStatus_should_throw_exception_when_order_is_not_found() {
		when(orderRepository.findById(100L)).thenReturn(Optional.empty());
		assertThrows(RuntimeException.class, () -> orderService.updateOrderStatus(100L, OrderStatus.SHIPPED));
	}

	@Test
	void updateOrderStatus_should_throw_exception_when_invalid_status() {
		assertThrows(IllegalArgumentException.class,
				() -> orderService.updateOrderStatus(100L, OrderStatus.valueOf("INVALID")));
	}

	@Test
	void updateOrderStatusByEvent_should_update_order_status_successfully_when_found() {
		List<OrderItem> orderItems = List.of(orderItem);
		order.setItems(orderItems);
		when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
		doAnswer(invocation -> {
			Runnable runnable = invocation.getArgument(4);
			runnable.run();
			return null;
		}).when(distributedLockService)
			.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

		orderService.updateOrderStatusByEvent(order.getId().toString(), OrderStatus.SHIPPED, "SHIPPED");
		assertEquals(OrderStatus.SHIPPED, order.getOrderStatus());
		verify(orderRepository, times(1)).save(order);
	}

	@Test
	void updateOrderStatusByEvent_should_throw_exception_when_order_is_not_found() {
		doThrow(new RuntimeException("Order not found")).when(distributedLockService)
			.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));
		assertThrows(RuntimeException.class,
				() -> orderService.updateOrderStatusByEvent(order.getId().toString(), OrderStatus.SHIPPED, "SHIPPED"));
	}

}
