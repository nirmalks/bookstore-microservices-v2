package com.nirmalks.checkout_service.order.repository;

import com.nirmalks.checkout_service.order.entity.Outbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findByStatusOrderByCreatedAtAsc(Outbox.EventStatus status, Pageable pageable);
}