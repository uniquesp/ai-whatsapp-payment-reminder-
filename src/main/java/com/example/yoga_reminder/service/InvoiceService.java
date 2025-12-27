package com.example.yoga_reminder.service;

import com.example.yoga_reminder.dto.response.InvoiceResponse;

public interface InvoiceService {

    InvoiceResponse createInvoice(Long subscriptionId);

    InvoiceResponse getInvoice(Long invoiceId);
}

