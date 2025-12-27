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
        ChatClient client = chatClientProvider.getIfAvailable();
        if (client == null) {
            return fallbackIntent(userReply, "AI client not configured");
        }

        try {
            String raw = client.prompt()
                    .system(PromptTemplates.intentSystemPrompt())
                    .user(PromptTemplates.intentUserPrompt(userReply))
                    .call()
                    .content();

            AiDecision parsed = objectMapper.readValue(raw, AiDecision.class);
            return normalizeDecision(parsed, userReply);
        } catch (Exception ex) {
            log.warn("AI intent detection failed, falling back. reason={}", ex.getMessage());
            return fallbackIntent(userReply, "AI inference failure");
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
        int followUpDays = decision.followUpDays() == null ? defaultFollowUp(intent) : decision.followUpDays();
        if (intent == IntentType.PAY_NOW) {
            followUpDays = 0;
        }
        followUpDays = Math.max(0, Math.min(7, followUpDays));
        return new AiDecision(intent, followUpDays);
    }

    private int defaultFollowUp(IntentType intent) {
        return intent == IntentType.PAY_LATER ? 3 : 0;
    }

    private AiDecision fallbackIntent(String userReply, String reason) {
        log.info("Using rule-based intent detection: {}", reason);
        IntentType intent = userReply != null && userReply.toLowerCase().contains("later")
                ? IntentType.PAY_LATER
                : IntentType.PAY_NOW;
        return new AiDecision(intent, defaultFollowUp(intent));
    }

    private String fallbackReminderMessage(String userName, LocalDate expiryDate) {
        // Use a deterministic message when AI is unavailable so reminders still send.
        return "Hi %s, your yoga plan expires on %s. Reply PAY NOW to renew or PAY LATER to choose a new reminder date."
                .formatted(userName, expiryDate);
    }
}

