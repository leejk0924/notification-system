package com.jk.notificationservice.adapter.in.scheduler;

import com.jk.notificationservice.application.port.in.RecoverStuckNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessingStuckRecoveryScheduler {

    private final RecoverStuckNotificationUseCase recoverStuckNotificationUseCase;

    @Scheduled(fixedDelayString = "${notification.scheduler.stuck-recovery-delay}")
    public void scheduledRecovery() {
        log.debug("stuck 복구 스케줄러 실행");
        recoverStuckNotificationUseCase.recover();
    }
}
