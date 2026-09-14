package com.techloom.pos.dto;

import com.techloom.pos.entity.PaymentMethod;
import com.techloom.pos.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private String paymentReference;
    private PaymentMethod paymentMethod;
    private Double amount;
    private PaymentStatus status;
    private LocalDateTime createdAt;
}
