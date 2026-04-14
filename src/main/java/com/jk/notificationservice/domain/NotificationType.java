package com.jk.notificationservice.domain;

import lombok.RequiredArgsConstructor;

/**
 * 알림 유형.
 * 각 유형은 사용자에게 표시될 설명을 가진다.
 */
@RequiredArgsConstructor
public enum NotificationType {

    /** 수강 신청이 완료된 경우 */
    ENROLLMENT_COMPLETED("수강 신청 완료"),

    /** 결제가 확정된 경우 */
    PAYMENT_CONFIRMED("결제 확정"),

    /** 강의 시작 하루 전 리마인더 */
    COURSE_START_REMINDER("강의 시작 D-1"),

    /** 수강 신청이 취소된 경우 */
    ENROLLMENT_CANCELLED("취소 처리");

    private final String description;
}