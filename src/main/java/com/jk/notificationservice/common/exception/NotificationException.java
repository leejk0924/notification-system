package com.jk.notificationservice.common.exception;

import lombok.Getter;

@Getter
public class NotificationException extends RuntimeException {

    private final ErrorCode errorCode;

    public NotificationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public NotificationException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public NotificationException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
    }
}
