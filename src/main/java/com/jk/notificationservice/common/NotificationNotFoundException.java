package com.jk.notificationservice.common;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(Long id) {
        super("알림 요청을 찾을 수 없습니다. id: " + id);
    }
}