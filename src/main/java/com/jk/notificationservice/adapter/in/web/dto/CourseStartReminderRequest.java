package com.jk.notificationservice.adapter.in.web.dto;

import com.jk.notificationservice.domain.NotificationChannel;
import jakarta.validation.constraints.NotNull;

public record CourseStartReminderRequest(
        @NotNull Long recipientId,
        @NotNull Long courseId,
        @NotNull NotificationChannel channel
) {}