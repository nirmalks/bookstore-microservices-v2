package com.nirmalks.checkout_service.order.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import saga.SagaState;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "purchase_order")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	private Double totalCost;

	@Column(name = "status", columnDefinition = "order_status_enum")
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	private OrderStatus orderStatus;

	@Column(name = "placed_date", nullable = false, updatable = false)
	private LocalDateTime placedDate;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<OrderItem> items = new ArrayList<>();

	@Embedded
	private ShippingAddress shippingAddress;

	@Column(name = "saga_id")
	private String sagaId;

	@Column(name = "saga_state")
	@Enumerated(EnumType.STRING)
	private SagaState sagaState;

	@Column(name = "saga_started_at")
	private LocalDateTime sagaStartedAt;

	@Column(name = "saga_completed_at")
	private LocalDateTime sagaCompletedAt;

	@Column(name = "compensation_reason")
	private String compensationReason;

	public SagaState getSagaState() {
		return sagaState;
	}

	public String getCompensationReason() {
		return compensationReason;
	}

	public void setSagaState(SagaState sagaState) {
		this.sagaState = sagaState;
	}

	public void setSagaId(String sagaId) {
		this.sagaId = sagaId;
	}

	public void setSagaStartedAt(LocalDateTime sagaStartedAt) {
		this.sagaStartedAt = sagaStartedAt;
	}

	public void setSagaCompletedAt(LocalDateTime sagaCompletedAt) {
		this.sagaCompletedAt = sagaCompletedAt;
	}

	public void setCompensationReason(String compensationReason) {
		this.compensationReason = compensationReason;
	}

	public LocalDateTime getSagaStartedAt() {
		return sagaStartedAt;
	}

	public String getSagaId() {
		return sagaId;
	}

	public LocalDateTime getSagaCompletedAt() {
		return sagaCompletedAt;
	}

	public ShippingAddress getShippingAddress() {
		return shippingAddress;
	}

	public void setShippingAddress(ShippingAddress shippingAddress) {
		this.shippingAddress = shippingAddress;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(Double totalCost) {
		this.totalCost = totalCost;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	public LocalDateTime getPlacedDate() {
		return placedDate;
	}

	public void setPlacedDate(LocalDateTime placedDate) {
		this.placedDate = placedDate;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public void setItems(List<OrderItem> items) {
		this.items = items;
	}

	@PrePersist
	public void setPlacedDate() {
		if (this.placedDate == null) {
			this.placedDate = LocalDateTime.now();
		}
	}

	public Double calculateTotalCost() {
		return this.items.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Order order = (Order) o;
		return Objects.equals(id, order.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	@Override
	public String toString() {
		return "Order{" + "id=" + id + ", userId=" + userId + ", totalCost=" + totalCost + ", orderStatus="
				+ orderStatus + ", placedDate=" + placedDate + ", items=" + items + '}';
	}

}
