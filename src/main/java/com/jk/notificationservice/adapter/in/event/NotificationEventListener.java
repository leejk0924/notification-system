package com.jk.notificationservice.adapter.in.event;

import com.jk.notificationservice.application.port.in.RegisterNotificationUseCase;
import com.jk.notificationservice.domain.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final RegisterNotificationUseCase registerNotificationUseCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(NotificationEvent event) {
        registerNotificationUseCase.register(event);
    }
}