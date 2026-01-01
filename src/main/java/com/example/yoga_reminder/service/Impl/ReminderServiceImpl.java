package com.example.yoga_reminder.service.Impl;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.entity.ReminderLog;
import com.example.yoga_reminder.domain.entity.Subscription;
import com.example.yoga_reminder.domain.entity.User;
import com.example.yoga_reminder.domain.enums.ReminderChannel;
import com.example.yoga_reminder.repository.InvoiceRepository;
import com.example.yoga_reminder.repository.ReminderLogRepository;
import com.example.yoga_reminder.service.AiService;
import com.example.yoga_reminder.service.ReminderService;
import com.example.yoga_reminder.service.WhatsAppService;
import com.example.yoga_reminder.scheduler.RenewalReminderScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderServiceImpl implements ReminderService {

    private final WhatsAppService whatsAppService;
    private final ReminderLogRepository reminderLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final AiService aiService;

    @Override
    @Transactional
    public void sendRenewalReminder(Invoice invoice) {

        Invoice hydrated = invoiceRepository.findWithSubscriptionAndUser(invoice.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        Subscription sub = hydrated.getSubscription();
        User user = sub.getUser();
        LocalDate today = LocalDate.now();

        if (alreadySentToday(hydrated, today)) {
            LocalDate nextDate = RenewalReminderScheduler.calculateNextReminderDate(
                    today.plusDays(1), sub.getEndDate(), hydrated.getReminderCount());
            hydrated.setNextReminderDate(nextDate);
            invoiceRepository.save(hydrated);
            log.info("Skipping reminder for invoice {} - already sent today. nextReminderDate={}",
                    hydrated.getId(), nextDate);
            return;
        }

        // AI crafts a short, personalized WhatsApp reminder.
        String message = aiService.generateReminderMessage(user.getName(), sub.getEndDate());
        log.info("Sending reminder for invoice {} to user {} ({}) with message: {}", invoice.getId(),
                user.getName(), user.getPhoneNumber(), message);

        whatsAppService.sendMessage(user.getPhoneNumber(), message);

        ReminderLog logEntry = new ReminderLog();
        logEntry.setInvoice(invoice);
        logEntry.setChannel(ReminderChannel.WHATSAPP);
        logEntry.setSentAt(LocalDateTime.now());
        logEntry.setMessagePreview(message);

        reminderLogRepository.save(logEntry);

        int sentCount = invoice.getReminderCount() + 1;
        LocalDate nextReminderDate = RenewalReminderScheduler.calculateNextReminderDate(
                today.plusDays(1), sub.getEndDate(), sentCount);

        invoice.setReminderCount(sentCount);
        invoice.setNextReminderDate(nextReminderDate);
        log.info("Reminder logged for invoice {} (sentCount={}, nextReminderDate={})",
                invoice.getId(), sentCount, invoice.getNextReminderDate());

        invoiceRepository.save(invoice);
    }

    private boolean alreadySentToday(Invoice invoice, LocalDate today) {
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.of(today, LocalTime.MAX);
        return reminderLogRepository.existsByInvoiceAndSentAtBetween(invoice, startOfDay, endOfDay);
    }
}

