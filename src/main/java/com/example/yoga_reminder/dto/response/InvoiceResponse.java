package com.example.yoga_reminder.dto.response;

import com.example.yoga_reminder.domain.enums.PaymentIntent;
import com.example.yoga_reminder.domain.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class InvoiceResponse {

    private Long invoiceId;
    private PaymentStatus paymentStatus;
    private PaymentIntent paymentIntent;
    private LocalDate nextReminderDate;
    private Integer reminderCount;

    private Long subscriptionId;
    private Long userId;
    private String userName;
    private String userPhone;
}

