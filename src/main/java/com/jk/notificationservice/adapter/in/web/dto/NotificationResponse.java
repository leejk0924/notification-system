package com.jk.notificationservice.adapter.in.web.dto;

import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationRequest;
import com.jk.notificationservice.domain.NotificationStatus;
import com.jk.notificationservice.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long recipientId,
        NotificationType notificationType,
        NotificationChannel channel,
        NotificationStatus status,
        String idempotencyKey,
        String referenceType,
        Long referenceId,
        String payload,
        LocalDateTime scheduledAt,
        LocalDateTime expireAt,
        int retryCount,
        int maxRetryCount,
        String failureReason,
        boolean read,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(NotificationRequest domain) {
        return new NotificationResponse(
                domain.getId(),
                domain.getRecipientId(),
                domain.getNotificationType(),
                domain.getChannel(),
                domain.getStatus(),
                domain.getIdempotencyKey(),
                domain.getReferenceType(),
                domain.getReferenceId(),
                domain.getPayload(),
                domain.getScheduledAt(),
                domain.getExpireAt(),
                domain.getRetryCount(),
                domain.getMaxRetryCount(),
                domain.getFailureReason(),
                domain.isRead(),
                domain.getSentAt(),
                domain.getCreatedAt()
        );
    }
}