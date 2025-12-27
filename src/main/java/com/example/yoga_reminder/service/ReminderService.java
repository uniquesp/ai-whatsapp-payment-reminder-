package com.example.yoga_reminder.service;

import com.example.yoga_reminder.domain.entity.Invoice;

public interface ReminderService {
    void sendRenewalReminder(Invoice invoice);
}
