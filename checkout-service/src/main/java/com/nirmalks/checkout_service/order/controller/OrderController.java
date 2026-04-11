package com.nirmalks.checkout_service.order.controller;

import com.nirmalks.checkout_service.order.api.DirectOrderRequest;
import com.nirmalks.checkout_service.order.api.OrderResponse;
import com.nirmalks.checkout_service.order.dto.OrderSummaryDto;
import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.service.OrderService;
import common.RestPage;
import dto.PageRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Order Management", description = "Operations related to user orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping("/direct")
	@Operation(summary = "Create a direct order",
			description = "Creates an order directly, bypassing the shopping cart.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Order created successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid input or item not available") })
	public ResponseEntity<OrderResponse> createDirectOrder(@RequestBody DirectOrderRequest orderRequest) {
		var order = orderService.createOrder(orderRequest);
		return ResponseEntity.ok(order);
	}

	@GetMapping("/{userId}")
	@Operation(summary = "Get orders by user",
			description = "Retrieves a paginated list of orders for a specific user.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "List of orders returned successfully"),
			@ApiResponse(responseCode = "404", description = "User not found") })
	public ResponseEntity<RestPage<OrderSummaryDto>> getOrdersByUser(@PathVariable Long userId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortKey, @RequestParam(defaultValue = "asc") String sortOrder) {
		PageRequestDto pageRequestDto = new PageRequestDto(page, size, sortKey, sortOrder);
		var orders = orderService.getOrdersByUser(userId, pageRequestDto);
		return ResponseEntity.ok(new RestPage<>(orders));
	}

	@PutMapping("/{orderId}")
	@Operation(summary = "Update order status",
			description = "Updates the status of a specific order. Accessible by ADMIN role only.") // Assuming
																									// this
																									// is
																									// admin
																									// only
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid status or input"),
			@ApiResponse(responseCode = "404", description = "Order not found") })
	public ResponseEntity<Void> updateOrderStatus(@PathVariable Long orderId, @RequestParam String status) {
		orderService.updateOrderStatus(orderId, OrderStatus.valueOf(status));
		return ResponseEntity.ok().build();
	}

}