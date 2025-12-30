package com.example.yoga_reminder.repository;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.entity.PaymentAction;
import com.example.yoga_reminder.domain.enums.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentActionRepository extends JpaRepository<PaymentAction, Long> {

    /**
     * Fetch all actions performed for a given invoice
     * (audit trail of user replies)
     */
    List<PaymentAction> findByInvoice(Invoice invoice);

    /**
     * Fetch actions by detected intent
     * (useful for analytics / AI tuning later)
     */
    List<PaymentAction> findByDetectedIntent(PaymentIntent detectedIntent);
}
