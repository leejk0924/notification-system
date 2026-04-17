package com.jk.notificationservice.adapter.in.web;

import com.jk.notificationservice.adapter.in.web.dto.CourseStartReminderRequest;
import com.jk.notificationservice.adapter.in.web.dto.EnrollmentCompletedRequest;
import com.jk.notificationservice.adapter.in.web.dto.PaymentConfirmedRequest;
import com.jk.notificationservice.domain.NotificationType;
import com.jk.notificationservice.domain.event.NotificationEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dummy")
@RequiredArgsConstructor
public class DummyEventController {

    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/enrollment-completed")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void enrollmentCompleted(@RequestBody @Valid EnrollmentCompletedRequest request) {
        eventPublisher.publishEvent(new NotificationEvent(
                request.recipientId(),
                NotificationType.ENROLLMENT_COMPLETED,
                request.channel(),
                "ENROLLMENT",
                request.enrollmentId()
        ));
    }

    @PostMapping("/payment-confirmed")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void paymentConfirmed(@RequestBody @Valid PaymentConfirmedRequest request) {
        eventPublisher.publishEvent(new NotificationEvent(
                request.recipientId(),
                NotificationType.PAYMENT_CONFIRMED,
                request.channel(),
                "PAYMENT",
                request.paymentId()
        ));
    }

    @PostMapping("/course-start-reminder")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void courseStartReminder(@RequestBody @Valid CourseStartReminderRequest request) {
        eventPublisher.publishEvent(new NotificationEvent(
                request.recipientId(),
                NotificationType.COURSE_START_REMINDER,
                request.channel(),
                "COURSE",
                request.courseId()
        ));
    }
}