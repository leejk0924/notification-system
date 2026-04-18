package com.jk.notificationservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "알림 요청을 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 알림에 대한 접근 권한이 없습니다."),
    INVALID_STATE(HttpStatus.CONFLICT, "현재 상태에서는 해당 작업을 수행할 수 없습니다."),
    SEND_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "알림 발송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus statusCode() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
