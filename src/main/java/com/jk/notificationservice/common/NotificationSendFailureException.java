package com.jk.notificationservice.common;

public class NotificationSendFailureException extends RuntimeException {
    public NotificationSendFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}