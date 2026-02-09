package com.nirmalks.catalog_service.book.repository;

import com.nirmalks.catalog_service.book.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

	Optional<StockReservation> findBySagaId(String sagaId);

}
