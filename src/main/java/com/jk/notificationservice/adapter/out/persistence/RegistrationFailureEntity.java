package com.jk.notificationservice.adapter.out.persistence;

import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_registration_failures")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class RegistrationFailureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(length = 64, nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private NotificationChannel channel;

    @Column(length = 64)
    private String referenceType;

    private Long referenceId;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}