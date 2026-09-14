package com.techloom.ecommerce.repository;

import com.techloom.ecommerce.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(Long id);

    boolean existsByCartId(String cartId);

    Optional<Order> findByCartId(String cartId);

    Optional<Order> findByPaymentReference(String paymentReference);

    List<Order> findByCustomerNameIgnoreCaseOrderByCreatedAtDesc(String customerName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.status = :status and o.reservationExpiresAt <= :now")
    List<Order> findExpiredReservations(com.techloom.ecommerce.entity.OrderStatus status, LocalDateTime now);
}
