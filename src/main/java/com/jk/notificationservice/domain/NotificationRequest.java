package com.jk.notificationservice.domain;

import com.jk.notificationservice.common.NotificationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationRequest {

    private Long id;
    private Long recipientId;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private NotificationStatus status;
    private String idempotencyKey;
    private String referenceType;
    private Long referenceId;
    private String payload;
    private LocalDateTime scheduledAt;
    private LocalDateTime expireAt;
    private int retryCount;
    private int maxRetryCount;
    private LocalDateTime nextRetryAt;
    private String failureReason;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 새 알림 요청 생성
    public static NotificationRequest create(
            Long recipientId,
            NotificationType notificationType,
            NotificationChannel channel,
            String idempotencyKey,
            int maxRetryCount,
            String referenceType,
            Long referenceId,
            String payload,
            LocalDateTime scheduledAt
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
        request.expireAt = calculateExpireAt(notificationType);
        request.status = NotificationStatus.PENDING;
        request.retryCount = 0;
        request.read = false;
        return request;
    }

    private static LocalDateTime calculateExpireAt(NotificationType type) {
        return switch (type) {
            case PAYMENT_CONFIRMED     -> LocalDateTime.now().plusHours(12);
            case COURSE_START_REMINDER -> LocalDateTime.now().plusHours(2);
            default                    -> LocalDateTime.now().plusHours(24);
        };
    }


    // 영속성 레이어에서 도메인 객체 재구성 — Mapper 전용
    public static NotificationRequest reconstruct(
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
            LocalDateTime nextRetryAt,
            String failureReason,
            boolean isRead,
            LocalDateTime readAt,
            LocalDateTime sentAt,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new NotificationRequest(
                id, recipientId, notificationType, channel, status,
                idempotencyKey, referenceType, referenceId, payload,
                scheduledAt, expireAt, retryCount, maxRetryCount,
                nextRetryAt, failureReason, isRead, readAt, sentAt,
                version, createdAt, updatedAt
        );
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
    public void markAsPermanentlyFailed(String reason) {
        validateStatus(NotificationStatus.PROCESSING);
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
    }

    // 상태 전이: 유효 기한 초과
    public void markAsExpired() {
        if (this.status != NotificationStatus.PENDING && this.status != NotificationStatus.PROCESSING) {
            throw new NotificationException(
                    "PENDING 또는 PROCESSING 상태에서만 EXPIRED로 전이할 수 있습니다. 현재 상태: " + this.status
            );
        }
        this.status = NotificationStatus.EXPIRED;
    }

    // 읽음 처리 — IN_APP 전용
    public void markAsRead() {
        if (this.channel != NotificationChannel.IN_APP) {
            return;
        }
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    // 일시적 실패 처리 — 재시도 가능 여부에 따라 PENDING 또는 DEAD_LETTER로 자동 분기
    public void handleFailure(String reason, LocalDateTime nextRetryAt) {
        validateStatus(NotificationStatus.PROCESSING);
        if (isRetryable()) {
            scheduleRetry(nextRetryAt);
        } else {
            this.status = NotificationStatus.DEAD_LETTER;
            this.failureReason = reason;
        }
    }

    // 재시도 예약 (지수 백오프 + Jitter 적용된 nextRetryAt 전달)
    private void scheduleRetry(LocalDateTime nextRetryAt) {
        this.retryCount++;
        this.status = NotificationStatus.PENDING;
        this.nextRetryAt = nextRetryAt;
    }

    private void validateStatus(NotificationStatus expected) {
        if (this.status != expected) {
            throw new NotificationException(
                    expected + " 상태에서만 가능한 전이입니다. 현재 상태: " + this.status
            );
        }
    }
}