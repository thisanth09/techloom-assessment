package com.techloom.pos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 60000)
    public void expireReservations() {
        orderService.expireReservations();
    }
}
