package com.techloom.ecommerce.dto;

import com.techloom.ecommerce.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private String customerName;
    private OrderStatus status;
    private Double totalAmount;
    private String paymentMethod;
    private String paymentReference;
    private LocalDateTime createdAt;
    private String cancellationReason;
    private LocalDateTime refundedAt;
    private List<OrderItemResponse> items;
}
