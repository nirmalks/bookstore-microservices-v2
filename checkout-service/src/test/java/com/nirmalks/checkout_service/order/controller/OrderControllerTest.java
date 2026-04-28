package com.nirmalks.checkout_service.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nirmalks.checkout_service.order.api.DirectOrderRequest;
import com.nirmalks.checkout_service.order.api.OrderResponse;
import com.nirmalks.checkout_service.order.dto.AddressRequest;
import com.nirmalks.checkout_service.order.dto.OrderSummaryDto;
import com.nirmalks.checkout_service.order.service.OrderService;

class OrderControllerTest {

	private MockMvc mockMvc;

	private OrderService orderService;

	private ObjectMapper objectMapper;

	private AddressRequest addressRequest;

	private DirectOrderRequest directOrderRequest;

	private OrderResponse orderResponse;

	@BeforeEach
	void setUp() {
		orderService = mock(OrderService.class);
		OrderController orderController = new OrderController(orderService);

		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		mockMvc = MockMvcBuilders.standaloneSetup(orderController)
			.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
			.build();

		addressRequest = new AddressRequest("City", "State", "Country", "123456", false, "Address");
		directOrderRequest = new DirectOrderRequest(1L, new ArrayList<>(), addressRequest);

		OrderSummaryDto orderSummary = new OrderSummaryDto(100L, "PENDING", LocalDateTime.now(), 100.0,
				new ArrayList<>(), null);

		orderResponse = new OrderResponse(orderSummary, null, "Order placed successfully.");
	}

	@Test
	void createDirectOrder_should_create_order_successfully() throws Exception {
		when(orderService.createOrder(any(DirectOrderRequest.class))).thenReturn(orderResponse);

		mockMvc
			.perform(post("/api/v1/orders/direct").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(directOrderRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Order placed successfully."))
			.andExpect(jsonPath("$.order.id").value(100));
	}

	@Test
	void getOrdersByUser_should_return_orders_successfully() throws Exception {
		OrderSummaryDto orderSummary = new OrderSummaryDto(100L, "PENDING", LocalDateTime.now(), 100.0,
				new ArrayList<>(), null);
		PageImpl<OrderSummaryDto> page = new PageImpl<>(List.of(orderSummary));

		when(orderService.getOrdersByUser(any(), any())).thenReturn(page);

		mockMvc
			.perform(get("/api/v1/orders/1").param("page", "0")
				.param("size", "10")
				.param("sortKey", "id")
				.param("sortOrder", "asc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(100));
	}

	@Test
	void updateOrderStatus_should_update_status_successfully() throws Exception {
		mockMvc.perform(put("/api/v1/orders/100").param("status", "CONFIRMED")).andExpect(status().isOk());
	}

}
