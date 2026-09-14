package com.techloom.ecommerce.service;

import com.techloom.ecommerce.dto.CheckoutItemRequest;
import com.techloom.ecommerce.dto.CheckoutRequest;
import com.techloom.ecommerce.dto.OrderItemResponse;
import com.techloom.ecommerce.dto.OrderResponse;
import com.techloom.ecommerce.entity.Order;
import com.techloom.ecommerce.entity.OrderItem;
import com.techloom.ecommerce.entity.OrderStatus;
import com.techloom.ecommerce.entity.Product;
import com.techloom.ecommerce.exception.ResourceNotFoundException;
import com.techloom.ecommerce.repository.OrderRepository;
import com.techloom.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int RESERVATION_MINUTES = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        if (request.getCartId() != null && !request.getCartId().isBlank()) {
            // A retried request must reuse its original order rather than reserve stock twice.
            var existingOrder = orderRepository.findByCartId(request.getCartId());
            if (existingOrder.isPresent()) {
                return mapToResponse(existingOrder.get());
            }
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0.0;

        for (CheckoutItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdForUpdate(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException("Product is currently unavailable: " + product.getName());
            }

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            double lineTotal = product.getPrice() * itemRequest.getQuantity();
            total += lineTotal;

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            orderItems.add(orderItem);
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .paymentMethod(request.getPaymentMethod())
                .cartId(blankToNull(request.getCartId()))
                .status(OrderStatus.RESERVED)
                .totalAmount(total)
                .reservedAt(now)
                .reservationExpiresAt(now.plusMinutes(RESERVATION_MINUTES))
                .items(orderItems)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        return mapToResponse(orderRepository.save(savedOrder));
    }

    @Transactional
    public OrderResponse simulatePayment(Long orderId, String paymentStatus, String paymentReference) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalArgumentException("Payment is not allowed for an order in status: " + order.getStatus());
        }

        if (order.getReservationExpiresAt() != null && !order.getReservationExpiresAt().isAfter(LocalDateTime.now())) {
            releaseInventory(order);
            order.setStatus(OrderStatus.EXPIRED);
            order.setReservationExpiresAt(null);
            order.setCancellationReason("Checkout reservation expired before payment");
            return mapToResponse(orderRepository.save(order));
        }

        String normalizedPaymentReference = paymentReference == null ? null : paymentReference.trim();
        if (normalizedPaymentReference == null || normalizedPaymentReference.isBlank()) {
            throw new IllegalArgumentException("Payment reference is required");
        }

        var existingPayment = orderRepository.findByPaymentReference(normalizedPaymentReference);
        if (existingPayment.isPresent() && !existingPayment.get().getId().equals(orderId)) {
            throw new IllegalArgumentException("Payment reference has already been used");
        }

        order.setPaymentReference(normalizedPaymentReference);

        if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            order.setReservationExpiresAt(null);
        } else if ("FAILED".equalsIgnoreCase(paymentStatus)) {
            order.setStatus(OrderStatus.FAILED);
            releaseInventory(order);
        } else if ("TIMEOUT".equalsIgnoreCase(paymentStatus)) {
            order.setStatus(OrderStatus.EXPIRED);
            releaseInventory(order);
        } else {
            throw new IllegalArgumentException("Unsupported payment status: " + paymentStatus);
        }

        return mapToResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long id, String reason) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
            throw new IllegalArgumentException("Order cannot be cancelled in its current state");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason != null && !reason.isBlank() ? reason : "Customer cancelled order");
        order.setCancelledAt(LocalDateTime.now());
        order.setReservationExpiresAt(null);
        if (previousStatus == OrderStatus.RESERVED || previousStatus == OrderStatus.PAID) {
            releaseInventory(order);
        }
        if (order.getPaidAt() != null) {
            order.setRefundedAt(LocalDateTime.now());
        }
        return mapToResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getOrders(String customerName) {
        List<Order> orders = customerName == null || customerName.isBlank()
                ? orderRepository.findAll()
                : orderRepository.findByCustomerNameIgnoreCaseOrderByCreatedAtDesc(customerName.trim());
        return orders.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    /** Runs periodically and restores stock for abandoned checkouts. */
    @Transactional
    public void expireReservations() {
        for (Order order : orderRepository.findExpiredReservations(OrderStatus.RESERVED, LocalDateTime.now())) {
            releaseInventory(order);
            order.setStatus(OrderStatus.EXPIRED);
            order.setReservationExpiresAt(null);
            order.setCancelledAt(LocalDateTime.now());
            orderRepository.save(order);
        }
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            double lineTotal = item.getUnitPrice() * item.getQuantity();
            itemResponses.add(OrderItemResponse.builder()
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build());
        }

        return OrderResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentReference(order.getPaymentReference())
                .createdAt(order.getCreatedAt())
                .cancellationReason(order.getCancellationReason())
                .refundedAt(order.getRefundedAt())
                .items(itemResponses)
                .build();
    }

    private void releaseInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProduct().getId()));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
