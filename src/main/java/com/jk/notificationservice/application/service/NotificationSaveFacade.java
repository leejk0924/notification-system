package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.out.NotificationRepository;
import com.jk.notificationservice.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationSaveFacade {
    private static final int DEFAULT_MAX_RETRY_COUNT = 3;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void save(NotificationRequest request) {
        notificationRepository.save(request);
    }
}
