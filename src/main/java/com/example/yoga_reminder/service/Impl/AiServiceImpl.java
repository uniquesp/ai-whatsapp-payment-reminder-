package com.example.yoga_reminder.service.Impl;

import java.time.LocalDate;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.yoga_reminder.domain.enums.IntentType;
import com.example.yoga_reminder.dto.AiDecision;
import com.example.yoga_reminder.service.AiService;
import com.example.yoga_reminder.service.ai.PromptTemplates;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectMapper objectMapper;

    @Override
    public AiDecision detectIntent(String userReply) {
        String safeUserReply = userReply == null ? "" : userReply;
        ChatClient client = chatClientProvider.getIfAvailable();
        if (client == null) {
            return fallbackIntent(safeUserReply, "AI client not configured");
        }

        try {
            log.info("Calling AI for intent detection with reply text: {}", safeUserReply);
            String raw = client.prompt()
                    .system(PromptTemplates.intentSystemPrompt())
                    .user(PromptTemplates.intentUserPrompt(safeUserReply))
                    .call()
                    .content();
            log.info("AI raw response for intent detection: {}", raw);

            AiDecision parsed = objectMapper.readValue(raw, AiDecision.class);
            return normalizeDecision(parsed, safeUserReply);
        } catch (Exception ex) {
            log.warn("AI intent detection failed, falling back. reason={}", ex.getMessage());
            return fallbackIntent(safeUserReply, "AI inference failure");
        }
    }

    @Override
    public String generateReminderMessage(String userName, LocalDate expiryDate) {
        String fallback = fallbackReminderMessage(userName, expiryDate);
        ChatClient client = chatClientProvider.getIfAvailable();
        if (client == null) {
            return fallback;
        }

        try {
            log.info("Calling AI for reminder message for user {} expiring {}", userName, expiryDate);
            return client.prompt()
                    .system(PromptTemplates.reminderSystemPrompt())
                    .user(PromptTemplates.reminderUserPrompt(userName, expiryDate))
                    .call()
                    .content();
        } catch (Exception ex) {
            log.warn("AI reminder generation failed, using fallback. reason={}", ex.getMessage());
            return fallback;
        }
    }

    private AiDecision normalizeDecision(AiDecision decision, String userReply) {
        if (decision == null || decision.intent() == null) {
            return fallbackIntent(userReply, "missing intent");
        }

        IntentType intent = decision.intent();
        Integer followUpDays = normalizeFollowUp(intent, decision.followUpDays());
        return new AiDecision(intent, followUpDays);
    }

    private Integer normalizeFollowUp(IntentType intent, Integer provided) {
        if (intent == IntentType.PAY_NOW || intent == IntentType.DECLINE) {
            return null;
        }
        int days = provided == null ? 3 : provided;
        days = Math.max(1, Math.min(7, days));
        return days;
    }

    private AiDecision fallbackIntent(String userReply, String reason) {
        log.info("Using rule-based intent detection: {}", reason);
        String text = userReply == null ? "" : userReply.toLowerCase();
        IntentType intent;
        if (text.contains("not pay") || text.contains("cancel") || text.contains("stop") || text.contains("don't want") || text.contains("do not want") || text.equals("no")) {
            intent = IntentType.DECLINE;
        } else if (text.contains("later") || text.contains("tomorrow") || text.contains("next week") || text.contains("week") || text.contains("next day")) {
            intent = IntentType.PAY_LATER;
        } else if (text.contains("pay now") || text.contains("immediately") || text.contains("done")) {
            intent = IntentType.PAY_NOW;
        } else {
            intent = IntentType.PAY_LATER; // safest default to re-engage later
        }
        return new AiDecision(intent, normalizeFollowUp(intent, null));
    }

    private String fallbackReminderMessage(String userName, LocalDate expiryDate) {
        // Use a deterministic message when AI is unavailable so reminders still send.
        return "Hi %s, your yoga plan expires on %s. Reply PAY NOW to renew or PAY LATER to choose a new reminder date."
                .formatted(userName, expiryDate);
    }
}

