package com.jk.notificationservice.adapter.out.persistence;

import com.jk.notificationservice.adapter.out.persistence.mapper.NotificationRequestMapper;
import com.jk.notificationservice.application.port.out.NotificationRepository;
import com.jk.notificationservice.domain.NotificationRequest;
import com.jk.notificationservice.domain.NotificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationRepository {

    private final NotificationRequestJpaRepository jpaRepository;
    private final NotificationRequestMapper mapper;

    @Override
    public NotificationRequest save(NotificationRequest request) {
        NotificationRequestEntity entity = mapper.toEntity(request);
        return mapper.toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<NotificationRequest> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<NotificationRequest> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Page<NotificationRequest> findByRecipientId(Long recipientId, Boolean read, Pageable pageable) {
        return jpaRepository.findByRecipientId(recipientId, read, pageable).map(mapper::toDomain);
    }

    @Override
    public List<NotificationRequest> findPendingForDispatch(LocalDateTime now, int limit) {
        return jpaRepository
                .findPendingForDispatch(NotificationStatus.PENDING, now, PageRequest.of(0, limit))
                .stream().map(mapper::toDomain).toList();
    }
}