package com.nirmalks.checkout_service.order.dto;

public record AddressRequest(String city, String state, String country, String pinCode, boolean isDefault,
		String address) {
}
