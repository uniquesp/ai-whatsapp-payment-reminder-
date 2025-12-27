package com.example.yoga_reminder.repository;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findBySubscriptionId(Long subscriptionId);

    List<Invoice> findByPaymentStatusAndNextReminderDate(
            PaymentStatus status,
            LocalDate reminderDate
    );
}

