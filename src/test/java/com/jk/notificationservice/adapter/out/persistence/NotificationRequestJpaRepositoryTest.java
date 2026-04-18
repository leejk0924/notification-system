package com.jk.notificationservice.adapter.out.persistence;

import com.jk.notificationservice.TestcontainersConfiguration;
import com.jk.notificationservice.config.JpaAuditingConfig;
import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationStatus;
import com.jk.notificationservice.domain.NotificationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
class NotificationRequestJpaRepositoryTest {

    @Autowired
    private NotificationRequestJpaRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Nested
    @DisplayName("findStuckProcessing 메서드")
    class FindStuckProcessing {

        @Test
        @DisplayName("PROCESSING 상태이고 updatedAt이 stuckBefore보다 이전인 건을 반환한다")
        void findStuckProcessing_조건충족_반환() {
            NotificationRequestEntity stuck = saveAndFlush(processingEntity("key-1"));
            backdate(stuck.getId(), LocalDateTime.now().minusMinutes(10));

            List<NotificationRequestEntity> result = repository.findStuckProcessing(
                    LocalDateTime.now().minusMinutes(5), PageRequest.of(0, 50));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(stuck.getId());
        }

        @Test
        @DisplayName("PROCESSING이지만 최근에 업데이트된 건은 반환하지 않는다")
        void findStuckProcessing_최근업데이트_미반환() {
            saveAndFlush(processingEntity("key-1"));

            List<NotificationRequestEntity> result = repository.findStuckProcessing(
                    LocalDateTime.now().minusMinutes(5), PageRequest.of(0, 50));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("PROCESSING이 아닌 상태는 반환하지 않는다")
        void findStuckProcessing_비PROCESSING_미반환() {
            NotificationRequestEntity pending = saveAndFlush(entityWithStatus("key-1", NotificationStatus.PENDING));
            backdate(pending.getId(), LocalDateTime.now().minusMinutes(10));

            List<NotificationRequestEntity> result = repository.findStuckProcessing(
                    LocalDateTime.now().minusMinutes(5), PageRequest.of(0, 50));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("limit 개수만큼만 반환한다")
        void findStuckProcessing_limit_적용() {
            for (int i = 0; i < 5; i++) {
                NotificationRequestEntity e = saveAndFlush(processingEntity("key-" + i));
                backdate(e.getId(), LocalDateTime.now().minusMinutes(10));
            }

            List<NotificationRequestEntity> result = repository.findStuckProcessing(
                    LocalDateTime.now().minusMinutes(5), PageRequest.of(0, 3));

            assertThat(result).hasSize(3);
        }
    }

    private NotificationRequestEntity saveAndFlush(NotificationRequestEntity entity) {
        NotificationRequestEntity saved = repository.save(entity);
        entityManager.flush();
        return saved;
    }

    private void backdate(Long id, LocalDateTime pastTime) {
        entityManager.createNativeQuery(
                        "UPDATE notification_requests SET updated_at = :updatedAt WHERE id = :id")
                .setParameter("updatedAt", pastTime)
                .setParameter("id", id)
                .executeUpdate();
        entityManager.clear();
    }

    private NotificationRequestEntity processingEntity(String idempotencyKey) {
        return entityWithStatus(idempotencyKey, NotificationStatus.PROCESSING);
    }

    private NotificationRequestEntity entityWithStatus(String idempotencyKey, NotificationStatus status) {
        return NotificationRequestEntity.builder()
                .recipientId(1L)
                .notificationType(NotificationType.ENROLLMENT_COMPLETED)
                .channel(NotificationChannel.EMAIL)
                .status(status)
                .idempotencyKey(idempotencyKey)
                .retryCount(0)
                .maxRetryCount(3)
                .isRead(false)
                .build();
    }
}