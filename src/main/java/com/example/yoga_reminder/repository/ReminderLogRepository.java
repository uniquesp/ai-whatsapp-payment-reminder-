package com.example.yoga_reminder.repository;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.entity.ReminderLog;
import com.example.yoga_reminder.domain.enums.ReminderChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    /**
     * Fetch all reminders sent for a specific invoice
     */
    List<ReminderLog> findByInvoice(Invoice invoice);

    /**
     * Fetch reminders by channel (useful if you add SMS / Email later)
     */
    List<ReminderLog> findByChannel(ReminderChannel channel);

    /**
     * Idempotency guard: check if a reminder was already sent for this invoice today.
     */
    boolean existsByInvoiceAndSentAtBetween(Invoice invoice, LocalDateTime start, LocalDateTime end);

    /**
     * Lightweight existence check to confirm at least one reminder was sent for this invoice.
     * Used to ensure replies are only processed after a reminder was delivered.
     */
    boolean existsByInvoice(Invoice invoice);
}
