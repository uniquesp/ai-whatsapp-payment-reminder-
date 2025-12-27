package com.example.yoga_reminder.service;

import com.example.yoga_reminder.domain.entity.Invoice;

public interface InvoiceService {

    Invoice createInvoice(Long subscriptionId);

    Invoice getInvoice(Long invoiceId);
}

