package com.techloom.pos.service;

import com.techloom.pos.dto.*;
import com.techloom.pos.entity.*;
import com.techloom.pos.exception.BusinessException;
import com.techloom.pos.exception.ConflictException;
import com.techloom.pos.exception.ResourceNotFoundException;
import com.techloom.pos.repository.OrderRepository;
import com.techloom.pos.repository.PaymentRepository;
import com.techloom.pos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int RESERVATION_MINUTES = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }

        if (request.getCartId() != null && !request.getCartId().isBlank()) {
            // Checkout retries return the original reservation; they never take stock twice.
            var existingOrder = orderRepository.findByCartId(request.getCartId());
            if (existingOrder.isPresent()) {
                return mapToResponse(existingOrder.get());
            }
        }

        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0.0;

        for (CheckoutItemRequest itemRequest : request.getItems()) {
            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new BusinessException("Quantity must be greater than zero for product " + itemRequest.getProductId());
            }

            Product product = productRepository.findByIdForUpdate(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + product.getName());
            }

            product.setStock(product.getStock() - itemRequest.getQuantity());

            double lineTotal = product.getPrice() * itemRequest.getQuantity();
            total += lineTotal;

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build());
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .cartId(request.getCartId() == null || request.getCartId().isBlank() ? null : request.getCartId())
                .status(OrderStatus.RESERVED)
                .total(total)
                .totalAmount(total)
                .reservedAt(now)
                .reservationExpiresAt(now.plusMinutes(RESERVATION_MINUTES))
                .items(orderItems)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException("Paid order cannot be cancelled");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return mapToResponse(order);
        }

        releaseReservationStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setReservationExpiresAt(null);
        return mapToResponse(orderRepository.save(order));
    }

    @Transactional
    public PaymentResponse processPayment(Long id, PaymentRequest request) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new ConflictException("Order is already paid");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED || order.getStatus() == OrderStatus.FAILED) {
            throw new BusinessException("Cannot process payment for order in status: " + order.getStatus());
        }
        if (request.getAmount() == null || request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new BusinessException("Payment amount must match order total of " + order.getTotalAmount());
        }
        if (paymentRepository.existsByOrder_IdAndPaymentReference(id, request.getPaymentReference())) {
            throw new ConflictException("Duplicate payment reference for order: " + request.getPaymentReference());
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .paymentReference(request.getPaymentReference())
                .amount(request.getAmount())
                .status(request.getStatus())
                .build();

        if (request.getStatus() == PaymentStatus.SUCCESS) {
            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            order.setReservationExpiresAt(null);
        } else {
            releaseReservationStock(order);
            order.setStatus(request.getStatus() == PaymentStatus.TIMEOUT ? OrderStatus.EXPIRED : OrderStatus.FAILED);
            order.setCancelledAt(LocalDateTime.now());
            order.setReservationExpiresAt(null);
        }

        order.getPayments().add(payment);
        payment = paymentRepository.save(payment);
        return mapToPaymentResponse(payment);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    @Transactional
    public void expireReservations() {
        List<Order> expiredOrders = orderRepository.findExpiredReservations(OrderStatus.RESERVED, LocalDateTime.now());
        for (Order order : expiredOrders) {
            releaseReservationStock(order);
            order.setStatus(OrderStatus.EXPIRED);
            order.setCancelledAt(LocalDateTime.now());
            order.setReservationExpiresAt(null);
            orderRepository.save(order);
        }
    }

    private void releaseReservationStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProduct().getId()));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
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
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
