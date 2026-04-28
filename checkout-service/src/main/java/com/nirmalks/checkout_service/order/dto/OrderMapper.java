package com.nirmalks.checkout_service.order.dto;

import com.nirmalks.checkout_service.cart.entity.CartItem;
import com.nirmalks.checkout_service.order.entity.ShippingAddress;
import com.nirmalks.checkout_service.common.BookDto;
import com.nirmalks.checkout_service.common.UserDto;
import com.nirmalks.checkout_service.order.api.OrderItemRequest;
import com.nirmalks.checkout_service.order.api.OrderResponse;
import com.nirmalks.checkout_service.order.entity.Order;
import com.nirmalks.checkout_service.order.entity.OrderItem;
import com.nirmalks.checkout_service.order.entity.OrderStatus;

import java.time.LocalDateTime;

public class OrderMapper {

	public static ShippingAddress toShippingAddressEntity(AddressRequest addressRequest) {
		if (addressRequest == null) {
			return null;
		}
		ShippingAddress shippingAddress = new ShippingAddress();
		shippingAddress.setAddress(addressRequest.address());
		shippingAddress.setCity(addressRequest.city());
		shippingAddress.setState(addressRequest.state());
		shippingAddress.setPinCode(addressRequest.pinCode());
		shippingAddress.setCountry(addressRequest.country());
		return shippingAddress;
	}

	public static AddressDto toAddressDto(ShippingAddress shippingAddress) {
		if (shippingAddress == null) {
			return null;
		}
		return new AddressDto(shippingAddress.getCity(), shippingAddress.getState(), shippingAddress.getCountry(),
				shippingAddress.getPinCode(), shippingAddress.getAddress());
	}

	public static Order toOrderEntity(UserDto user, AddressRequest addressRequest) {
		Order order = new Order();
		order.setUserId(user.id());
		order.setOrderStatus(OrderStatus.PENDING);
		order.setPlacedDate(LocalDateTime.now());
		order.setTotalCost(0.0);
		order.setShippingAddress(OrderMapper.toShippingAddressEntity(addressRequest));
		return order;
	}

	public static OrderItem toOrderItemEntity(BookDto book, CartItem cartItem, Order order) {
		var orderItem = new OrderItem();
		orderItem.setBookId(cartItem.getBookId());
		orderItem.setQuantity(cartItem.getQuantity());
		orderItem.setPrice(book.getPrice());
		orderItem.setBookTitle(book.getTitle());
		orderItem.setOrder(order);
		return orderItem;
	}

	public static OrderItem toOrderItemEntity(BookDto book, OrderItemRequest itemDto, Order order) {
		var orderItem = new OrderItem();
		orderItem.setBookId(book.getId());
		orderItem.setQuantity(itemDto.quantity());
		orderItem.setPrice(book.getPrice());
		orderItem.setBookTitle(book.getTitle());
		orderItem.setOrder(order);
		return orderItem;
	}

	public static OrderResponse toResponse(UserDto user, Order order, String message) {
		return new OrderResponse(toOrderSummary(order, user), user, message);
	}

	public static OrderSummaryDto toOrderSummary(Order order, UserDto user) {
		return new OrderSummaryDto(order.getId(), order.getOrderStatus().toString(), order.getPlacedDate(),
				order.getTotalCost(), order.getItems().stream().map(OrderMapper::toOrderItemDto).toList(),
				OrderMapper.toAddressDto(order.getShippingAddress()));
	}

	public static OrderItemDto toOrderItemDto(OrderItem orderItem) {
		Long orderId = orderItem.getOrder() != null ? orderItem.getOrder().getId() : null;
		return new OrderItemDto(orderItem.getId(), orderId, orderItem.getBookId(), orderItem.getQuantity(),
				orderItem.getPrice(), orderItem.getBookTitle());
	}

}