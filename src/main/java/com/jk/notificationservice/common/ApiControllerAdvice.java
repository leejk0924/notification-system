package com.jk.notificationservice.common;

import com.jk.notificationservice.common.exception.ErrorResponse;
import com.jk.notificationservice.common.exception.NotificationException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiControllerAdvice {

    @ExceptionHandler(NotificationException.class)
    public ErrorResponse handleNotification(NotificationException e, HttpServletResponse response) {
        response.setStatus(e.getErrorCode().statusCode().value());
        return ErrorResponse.of(e.getErrorCode(), e.getMessage());
    }
}
