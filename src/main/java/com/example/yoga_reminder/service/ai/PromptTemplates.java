package com.example.yoga_reminder.service.ai;

import java.time.LocalDate;

public final class PromptTemplates {

    private PromptTemplates() {
    }

    public static String intentSystemPrompt() {
        return """
                You are an assistant that classifies WhatsApp replies about yoga class payments.
                Respond with JSON ONLY (no markdown, no prose). Use exactly this schema:
                {
                  "intent": "PAY_NOW" | "PAY_LATER" | "DECLINE",
                  "followUpDays": <number|null>
                }
                Intent meanings:
                - PAY_NOW: user will pay immediately (e.g., "pay now", "pay immediately", "done").
                - PAY_LATER: user will pay later (e.g., "tomorrow", "next day", "next week", "later").
                - DECLINE: user refuses or cancels (e.g., "not pay", "cancel", "stop", "no", "don't want").
                Examples:
                - "I'll pay next day" -> PAY_LATER with followUpDays between 1-7
                - "I'll pay next week" -> PAY_LATER with followUpDays 7
                - "I'll not pay" -> DECLINE with followUpDays null
                Output rules:
                - followUpDays MUST be null for PAY_NOW and DECLINE.
                - For PAY_LATER, followUpDays MUST be 1-7 (choose a sensible value based on phrasing).
                - Do NOT guess PAY_NOW unless payment is immediate.
                - Return only valid JSON.
                """;
    }

    public static String intentUserPrompt(String userReply) {
        return """
                Infer the intent from this WhatsApp reply and return the JSON only.
                Reply text: "%s"
                """.formatted(userReply);
    }

    public static String reminderSystemPrompt() {
        return """
                You write concise, warm WhatsApp payment reminders for yoga students.
                Output plain text only (no JSON/markdown). Keep it under 45 words.
                The message must include the student's name, the plan expiry date, and
                end by asking them to reply with PAY NOW or PAY LATER.
                """;
    }

    public static String reminderUserPrompt(String name, LocalDate expiryDate) {
        return """
                Create a short reminder that mentions:
                - Student name: %s
                - Plan expiry date: %s
                """.formatted(name, expiryDate);
    }
}

