package com.nirmalks.checkout_service.order.repository;

import com.nirmalks.checkout_service.order.entity.Order;
import com.nirmalks.checkout_service.order.entity.OrderStatus;

import saga.SagaState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

	long countByOrderStatus(OrderStatus orderStatus);

	@Query("SELECT o FROM Order o WHERE o.sagaState IN :states AND o.sagaStartedAt < :threshold")
	List<Order> findByPendingSagaStatesBefore(@Param("states") List<SagaState> states,
			@Param("threshold") LocalDateTime threshold);

	Optional<Order> findBySagaId(String sagaId);

}
