package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.in.QueryNotificationUseCase;
import com.jk.notificationservice.application.port.out.NotificationRepository;
import com.jk.notificationservice.common.exception.NotificationErrorCode;
import com.jk.notificationservice.common.exception.NotificationException;
import com.jk.notificationservice.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationQueryService implements QueryNotificationUseCase {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    @Override
    public NotificationRequest findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOT_FOUND, "알림 요청을 찾을 수 없습니다. id: " + id));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<NotificationRequest> findByRecipientId(Long recipientId, Boolean read, Pageable pageable) {
        return notificationRepository.findByRecipientId(recipientId, read, pageable);
    }
}