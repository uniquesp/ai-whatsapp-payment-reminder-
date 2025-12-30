package com.example.yoga_reminder.controller;

import com.example.yoga_reminder.dto.response.WhatsAppResponse;
import com.example.yoga_reminder.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/reply")
    public ResponseEntity<String> handleReply(@RequestBody WhatsAppResponse dto) {
        paymentService.processReply(dto);
        return ResponseEntity.ok("Reply processed");
    }
}

