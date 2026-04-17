package com.jk.notificationservice.application.service;

import com.jk.notificationservice.application.port.out.NotificationRepository;
import com.jk.notificationservice.common.NotificationNotFoundException;
import com.jk.notificationservice.domain.NotificationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @InjectMocks
    private NotificationQueryService sut;

    @Mock
    private NotificationRepository notificationRepository;

    @DisplayName("findById 메서드")
    @Nested
    class FindById {

        @Test
        @DisplayName("존재하는 id로 조회 시 해당 NotificationRequest를 반환한다")
        void findById_존재하는_id_반환() {
            // given
            NotificationRequest request = mock(NotificationRequest.class);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(request));

            // when
            NotificationRequest result = sut.findById(1L);

            // then
            assertThat(result).isSameAs(request);
        }

        @Test
        @DisplayName("존재하지 않는 id로 조회 시 NotificationNotFoundException이 발생한다")
        void findById_존재하지않는_id_예외() {
            // given
            given(notificationRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.findById(99L))
                    .isInstanceOf(NotificationNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("조회 시 repository에 id를 그대로 전달한다")
        void findById_repository_호출_검증() {
            // given
            NotificationRequest request = mock(NotificationRequest.class);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(request));

            // when
            sut.findById(1L);

            // then
            then(notificationRepository).should().findById(1L);
        }
    }

    @DisplayName("findByRecipientId 메서드")
    @Nested
    class FindByRecipientId {

        @Test
        @DisplayName("수신자 id로 조회 시 페이지 결과를 반환한다")
        void findByRecipientId_페이지_반환() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<NotificationRequest> page = new PageImpl<>(List.of(mock(NotificationRequest.class)));
            given(notificationRepository.findByRecipientId(1L, null, pageable)).willReturn(page);

            // when
            Page<NotificationRequest> result = sut.findByRecipientId(1L, null, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("read=true 필터로 조회 시 repository에 read=true를 전달한다")
        void findByRecipientId_읽음필터_전달() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(notificationRepository.findByRecipientId(1L, true, pageable))
                    .willReturn(Page.empty());

            // when
            sut.findByRecipientId(1L, true, pageable);

            // then
            then(notificationRepository).should().findByRecipientId(1L, true, pageable);
        }

        @Test
        @DisplayName("read=null이면 읽음 여부 필터 없이 조회한다")
        void findByRecipientId_필터없음_전달() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(notificationRepository.findByRecipientId(1L, null, pageable))
                    .willReturn(Page.empty());

            // when
            sut.findByRecipientId(1L, null, pageable);

            // then
            then(notificationRepository).should().findByRecipientId(1L, null, pageable);
        }

        @Test
        @DisplayName("결과가 없으면 빈 페이지를 반환한다")
        void findByRecipientId_결과없음_빈페이지() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(notificationRepository.findByRecipientId(1L, null, pageable))
                    .willReturn(Page.empty());

            // when
            Page<NotificationRequest> result = sut.findByRecipientId(1L, null, pageable);

            // then
            assertThat(result.isEmpty()).isTrue();
        }
    }
}