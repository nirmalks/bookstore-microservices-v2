package com.nirmalks.catalog_service.book.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public class StockReservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "saga_id", nullable = false, unique = true)
	private String sagaId;

	@Column(name = "order_id", nullable = false)
	private String orderId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "reserved_items", columnDefinition = "JSONB")
	private List<ReservedItem> reservedItems;

	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private ReservationStatus status = ReservationStatus.ACTIVE;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "released_at")
	private LocalDateTime releasedAt;

	public enum ReservationStatus {

		ACTIVE, RELEASED, CONVERTED

	}

	public record ReservedItem(Long bookId, int quantity) {
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setStatus(ReservationStatus status) {
		this.status = status;
	}

	public void setReleasedAt(LocalDateTime releasedAt) {
		this.releasedAt = releasedAt;
	}

	public String getSagaId() {
		return sagaId;
	}

	public String getOrderId() {
		return orderId;
	}

	public List<ReservedItem> getReservedItems() {
		return reservedItems;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setSagaId(String sagaId) {
		this.sagaId = sagaId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public void setReservedItems(List<ReservedItem> reservedItems) {
		this.reservedItems = reservedItems;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getReleasedAt() {
		return releasedAt;
	}

	public StockReservation() {

	}

	public StockReservation(String sagaId, String orderId, List<ReservedItem> reservedItems) {
		this.sagaId = sagaId;
		this.orderId = orderId;
		this.reservedItems = reservedItems;
	}

}