package com.nirmalks.checkout_service.order.entity;

public enum OrderStatus {

	PENDING("Pending"), CONFIRMED("Confirmed"), SHIPPED("Shipped"), DELIVERED("Delivered"), CANCELLED("Cancelled");

	private final String status;

	OrderStatus(String status) {
		this.status = status;
	}

	public String getStatus() {
		return status;
	}

}
