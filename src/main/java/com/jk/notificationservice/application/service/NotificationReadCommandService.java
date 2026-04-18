package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.in.MarkNotificationAsReadUseCase;
import com.jk.notificationservice.common.exception.NotificationErrorCode;
import com.jk.notificationservice.common.exception.NotificationException;
import com.jk.notificationservice.domain.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationReadCommandService implements MarkNotificationAsReadUseCase {

    private final NotificationFacade notificationFacade;

    @Override
    public NotificationRequest markAsRead(Long id, Long recipientId) {
        NotificationRequest request = notificationFacade.findById(id);

        if (!request.getRecipientId().equals(recipientId)) {
            throw new NotificationException(NotificationErrorCode.ACCESS_DENIED, "해당 알림에 대한 접근 권한이 없습니다. id: " + id);
        }

        if (request.isRead()) {
            return request;
        }

        request.markAsRead();

        try {
            return notificationFacade.save(request);
        } catch (ObjectOptimisticLockingFailureException e) {
            return notificationFacade.findById(id);
        }
    }
}
