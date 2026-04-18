package com.jk.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "notification.scheduler")
public record NotificationSchedulerProperties(
        Duration dispatchDelay,
        Duration stuckThreshold,
        Duration stuckRecoveryDelay
) {
}