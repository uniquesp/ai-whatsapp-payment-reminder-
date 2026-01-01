package com.example.yoga_reminder.service.Impl;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.entity.Subscription;
import com.example.yoga_reminder.domain.entity.User;
import com.example.yoga_reminder.domain.enums.PaymentStatus;
import com.example.yoga_reminder.dto.response.InvoiceResponse;
import com.example.yoga_reminder.repository.InvoiceRepository;
import com.example.yoga_reminder.repository.SubscriptionRepository;
import com.example.yoga_reminder.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    public InvoiceResponse createInvoice(Long subscriptionId) {

        if (subscriptionId == null) {
            throw new IllegalArgumentException("Subscription id is required");
        }

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        Invoice invoice = invoiceRepository.findBySubscriptionId(subscriptionId)
                .orElseGet(() -> {
                    Invoice newInvoice = new Invoice();
                    newInvoice.setSubscription(subscription);
                    newInvoice.setPaymentStatus(PaymentStatus.PENDING);
                    newInvoice.setNextReminderDate(LocalDate.now());
                    newInvoice.setReminderCount(0);

                    log.info("Invoice created for subscription {}", subscriptionId);
                    return invoiceRepository.save(newInvoice);
                });

        // Reload with user to avoid lazy serialization issues.
        Invoice hydrated = invoiceRepository.findWithSubscriptionAndUser(invoice.getId())
                .orElse(invoice);
        return toResponse(hydrated);
    }

    @Override
    public InvoiceResponse getInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findWithSubscriptionAndUser(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        log.info("Fetched invoice {} for subscription {}", invoiceId, invoice.getSubscription().getId());
        return toResponse(invoice);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        Subscription subscription = invoice.getSubscription();
        User user = subscription.getUser();

        return InvoiceResponse.builder()
                .invoiceId(invoice.getId())
                .paymentStatus(invoice.getPaymentStatus())
                .paymentIntent(invoice.getPaymentIntent())
                .nextReminderDate(invoice.getNextReminderDate())
                .reminderCount(invoice.getReminderCount())
                .subscriptionId(subscription.getId())
                .userId(user.getId())
                .userName(user.getName())
                .userPhone(user.getPhoneNumber())
                .build();
    }
}

