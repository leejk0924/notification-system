package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.out.DeadLetterPort;
import com.jk.notificationservice.application.port.out.NotificationRepository;
import com.jk.notificationservice.domain.NotificationChannel;
import com.jk.notificationservice.domain.NotificationRequest;
import com.jk.notificationservice.domain.NotificationStatus;
import com.jk.notificationservice.domain.NotificationType;
import com.jk.notificationservice.domain.event.NotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static com.jk.notificationservice.domain.NotificationChannel.EMAIL;
import static com.jk.notificationservice.domain.NotificationChannel.IN_APP;
import static com.jk.notificationservice.domain.NotificationType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @InjectMocks
    private NotificationCommandService sut;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeadLetterPort deadLetterPort;

    @DisplayName("register 메서드")
    @Nested
    class Register {

        @Test
        @DisplayName("신규 이벤트는 PENDING 상태로 저장된다")
        void register_신규이벤트_저장() {
            // given
            NotificationEvent event = createEvent(ENROLLMENT_COMPLETED, EMAIL);
            given(notificationRepository.findByIdempotencyKey(any())).willReturn(Optional.empty());
            given(notificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            sut.register(event);

            // then
            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            then(notificationRepository).should().save(captor.capture());
            NotificationRequest request = captor.getValue();

            assertThat(request.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(request.getRecipientId()).isEqualTo(event.recipientId());
            assertThat(request.getRetryCount()).isZero();
        }

        @Test
        @DisplayName("중복 멱등성 키가 존재하면 저장하지 않는다")
        void register_중복요청_무시() {
            // given
            NotificationEvent event = createEvent(ENROLLMENT_COMPLETED, EMAIL);
            given(notificationRepository.findByIdempotencyKey(any())).willReturn(Optional.of(mock(NotificationRequest.class)));

            // when
            sut.register(event);

            // then
            then(notificationRepository).should(never()).save(any());
            then(deadLetterPort).should(never()).save(any(), any());
        }

        @Test
        @DisplayName("멱등성 키는 {type}:{refType}:{refId}:{recipientId}:{channel} 형식으로 생성된다")
        void register_멱등성키_형식_검증() {
            // given
            NotificationEvent event = createEvent(ENROLLMENT_COMPLETED, EMAIL);
            given(notificationRepository.findByIdempotencyKey(any())).willReturn(Optional.empty());
            given(notificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            sut.register(event);

            // then
            String expectedKey = "ENROLLMENT_COMPLETED:ENROLLMENT:100:1:EMAIL";
            then(notificationRepository).should().findByIdempotencyKey(expectedKey);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            then(notificationRepository).should().save(captor.capture());
            assertThat(captor.getValue().getIdempotencyKey()).isEqualTo(expectedKey);
        }

        @Test
        @DisplayName("저장 중 예외 발생 시 DeadLetterPort에 저장한다")
        void register_저장실패_DeadLetter저장() {
            // given
            NotificationEvent event = createEvent(ENROLLMENT_COMPLETED, EMAIL);

            given(notificationRepository.findByIdempotencyKey(any())).willReturn(Optional.empty());
            given(notificationRepository.save(any())).willThrow(new RuntimeException("DB 오류"));

            // when
            sut.register(event);

            // then
            ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
            ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
            then(deadLetterPort).should().save(eventCaptor.capture(), reasonCaptor.capture());

            NotificationEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.recipientId()).isEqualTo(event.recipientId());
            assertThat(savedEvent.notificationType()).isEqualTo(event.notificationType());
            assertThat(savedEvent.channel()).isEqualTo(event.channel());
            assertThat(savedEvent.referenceType()).isEqualTo(event.referenceType());
            assertThat(savedEvent.referenceId()).isEqualTo(event.referenceId());
            assertThat(reasonCaptor.getValue()).isEqualTo("DB 오류");
        }

        @Test
        @DisplayName("저장 중 예외 발생 시 save를 재시도하지 않는다")
        void register_저장실패_재시도없음() {
            // given
            NotificationEvent event = createEvent(ENROLLMENT_COMPLETED, EMAIL);
            given(notificationRepository.findByIdempotencyKey(any())).willReturn(Optional.empty());
            given(notificationRepository.save(any())).willThrow(new RuntimeException("DB 오류"));

            // when
            sut.register(event);

            // then
            then(notificationRepository).should(times(1)).save(any());
        }
    }

    @DisplayName("expireAt TTL 계산")
    @Nested
    class ExpireAt {

        @DisplayName("타입별 TTL 계산")
        @ParameterizedTest(name = "{0} → {1}시간 후 만료")
        @MethodSource("ttlArguments")
        void expireAt_타입별_TTL(NotificationType type, long expectedHours) {
            // given
            NotificationEvent event = createEvent(type, EMAIL);
            given(notificationRepository.findByIdempotencyKey(any())).willReturn(Optional.empty());
            given(notificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            LocalDateTime before = LocalDateTime.now()
                    .plusHours(expectedHours)
                    .minusSeconds(5);

            sut.register(event);

            LocalDateTime after = LocalDateTime.now()
                    .plusHours(expectedHours)
                    .plusSeconds(5);

            // then
            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            then(notificationRepository).should().save(captor.capture());
            assertThat(captor.getValue().getExpireAt()).isBetween(before, after);
        }

        static Stream<Arguments> ttlArguments() {
            return Stream.of(
                    Arguments.of(ENROLLMENT_COMPLETED, 24L),
                    Arguments.of(ENROLLMENT_CANCELLED, 24L),
                    Arguments.of(PAYMENT_CONFIRMED, 12L),
                    Arguments.of(COURSE_START_REMINDER, 2L)
            );
        }
    }

    @DisplayName("채널별 멱등성 키 구분")
    @Nested
    class IdempotencyKey {

        @Test
        @DisplayName("동일 이벤트라도 채널이 다르면 다른 멱등성 키를 사용한다")
        void idempotencyKey_채널별_구분() {
            // given
            NotificationEvent emailEvent = createEvent(ENROLLMENT_COMPLETED, EMAIL);
            NotificationEvent inAppEvent = createEvent(ENROLLMENT_COMPLETED, IN_APP);
            given(notificationRepository.findByIdempotencyKey(any())).willReturn(Optional.empty());
            given(notificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            sut.register(emailEvent);
            sut.register(inAppEvent);

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(notificationRepository)
                    .should(times(2))
                    .findByIdempotencyKey(keyCaptor.capture());
            assertThat(keyCaptor.getAllValues().get(0))
                    .isNotEqualTo(keyCaptor.getAllValues().get(1));
        }

        @Test
        @DisplayName("동일 유저라도 강의(referenceId)가 다르면 다른 멱등성 키를 사용한다")
        void idempotencyKey_강의별_구분() {
            // given
            NotificationEvent course100Event = new NotificationEvent(1L, ENROLLMENT_COMPLETED, EMAIL, "ENROLLMENT", 100L);
            NotificationEvent course200Event = new NotificationEvent(1L, ENROLLMENT_COMPLETED, EMAIL, "ENROLLMENT", 200L);
            given(notificationRepository.findByIdempotencyKey(any()))
                    .willReturn(Optional.empty());
            given(notificationRepository.save(any()))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            sut.register(course100Event);
            sut.register(course200Event);

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(notificationRepository)
                    .should(times(2))
                    .findByIdempotencyKey(keyCaptor.capture());
            assertThat(keyCaptor.getAllValues().get(0))
                    .isNotEqualTo(keyCaptor.getAllValues().get(1));
        }
    }

    private NotificationEvent createEvent(NotificationType type, NotificationChannel channel) {
        return new NotificationEvent(1L, type, channel, "ENROLLMENT", 100L);
    }
}