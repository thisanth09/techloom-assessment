package com.techloom.pos.repository;

import com.techloom.pos.entity.Order;
import com.techloom.pos.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByCartId(String cartId);

    Optional<Order> findByCartId(String cartId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.status = :status and o.reservationExpiresAt is not null and o.reservationExpiresAt < :now")
    List<Order> findExpiredReservations(@Param("status") OrderStatus status, @Param("now") LocalDateTime now);
}
