package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.out.NotificationRepository;
import com.jk.notificationservice.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public Optional<NotificationRequest> findByIdempotencyKey(String idempotencyKey) {
        return notificationRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Transactional
    public List<NotificationRequest> claimPendingForDispatch(LocalDateTime now, int limit) {
        List<NotificationRequest> pending = notificationRepository.findPendingForDispatch(now, limit);

        List<NotificationRequest> claimed = new ArrayList<>();
        for (NotificationRequest request : pending) {
            if (request.isExpired(now)) {
                request.markAsExpired();
                notificationRepository.save(request);
            } else {
                request.markAsProcessing();
                claimed.add(notificationRepository.save(request));
            }
        }
        return claimed;
    }

    @Transactional(readOnly = true)
    public List<NotificationRequest> findStuckProcessing(LocalDateTime stuckBefore, int limit) {
        return notificationRepository.findStuckProcessing(stuckBefore, limit);
    }

    @Transactional
    public NotificationRequest save(NotificationRequest request) {
        return notificationRepository.save(request);
    }
}
