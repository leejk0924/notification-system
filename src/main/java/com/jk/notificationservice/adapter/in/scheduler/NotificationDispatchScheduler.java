package com.jk.notificationservice.adapter.in.scheduler;

import com.jk.notificationservice.application.port.in.DispatchNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatchScheduler {

    private final DispatchNotificationUseCase dispatchNotificationUseCase;

    @Scheduled(fixedDelayString = "${notification.scheduler.dispatch-delay}")
    public void scheduledDispatch() {
        log.debug("알림 발송 스케줄러 실행");
        dispatchNotificationUseCase.dispatch();
    }
}
