package com.jk.notificationservice.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String name();

    HttpStatus statusCode();

    String getMessage();
}
