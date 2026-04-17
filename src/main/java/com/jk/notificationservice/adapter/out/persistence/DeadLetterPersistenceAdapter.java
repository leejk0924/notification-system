package com.jk.notificationservice.adapter.out.persistence;

import com.jk.notificationservice.application.port.out.DeadLetterPort;
import com.jk.notificationservice.domain.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DeadLetterPersistenceAdapter implements DeadLetterPort {

    private final RegistrationFailureJpaRepository jpaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(NotificationEvent event, String failureReason) {
        jpaRepository.save(RegistrationFailureEntity.builder()
                .recipientId(event.recipientId())
                .notificationType(event.notificationType())
                .channel(event.channel())
                .referenceType(event.referenceType())
                .referenceId(event.referenceId())
                .failureReason(failureReason)
                .build());
    }
}