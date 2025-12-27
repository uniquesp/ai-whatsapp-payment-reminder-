package com.example.yoga_reminder.dto;

import com.example.yoga_reminder.domain.enums.IntentType;

public record AiDecision(IntentType intent, Integer followUpDays) {
}

