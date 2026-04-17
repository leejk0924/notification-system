package com.jk.notificationservice.adapter.in.web.dto;

import com.jk.notificationservice.domain.NotificationChannel;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmedRequest(
        @NotNull Long recipientId,
        @NotNull Long paymentId,
        @NotNull NotificationChannel channel
) {}