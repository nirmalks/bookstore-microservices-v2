package com.nirmalks.checkout_service.cart.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirmalks.checkout_service.cart.api.CartItemRequest;
import com.nirmalks.checkout_service.cart.api.CartResponse;
import com.nirmalks.checkout_service.cart.dto.CartItemDto;
import com.nirmalks.checkout_service.cart.service.CartService;

import exceptions.GlobalExceptionHandler;
import exceptions.ResourceNotFoundException;

@WebMvcTest(controllers = CartController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CartControllerTest {

	@MockitoBean
	private CartService cartService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void getCart_returns_CartDto_when_cart_is_found() throws Exception {
		when(cartService.getCart(1L)).thenReturn(new CartResponse(1L, List.of(new CartItemDto(1L, 1L, 1, 10.0)), 10.0));
		mockMvc.perform(get("/api/v1/cart/{userId}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1L))
			.andExpect(jsonPath("$.items").isArray())
			.andExpect(jsonPath("$.items[0].id").value(1L))
			.andExpect(jsonPath("$.items[0].bookId").value(1L))
			.andExpect(jsonPath("$.items[0].quantity").value(1))
			.andExpect(jsonPath("$.items[0].price").value(10.0));
	}

	@Test
	void getCart_returns_404_when_cart_is_not_found() throws Exception {
		when(cartService.getCart(1L)).thenThrow(new ResourceNotFoundException("Cart not found"));
		mockMvc.perform(get("/api/v1/cart/{userId}", 1L)).andExpect(status().isNotFound());
	}

	@Test
	void addToCart_returns_CartDto_when_book_is_added() throws Exception {
		when(cartService.addToCart(1L, new CartItemRequest(1L, 1)))
			.thenReturn(new CartResponse(1L, List.of(new CartItemDto(1L, 1L, 1, 10.0)), 10.0));
		mockMvc
			.perform(post("/api/v1/cart/{userId}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CartItemRequest(1L, 1))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1L))
			.andExpect(jsonPath("$.items").isArray())
			.andExpect(jsonPath("$.items[0].id").value(1L))
			.andExpect(jsonPath("$.items[0].bookId").value(1L))
			.andExpect(jsonPath("$.items[0].quantity").value(1))
			.andExpect(jsonPath("$.items[0].price").value(10.0));
	}

	@Test
	void addToCart_returns_404_when_book_is_not_found() throws Exception {
		when(cartService.addToCart(1L, new CartItemRequest(1L, 1)))
			.thenThrow(new ResourceNotFoundException("Book not found"));
		mockMvc
			.perform(post("/api/v1/cart/{userId}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CartItemRequest(1L, 1))))
			.andExpect(status().isNotFound());
	}

	@Test
	void clearCart_returns_200_when_cart_is_cleared() throws Exception {
		mockMvc.perform(delete("/api/v1/cart/{userId}", 1L)).andExpect(status().isOk());
		verify(cartService).clearCart(1L);
	}

	@Test
	void clearCart_returns_404_when_cart_is_not_found() throws Exception {
		doThrow(new ResourceNotFoundException("Cart not found")).when(cartService).clearCart(1L);
		mockMvc.perform(delete("/api/v1/cart/{userId}", 1L)).andExpect(status().isNotFound());
	}

	@Test
	void removeItemFromCart_returns_200_when_item_is_removed() throws Exception {
		mockMvc.perform(delete("/api/v1/cart/{cartId}/item/{itemId}", 1L, 1L)).andExpect(status().isOk());
		verify(cartService).removeItemFromCart(1L, 1L);
	}

	@Test
	void removeItemFromCart_returns_404_when_item_is_not_found() throws Exception {
		doThrow(new ResourceNotFoundException("Item not found")).when(cartService).removeItemFromCart(1L, 1L);
		mockMvc.perform(delete("/api/v1/cart/{cartId}/item/{itemId}", 1L, 1L)).andExpect(status().isNotFound());
	}

}
