package com.jk.notificationservice.adapter.out.persistence;

import com.jk.notificationservice.common.BaseAudit;
import com.jk.notificationservice.domain.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_requests")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationRequestEntity extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(length = 64, nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private NotificationStatus status;

    @Column(length = 512, nullable = false)
    private String idempotencyKey;

    @Column(length = 64)
    private String referenceType;

    private Long referenceId;

    @Column(columnDefinition = "JSON")
    private String payload;

    private LocalDateTime scheduledAt;
    private LocalDateTime expireAt;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private int maxRetryCount;

    private LocalDateTime nextRetryAt;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Column(nullable = false)
    private boolean isRead;

    private LocalDateTime readAt;

    private LocalDateTime sentAt;

    @Version
    private Long version;
}