package com.example.yoga_reminder.scheduler;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.entity.Subscription;
import com.example.yoga_reminder.domain.enums.PaymentIntent;
import com.example.yoga_reminder.domain.enums.PaymentStatus;
import com.example.yoga_reminder.domain.enums.SubscriptionStatus;
import com.example.yoga_reminder.repository.InvoiceRepository;
import com.example.yoga_reminder.repository.SubscriptionRepository;
import com.example.yoga_reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RenewalReminderScheduler {

    private static final int NOTICE_DAYS = 5;
    private static final int[] REMINDER_STEPS = new int[]{5, 3, 1};

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReminderService reminderService;

    /**
     * Runs daily to handle expiring subscriptions
     */
    @Scheduled(cron = "0 0 9 * * *") // daily at 9 AM
//    @Scheduled(cron = "20 * * * * *") // every minute
    @Transactional
    public void processRenewals() {

        LocalDate today = LocalDate.now();
        LocalDate noticeDate = today.plusDays(NOTICE_DAYS);

        log.info("Running renewal scheduler for window {} -> {}", today, noticeDate);

        List<Subscription> subscriptions =
                subscriptionRepository.findExpiringSubscriptions(today, noticeDate);

        for (Subscription subscription : subscriptions) {

            log.info("Evaluating subscription {} (status={}, endDate={})", subscription.getId(),
                    subscription.getStatus(), subscription.getEndDate());

            if (!isEligibleForProcessing(subscription, today, noticeDate)) {
                log.info("Skipping subscription {} (out of window or inactive)", subscription.getId());
                continue;
            }

            Invoice invoice = invoiceRepository
                    .findBySubscriptionId(subscription.getId())
                    .orElseGet(() -> createInvoice(subscription, today));

            log.info("Working with invoice {} for subscription {}", invoice.getId(), subscription.getId());

            if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
                log.info("Skipping invoice {} for subscription {} because already PAID", invoice.getId(),
                        subscription.getId());
                continue;
            }

            if (isReminderDue(invoice, today)) {
                log.info("Reminder due for invoice {} (reminderCount={}, nextReminderDate={})",
                        invoice.getId(), invoice.getReminderCount(), invoice.getNextReminderDate());
                reminderService.sendRenewalReminder(invoice);
            } else {
                log.info("Reminder not due for invoice {} (reminderCount={}, nextReminderDate={})", invoice.getId(),
                        invoice.getReminderCount(), invoice.getNextReminderDate());
            }
        }
    }

    private boolean isEligibleForProcessing(Subscription subscription, LocalDate today, LocalDate noticeDate) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            return false;
        }
        LocalDate endDate = subscription.getEndDate();
        return !endDate.isBefore(today) && !endDate.isAfter(noticeDate);
    }

    private Invoice createInvoice(Subscription subscription, LocalDate today) {

        Invoice invoice = new Invoice();
        invoice.setSubscription(subscription);
        invoice.setPaymentStatus(PaymentStatus.PENDING);
        // Default intent so validation passes; AI will update after user reply.
        invoice.setPaymentIntent(PaymentIntent.PAY_NOW);
        invoice.setNextReminderDate(calculateNextReminderDate(today, subscription.getEndDate(), 0));
        invoice.setReminderCount(0);

        log.info("Created invoice for subscription {}", subscription.getId());
        return invoiceRepository.save(invoice);
    }

    private boolean isReminderDue(Invoice invoice, LocalDate today) {
        if (invoice.getReminderCount() >= REMINDER_STEPS.length) {
            invoice.setNextReminderDate(null);
            log.info("Skipping invoice {} - max reminders reached (count={})", invoice.getId(),
                    invoice.getReminderCount());
            return false;
        }

        if (invoice.getNextReminderDate() == null) {
            // If no next reminder is scheduled but still pending, compute from policy.
            invoice.setNextReminderDate(calculateNextReminderDate(today,
                    invoice.getSubscription().getEndDate(), invoice.getReminderCount()));
            log.info("Computed nextReminderDate for invoice {} as {}", invoice.getId(), invoice.getNextReminderDate());
        }

        if (invoice.getNextReminderDate() == null) {
            log.info("No further reminders scheduled for invoice {} (schedule exhausted)", invoice.getId());
            return false;
        }

        return invoice.getNextReminderDate() != null
                && !invoice.getNextReminderDate().isAfter(today);
    }

    /**
     * Reminder policy: send at days-to-expiry 5, 3, 1.
     * Returns the calendar date for the next reminder based on how many have already been sent.
     * referenceDate is the baseline date to use when we need to "catch up" (e.g., after sending today,
     * we pass tomorrow to avoid scheduling again on the same day).
     */
    public static LocalDate calculateNextReminderDate(LocalDate referenceDate, LocalDate endDate, int reminderCount) {
        if (reminderCount >= REMINDER_STEPS.length) {
            return null;
        }

        int step = REMINDER_STEPS[reminderCount];
        LocalDate targetDate = endDate.minusDays(step);
        LocalDate catchUpDate = referenceDate;
        if (targetDate.isBefore(catchUpDate)) {
            targetDate = catchUpDate;
        }

        LocalDate expiryLimit = endDate.minusDays(1);
        if (targetDate.isAfter(expiryLimit)) {
            return null; // do not schedule beyond expiry window
        }
        return targetDate;
    }
}
