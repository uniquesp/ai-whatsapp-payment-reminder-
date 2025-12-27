package com.example.yoga_reminder.service.Impl;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.entity.Subscription;
import com.example.yoga_reminder.domain.enums.PaymentStatus;
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
    public Invoice createInvoice(Long subscriptionId) {

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        return invoiceRepository.findBySubscriptionId(subscriptionId)
                .orElseGet(() -> {
                    Invoice invoice = new Invoice();
                    invoice.setSubscription(subscription);
                    invoice.setPaymentStatus(PaymentStatus.PENDING);
                    invoice.setPaymentIntent(null);
                    invoice.setNextReminderDate(LocalDate.now());
                    invoice.setReminderCount(0);

                    log.info("Invoice created for subscription {}", subscriptionId);
                    return invoiceRepository.save(invoice);
                });
    }

    @Override
    public Invoice getInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
    }
}

