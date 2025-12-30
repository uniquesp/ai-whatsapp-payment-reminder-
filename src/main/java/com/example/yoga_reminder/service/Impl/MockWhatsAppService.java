package com.example.yoga_reminder.service.Impl;

import com.example.yoga_reminder.service.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MockWhatsAppService implements WhatsAppService {

    @Override
    public void sendMessage(String phoneNumber, String message) {
        log.info("📲 WhatsApp message to {} : {}", phoneNumber, message);
    }
}

