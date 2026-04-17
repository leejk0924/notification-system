package com.jk.notificationservice.adapter.in.web;

import com.jk.notificationservice.adapter.in.web.dto.NotificationResponse;
import com.jk.notificationservice.application.port.in.QueryNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final QueryNotificationUseCase queryNotificationUseCase;

    @GetMapping("/{id}")
    public NotificationResponse findById(@PathVariable Long id) {
        return NotificationResponse.from(queryNotificationUseCase.findById(id));
    }

    @GetMapping
    public Page<NotificationResponse> findByRecipientId(
            @RequestHeader("X-User-Id") Long recipientId,
            @RequestParam(required = false) Boolean read,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return queryNotificationUseCase.findByRecipientId(recipientId, read, pageable).map(NotificationResponse::from);
    }
}