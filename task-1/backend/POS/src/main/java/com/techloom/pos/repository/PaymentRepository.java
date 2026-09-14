package com.techloom.pos.repository;

import com.techloom.pos.entity.Payment;
import com.techloom.pos.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrder_IdAndPaymentReference(Long orderId, String paymentReference);

    boolean existsByOrder_IdAndStatus(Long orderId, PaymentStatus status);
}