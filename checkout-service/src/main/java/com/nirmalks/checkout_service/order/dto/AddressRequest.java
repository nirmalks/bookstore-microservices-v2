package com.nirmalks.checkout_service.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(@NotBlank(message = "City is required") String city,
		@NotBlank(message = "State is required") String state,
		@NotBlank(message = "Country is required") String country,
		@NotBlank(message = "Pin code is required") @Size(min = 5, max = 10,
				message = "Invalid pin code length") String pinCode,
		boolean isDefault, @NotBlank(message = "Address detail is required") String address) {
}
