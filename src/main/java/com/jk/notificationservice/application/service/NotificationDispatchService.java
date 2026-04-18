package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.in.DispatchNotificationUseCase;
import com.jk.notificationservice.application.port.out.NotificationSendPort;
import com.jk.notificationservice.application.port.out.RetryPolicyPort;
import com.jk.notificationservice.common.exception.NotificationException;
import com.jk.notificationservice.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RetryPolicyPort retryPolicyPort;

    @Override
    public void dispatch() {
        List<NotificationRequest> claimed = notificationFacade
                .claimPendingForDispatch(LocalDateTime.now(), DISPATCH_BATCH_SIZE);
        claimed.forEach(this::dispatchSingle);
    }

    private void dispatchSingle(NotificationRequest processing) {
        try {
            notificationSendPort.send(processing);
            processing.markAsSent();
            log.info("알림 발송 완료. id={}", processing.getId());
        } catch (NotificationException e) {
            log.warn("알림 발송 실패. id={}, reason={}", processing.getId(), e.getMessage());
            processing.handleFailure(e.getMessage(), retryPolicyPort.calculateNextRetryAt(processing.getRetryCount()));
        }

        notificationFacade.save(processing);
    }
}
