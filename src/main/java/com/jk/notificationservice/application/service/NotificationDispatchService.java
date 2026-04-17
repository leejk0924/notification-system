package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.in.DispatchNotificationUseCase;
import com.jk.notificationservice.application.port.out.NotificationSendPort;
import com.jk.notificationservice.common.NotificationSendFailureException;
import com.jk.notificationservice.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService implements DispatchNotificationUseCase {

    private static final int DISPATCH_BATCH_SIZE = 50;

    private final NotificationFacade notificationFacade;
    private final NotificationSendPort notificationSendPort;

    @Override
    public void dispatch() {
        List<NotificationRequest> targets = notificationFacade
                .findPendingForDispatch(LocalDateTime.now(), DISPATCH_BATCH_SIZE);
        targets.forEach(this::dispatchSingle);
    }

    private void dispatchSingle(NotificationRequest request) {
        if (request.isExpired(LocalDateTime.now())) {
            request.markAsExpired();
            notificationFacade.save(request);
            log.info("알림 만료 처리. id={}", request.getId());
            return;
        }

        NotificationRequest processing;
        try {
            request.markAsProcessing();
            processing = notificationFacade.save(request);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.debug("다른 워커가 선점. entity={}, id={}", e.getPersistentClassName(), request.getId());
            return;
        }

        try {
            notificationSendPort.send(processing);
            processing.markAsSent();
            log.info("알림 발송 완료. id={}", processing.getId());
        } catch (NotificationSendFailureException e) {
            log.warn("알림 발송 실패. id={}, reason={}", processing.getId(), e.getMessage());
            processing.handleFailure(e.getMessage(), calculateNextRetryAt(processing.getRetryCount()));
        }

        notificationFacade.save(processing);
    }

    private LocalDateTime calculateNextRetryAt(int retryCount) {
        long delaySeconds = Math.min((long) Math.pow(2, retryCount) * 60L, 1800L);
        long jitter = (long) (Math.random() * 10);
        return LocalDateTime.now().plusSeconds(delaySeconds + jitter);
    }
}
