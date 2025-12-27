package com.example.yoga_reminder.service.Impl;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.entity.PaymentAction;
import com.example.yoga_reminder.domain.enums.PaymentIntent;
import com.example.yoga_reminder.domain.enums.PaymentStatus;
import com.example.yoga_reminder.domain.enums.IntentType;
import com.example.yoga_reminder.dto.AiDecision;
import com.example.yoga_reminder.dto.response.WhatsAppResponse;
import com.example.yoga_reminder.repository.InvoiceRepository;
import com.example.yoga_reminder.repository.PaymentActionRepository;
import com.example.yoga_reminder.service.AiService;
import com.example.yoga_reminder.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentActionRepository paymentActionRepository;
    private final AiService aiService;

    @Override
    public void processReply(WhatsAppResponse dto) {

        Long invoiceId = dto.getInvoiceId();
        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice id is required");
        }

        Invoice invoice = invoiceRepository.findWithSubscriptionUserAndPlan(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        // AI interprets the user's free-text reply into structured intent.
        AiDecision decision = aiService.detectIntent(dto.getMessage());
        PaymentIntent intent = mapIntent(decision.intent());
        log.info("Processed AI intent for invoice {}: intent={}, followUpDays={}", invoiceId, intent,
                decision.followUpDays());

        PaymentAction action = new PaymentAction();
        action.setInvoice(invoice);
        action.setUserReplyText(dto.getMessage());
        action.setDetectedIntent(intent);
        action.setActionTime(LocalDateTime.now());

        paymentActionRepository.save(action);
        log.info("Saved payment action id={} for invoice {} with intent {}", action.getId(), invoiceId, intent);

        if (intent == PaymentIntent.PAY_NOW) {
            applyPayNow(invoice);
        } else if (intent == PaymentIntent.PAY_LATER) {
            applyPayLater(invoice, decision);
        } else {
            applyDecline(invoice);
        }

        invoiceRepository.save(invoice);
        log.info("Updated invoice {} after processing reply: paymentIntent={}, paymentStatus={}, nextReminderDate={}",
                invoiceId, invoice.getPaymentIntent(), invoice.getPaymentStatus(), invoice.getNextReminderDate());
    }

    private PaymentIntent mapIntent(IntentType intentType) {
        if (intentType == IntentType.PAY_LATER) {
            return PaymentIntent.PAY_LATER;
        } else if (intentType == IntentType.DECLINE) {
            return PaymentIntent.DECLINE;
        }
        return PaymentIntent.PAY_NOW;
    }

    private void applyPayNow(Invoice invoice) {
        invoice.setPaymentIntent(PaymentIntent.PAY_NOW);
        // Do not auto-mark paid; simulate redirect to payment and keep status pending.
        invoice.setPaymentStatus(PaymentStatus.PENDING);
        invoice.setNextReminderDate(null); // pause reminders while user pays

        log.info("PAY_NOW intent received for invoice {} - redirecting user to payment flow (mock).", invoice.getId());
    }

    private void applyPayLater(Invoice invoice, AiDecision decision) {
        invoice.setPaymentIntent(PaymentIntent.PAY_LATER);
        int followUpDays = normalizeFollowUpDays(decision.followUpDays());
        LocalDate today = LocalDate.now();
        LocalDate userPreferredDate = today.plusDays(followUpDays);
        // Cap reminders to stay within subscription validity. Prevents chasing users after expiry.
        LocalDate expiryBasedLimit = invoice.getSubscription().getEndDate().minusDays(1);
        LocalDate finalReminderDate = userPreferredDate.isBefore(expiryBasedLimit)
                ? userPreferredDate
                : expiryBasedLimit;

        if (expiryBasedLimit.isBefore(today)) {
            // Subscription already past the allowed reminder window; stop and mark failed.
            invoice.setPaymentStatus(PaymentStatus.FAILED);
            invoice.setNextReminderDate(null);
            log.info("PAY_LATER intent but expiry window passed (expiry limit {}). Marking invoice {} FAILED.",
                    expiryBasedLimit, invoice.getId());
            return;
        }

        invoice.setNextReminderDate(finalReminderDate);
        invoice.setPaymentStatus(PaymentStatus.PENDING);
        log.info("PAY_LATER intent; followUpDays={}, userPreferredDate={}, cappedByExpiry={}, finalNextReminderDate={}",
                followUpDays, userPreferredDate, expiryBasedLimit, finalReminderDate);
    }

    private void applyDecline(Invoice invoice) {
        invoice.setPaymentIntent(PaymentIntent.DECLINE);
        invoice.setPaymentStatus(PaymentStatus.FAILED);
        invoice.setNextReminderDate(null); // stop reminders
        log.info("DECLINE intent; marking invoice {} as FAILED and stopping reminders.", invoice.getId());
    }

    private int normalizeFollowUpDays(Integer followUpDays) {
        int days = followUpDays != null ? followUpDays : 3;
        days = Math.max(1, Math.min(7, days));
        return days;
    }
}

