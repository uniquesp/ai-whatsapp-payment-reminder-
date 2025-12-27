package com.example.yoga_reminder.service.Impl;

import com.example.yoga_reminder.domain.entity.Invoice;
import com.example.yoga_reminder.domain.entity.ReminderLog;
import com.example.yoga_reminder.domain.entity.Subscription;
import com.example.yoga_reminder.domain.entity.User;
import com.example.yoga_reminder.domain.enums.ReminderChannel;
import com.example.yoga_reminder.repository.InvoiceRepository;
import com.example.yoga_reminder.repository.ReminderLogRepository;
import com.example.yoga_reminder.service.AiService;
import com.example.yoga_reminder.service.ReminderService;
import com.example.yoga_reminder.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderServiceImpl implements ReminderService {

    private final WhatsAppService whatsAppService;
    private final ReminderLogRepository reminderLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final AiService aiService;

    @Override
    public void sendRenewalReminder(Invoice invoice) {

        Subscription sub = invoice.getSubscription();
        User user = sub.getUser();

        // AI crafts a short, personalized WhatsApp reminder.
        String message = aiService.generateReminderMessage(user.getName(), sub.getEndDate());

        whatsAppService.sendMessage(user.getPhoneNumber(), message);

        ReminderLog logEntry = new ReminderLog();
        logEntry.setInvoice(invoice);
        logEntry.setChannel(ReminderChannel.WHATSAPP);
        logEntry.setSentAt(LocalDateTime.now());
        logEntry.setMessagePreview(message);

        reminderLogRepository.save(logEntry);

        invoice.setReminderCount(invoice.getReminderCount() + 1);
        invoice.setNextReminderDate(LocalDate.now().plusDays(2));

        invoiceRepository.save(invoice);
    }
}

