package com.jk.notificationservice.domain;

import com.jk.notificationservice.common.BaseAudit;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationRequest extends BaseAudit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public static NotificationRequest create(
            Long recipientId,
            NotificationType notificationType,
            NotificationChannel channel,
            String idempotencyKey,
            int maxRetryCount,
            String referenceType,
            Long referenceId,
            String payload,
            LocalDateTime scheduledAt,
            LocalDateTime expireAt
    ) {
        NotificationRequest request = new NotificationRequest();
        request.recipientId = recipientId;
        request.notificationType = notificationType;
        request.channel = channel;
        request.idempotencyKey = idempotencyKey;
        request.maxRetryCount = maxRetryCount;
        request.referenceType = referenceType;
        request.referenceId = referenceId;
        request.payload = payload;
        request.scheduledAt = scheduledAt;
        request.expireAt = expireAt;
        request.status = NotificationStatus.PENDING;
        request.retryCount = 0;
        request.isRead = false;
        return request;
    }

    // 만료 여부 확인
    public boolean isExpired(LocalDateTime now) {
        return expireAt != null && now.isAfter(expireAt);
    }

    // 재시도 가능 여부 확인
    public boolean isRetryable() {
        return retryCount < maxRetryCount;
    }

    // 상태 전이: 처리 중
    public void markAsProcessing() {
        validateStatus(NotificationStatus.PENDING);
        this.status = NotificationStatus.PROCESSING;
    }

    // 상태 전이: 발송 완료
    public void markAsSent() {
        validateStatus(NotificationStatus.PROCESSING);
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    // 상태 전이: 영구 실패 (재시도 불가 — 잘못된 수신자, 페이로드 오류 등)
    public void markAsFailed(String reason) {
        validateStatus(NotificationStatus.PROCESSING);
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
    }

    // 상태 전이: 유효 기한 초과
    public void markAsExpired() {
        if (this.status != NotificationStatus.PENDING && this.status != NotificationStatus.PROCESSING) {
            throw new IllegalArgumentException(
                    "PENDING 또는 PROCESSING 상태에서만 EXPIRED로 전이할 수 있습니다. 현재 상태: " + this.status
            );
        }
        this.status = NotificationStatus.EXPIRED;
    }

    // 재시도 예약 (지수 백오프 + Jitter 적용된 nextRetryAt 전달)
    public void scheduleRetry(LocalDateTime nextRetryAt) {
        validateStatus(NotificationStatus.PROCESSING);
        this.retryCount++;
        this.status = NotificationStatus.PENDING;
        this.nextRetryAt = nextRetryAt;
    }

    // 읽음 처리 — IN_APP 전용
    public void markAsRead() {
        if (this.channel != NotificationChannel.IN_APP) {
            return;
        }
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    // 일시적 실패 처리 — 재시도 가능 여부에 따라 PENDING 또는 DEAD_LETTER로 자동 분기
    public void handleFailure(String reason, LocalDateTime nextRetryAt) {
        validateStatus(NotificationStatus.PROCESSING);
        if (isRetryable()) {
            this.retryCount++;
            this.status = NotificationStatus.PENDING;
            this.nextRetryAt = nextRetryAt;
        } else {
            this.status = NotificationStatus.DEAD_LETTER;
            this.failureReason = reason;
        }
    }

    private void validateStatus(NotificationStatus expected) {
        if (this.status != expected) {
            throw new IllegalArgumentException(
                    expected + " 상태에서만 가능한 전이입니다. 현재 상태: " + this.status
            );
        }
    }
}