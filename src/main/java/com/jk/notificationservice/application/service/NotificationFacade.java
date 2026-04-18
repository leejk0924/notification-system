package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.out.NotificationRepository;
import com.jk.notificationservice.common.exception.NotificationErrorCode;
import com.jk.notificationservice.common.exception.NotificationException;
import com.jk.notificationservice.domain.NotificationRequest;
import com.jk.notificationservice.domain.NotificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationFacade {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationRequest findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOT_FOUND, "알림 요청을 찾을 수 없습니다. id: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<NotificationRequest> findByIdempotencyKey(String idempotencyKey) {
        return notificationRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Transactional
    public List<NotificationRequest> claimPendingForDispatch(LocalDateTime now, int limit) {
        List<NotificationRequest> pending = notificationRepository.findPendingForDispatch(now, limit);
        return pending.stream()
                .map(request -> {
                    if (request.isExpired(now)) {
                        request.markAsExpired();
                    } else {
                        request.markAsProcessing();
                    }
                    return notificationRepository.save(request);
                })
                .filter(saved -> saved.getStatus() == NotificationStatus.PROCESSING)
                .toList();
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
