package com.jk.notificationservice.application.port.in;

import com.jk.notificationservice.domain.NotificationRequest;

public interface MarkNotificationAsReadUseCase {

    /**
     * 알림을 읽음 처리한다. 이미 읽음 상태이면 멱등하게 현재 상태를 반환한다.
     *
     * @param id          읽음 처리할 알림 ID
     * @param recipientId 요청자 ID (소유권 확인용)
     * @return 읽음 처리된 알림 요청
     */
    NotificationRequest markAsRead(Long id, Long recipientId);
}
