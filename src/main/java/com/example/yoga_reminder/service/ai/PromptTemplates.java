package com.example.yoga_reminder.service.ai;

import java.time.LocalDate;

public final class PromptTemplates {

    private PromptTemplates() {
    }

    public static String intentSystemPrompt() {
        return """
                You are an assistant that classifies WhatsApp replies about yoga class payments.
                Respond with JSON only, no markdown, no prose. Use exactly this schema:
                {
                  "intent": "PAY_NOW" | "PAY_LATER",
                  "followUpDays": <number>
                }
                Rules:
                - PAY_NOW means the user wants to pay immediately or already paid.
                - PAY_LATER means the user will pay later, is busy, or schedules payment.
                - followUpDays MUST be 0 for PAY_NOW.
                - For PAY_LATER pick 1-7 days based on the phrase: "tomorrow"=1, "day after tomorrow"=2, "later"/"this week"=3-5, "next week"=7. If unclear, default to 3.
                - Always return intent in upper-case and followUpDays as a number.
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

