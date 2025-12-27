package com.example.yoga_reminder.repository;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findBySubscriptionId(Long subscriptionId);

    List<Invoice> findByPaymentStatusAndNextReminderDate(
            PaymentStatus status,
            LocalDate reminderDate
    );

    @Query("""
            select i from Invoice i
            join fetch i.subscription s
            join fetch s.user
            where i.id = :invoiceId
            """)
    Optional<Invoice> findWithSubscriptionAndUser(@Param("invoiceId") Long invoiceId);

    @Query("""
            select i from Invoice i
            join fetch i.subscription s
            join fetch s.user
            where s.id = :subscriptionId
            """)
    Optional<Invoice> findBySubscriptionWithUser(@Param("subscriptionId") Long subscriptionId);
}

