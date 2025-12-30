package com.example.yoga_reminder.controller;

import com.example.yoga_reminder.dto.response.InvoiceResponse;
import com.example.yoga_reminder.scheduler.RenewalReminderScheduler;
import com.example.yoga_reminder.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final InvoiceService invoiceService;
    private final RenewalReminderScheduler scheduler;

    @PostMapping("/subscriptions/{id}/invoice")
    public ResponseEntity<InvoiceResponse> createInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.createInvoice(id));
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @PostMapping("/reminders/run")
    public ResponseEntity<String> runScheduler() {
        scheduler.processRenewals();
        return ResponseEntity.ok("Scheduler triggered");
    }
}

