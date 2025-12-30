package com.example.yoga_reminder.service.Impl;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.entity.PaymentAction;
import com.example.yoga_reminder.domain.enums.PaymentIntent;
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

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        // AI interprets the user's free-text reply into structured intent.
        AiDecision decision = aiService.detectIntent(dto.getMessage());
        PaymentIntent intent = mapIntent(decision.intent());

        PaymentAction action = new PaymentAction();
        action.setInvoice(invoice);
        action.setUserReplyText(dto.getMessage());
        action.setDetectedIntent(intent);
        action.setActionTime(LocalDateTime.now());

        paymentActionRepository.save(action);

        invoice.setPaymentIntent(intent);

        if (intent == PaymentIntent.PAY_LATER) {
            int followUpDays = normalizeFollowUpDays(decision.followUpDays());
            invoice.setNextReminderDate(LocalDate.now().plusDays(followUpDays));
        }

        invoiceRepository.save(invoice);
    }

    private PaymentIntent mapIntent(IntentType intentType) {
        return intentType == IntentType.PAY_LATER
                ? PaymentIntent.PAY_LATER
                : PaymentIntent.PAY_NOW;
    }

    private int normalizeFollowUpDays(Integer followUpDays) {
        int days = followUpDays != null ? followUpDays : 3;
        days = Math.max(1, Math.min(7, days));
        return days;
    }
}

