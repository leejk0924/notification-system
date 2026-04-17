package com.jk.notificationservice.application.port.out;

import com.jk.notificationservice.domain.NotificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NotificationRepository {

    NotificationRequest save(NotificationRequest request);

    Optional<NotificationRequest> findById(Long id);

    Optional<NotificationRequest> findByIdempotencyKey(String idempotencyKey);

    Page<NotificationRequest> findByRecipientId(Long recipientId, Boolean read, Pageable pageable);
}