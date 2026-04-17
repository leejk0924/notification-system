package com.jk.notificationservice.adapter.out.persistence.mapper;

import com.jk.notificationservice.adapter.out.persistence.NotificationRequestEntity;
import com.jk.notificationservice.domain.NotificationRequest;
import org.springframework.stereotype.Component;

@Component
public class NotificationRequestMapper {

    public NotificationRequest toDomain(NotificationRequestEntity entity) {
        if (entity == null) {
            return null;
        }

        return NotificationRequest.reconstruct(
                entity.getId(),
                entity.getRecipientId(),
                entity.getNotificationType(),
                entity.getChannel(),
                entity.getStatus(),
                entity.getIdempotencyKey(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getPayload(),
                entity.getScheduledAt(),
                entity.getExpireAt(),
                entity.getRetryCount(),
                entity.getMaxRetryCount(),
                entity.getNextRetryAt(),
                entity.getFailureReason(),
                entity.isRead(),
                entity.getReadAt(),
                entity.getSentAt(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public NotificationRequestEntity toEntity(NotificationRequest domain) {
        if (domain == null) {
            return null;
        }

        return NotificationRequestEntity.builder()
                .id(domain.getId())
                .recipientId(domain.getRecipientId())
                .notificationType(domain.getNotificationType())
                .channel(domain.getChannel())
                .status(domain.getStatus())
                .idempotencyKey(domain.getIdempotencyKey())
                .referenceType(domain.getReferenceType())
                .referenceId(domain.getReferenceId())
                .payload(domain.getPayload())
                .scheduledAt(domain.getScheduledAt())
                .expireAt(domain.getExpireAt())
                .retryCount(domain.getRetryCount())
                .maxRetryCount(domain.getMaxRetryCount())
                .nextRetryAt(domain.getNextRetryAt())
                .failureReason(domain.getFailureReason())
                .isRead(domain.isRead())
                .readAt(domain.getReadAt())
                .sentAt(domain.getSentAt())
                .version(domain.getVersion())
                .build();
    }
}
