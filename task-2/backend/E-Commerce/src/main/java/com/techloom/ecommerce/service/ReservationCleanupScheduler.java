package com.techloom.ecommerce.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Releases the stock held by checkout sessions that were abandoned. */
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 30_000)
    public void expireAbandonedReservations() {
        orderService.expireReservations();
    }
}
