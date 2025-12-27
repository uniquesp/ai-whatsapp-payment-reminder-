package com.example.yoga_reminder.scheduler;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.entity.Subscription;
import com.example.yoga_reminder.domain.enums.PaymentIntent;
import com.example.yoga_reminder.domain.enums.PaymentStatus;
import com.example.yoga_reminder.repository.InvoiceRepository;
import com.example.yoga_reminder.repository.SubscriptionRepository;
import com.example.yoga_reminder.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RenewalReminderScheduler {

    private static final int NOTICE_DAYS = 5;

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReminderService reminderService;

    /**
     * Runs daily to handle expiring subscriptions
     */
//    @Scheduled(cron = "0 0 9 * * *") // daily at 9 AM
    @Scheduled(cron = "0 * * * * *") // every minute
    public void processRenewals() {

        LocalDate today = LocalDate.now();
        LocalDate noticeDate = today.plusDays(NOTICE_DAYS);

        log.info("Running renewal scheduler for window {} -> {}", today, noticeDate);

        List<Subscription> subscriptions =
                subscriptionRepository.findExpiringSubscriptions(today, noticeDate);

        for (Subscription subscription : subscriptions) {

            Invoice invoice = invoiceRepository
                    .findBySubscriptionId(subscription.getId())
                    .orElseGet(() -> createInvoice(subscription));

            if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
                continue;
            }

            if (isReminderDue(invoice, today)) {
                reminderService.sendRenewalReminder(invoice);
            }
        }
    }

    private Invoice createInvoice(Subscription subscription) {

        Invoice invoice = new Invoice();
        invoice.setSubscription(subscription);
        invoice.setPaymentStatus(PaymentStatus.PENDING);
        // Default intent so validation passes; AI will update after user reply.
        invoice.setPaymentIntent(PaymentIntent.PAY_NOW);
        invoice.setNextReminderDate(LocalDate.now());
        invoice.setReminderCount(0);

        log.info("Created invoice for subscription {}", subscription.getId());
        return invoiceRepository.save(invoice);
    }

    private boolean isReminderDue(Invoice invoice, LocalDate today) {
        return invoice.getNextReminderDate() != null
                && !invoice.getNextReminderDate().isAfter(today);
    }
}
