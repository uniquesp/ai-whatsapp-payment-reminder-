package com.example.yoga_reminder.service;

import java.time.LocalDate;

import com.example.yoga_reminder.dto.AiDecision;

public interface AiService {

    AiDecision detectIntent(String userReply);

    String generateReminderMessage(String userName, LocalDate expiryDate);
}

