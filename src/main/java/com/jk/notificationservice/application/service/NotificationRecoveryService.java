package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.in.RecoverStuckNotificationUseCase;
import com.jk.notificationservice.config.NotificationSchedulerProperties;
import com.jk.notificationservice.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRecoveryService implements RecoverStuckNotificationUseCase {

    private static final int RECOVERY_BATCH_SIZE = 50;

    private final NotificationFacade notificationFacade;
    private final NotificationSchedulerProperties properties;

    @Override
    public void recover() {
        LocalDateTime stuckBefore = LocalDateTime.now().minus(properties.stuckThreshold());
        List<NotificationRequest> stuckList = notificationFacade
                .findStuckProcessing(stuckBefore, RECOVERY_BATCH_SIZE);

        if (stuckList.isEmpty()) {
            return;
        }

        log.warn("stuck 알림 감지. count={}", stuckList.size());
        stuckList.forEach(request -> {
            request.recoverFromStuck();
            notificationFacade.save(request);
            log.info("stuck 알림 복구 완료. id={}", request.getId());
        });
    }
}
