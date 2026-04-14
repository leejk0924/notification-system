package com.jk.notificationservice.domain;

import lombok.RequiredArgsConstructor;

/**
 * 알림 발송 채널
 * 각 채널은 읽음 추적(read tracking) 지원 여부를 가진다.
 */
@RequiredArgsConstructor
public enum NotificationChannel {
    EMAIL(false),
    IN_APP(true);

    private final boolean supportsReadTracking;

    public boolean supportsReadTracking() {
        return this.supportsReadTracking;
    }
}