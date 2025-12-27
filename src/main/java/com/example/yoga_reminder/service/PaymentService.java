package com.example.yoga_reminder.service;

import com.example.yoga_reminder.dto.response.WhatsAppResponse;

public interface PaymentService {
    public void processReply(WhatsAppResponse dto);
}
