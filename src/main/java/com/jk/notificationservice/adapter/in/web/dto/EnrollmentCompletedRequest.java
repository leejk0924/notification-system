package com.jk.notificationservice.adapter.in.web.dto;

import com.jk.notificationservice.domain.NotificationChannel;
import jakarta.validation.constraints.NotNull;

public record EnrollmentCompletedRequest(
        @NotNull Long recipientId,
        @NotNull Long enrollmentId,
        @NotNull NotificationChannel channel
) {}