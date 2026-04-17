package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.in.RegisterNotificationUseCase;
import com.jk.notificationservice.application.port.out.DeadLetterPort;
import com.jk.notificationservice.domain.NotificationRequest;
import com.jk.notificationservice.domain.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService implements RegisterNotificationUseCase {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private final NotificationFacade notificationFacade;
    private final DeadLetterPort deadLetterPort;

    @Override
    public void register(NotificationEvent event) {
        String idempotencyKey = buildIdempotencyKey(event);

        if (notificationFacade.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.info("중복 알림 요청 무시. idempotencyKey={}", idempotencyKey);
            return;
        }

        try {
            notificationFacade.save(NotificationRequest.create(
                    event.recipientId(),
                    event.notificationType(),
                    event.channel(),
                    idempotencyKey,
                    DEFAULT_MAX_RETRY_COUNT,
                    event.referenceType(),
                    event.referenceId(),
                    null,
                    null                            // TODO: scheduledAt — referenceId로 조회 필요 (추후 구현)
            ));
            log.info("알림 등록 완료. idempotencyKey={}", idempotencyKey);
        } catch (Exception e) {
            log.error("알림 등록 실패 — DEAD_LETTER 저장 시도. idempotencyKey={}", idempotencyKey, e);
            deadLetterPort.save(event, e.getMessage());
        }
    }

    private String buildIdempotencyKey(NotificationEvent event) {
        return String.format("%s:%s:%d:%d:%s",
                event.notificationType(),
                event.referenceType(),
                event.referenceId(),
                event.recipientId(),
                event.channel()
        );
    }
}