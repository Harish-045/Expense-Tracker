package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.model.SavingsGoal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final String OLLAMA_URL =
            System.getenv().getOrDefault(
                    "OLLAMA_URL",
                    "http://localhost:11434/api/generate"
            );

    private static final String MODEL =
            System.getenv().getOrDefault("OLLAMA_MODEL", "llama3.2:3b");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AiService(ObjectMapper objectMapper) {
        this.webClient = WebClient.builder().build();
        this.objectMapper = objectMapper;
    }

    // 1. Monthly spending summary
    public String getMonthlySpendingSummary(
            String userName,
            Map<String, BigDecimal> categoryTotals,
            BigDecimal totalExpenses,
            BigDecimal totalIncome,
            int month,
            int year) {

        String prompt = """
                You are SpendWise AI, a concise personal finance assistant.

                Address the user by their name when greeting them.
                User name: %s

                Month: %d/%d
                Income: ₹%s
                Expenses: ₹%s
                Spending by category: %s

                Give a concise personalized financial summary.

                Include:
                - One sentence about their financial position.
                - Their highest spending category.
                - Two practical ways to save money.

                Maximum 90 words. Use simple English and bullet points.
                Use only the supplied financial data.
                Do not repeat the entire input.
                If the name is missing, do not invent one.
                """.formatted(
                safeName(userName),
                month,
                year,
                amount(totalIncome),
                amount(totalExpenses),
                categoryTotals == null ? "{}" : categoryTotals
        );

        return generateResponse(prompt);
    }

    // 2. Budget suggestions
    public String getBudgetSuggestions(
            String userName,
            Map<String, BigDecimal> categoryTotals,
            List<Budget> budgets) {

        String prompt = """
                You are SpendWise AI, a personal budgeting assistant.

                User name: %s
                Spending by category: %s
                Existing budgets: %s

                Give personalized budget recommendations.

                Include:
                - The category needing the most attention.
                - Two realistic actions to control spending.
                - One simple weekly budgeting tip.

                Maximum 90 words. Use simple English and bullet points.
                Do not invent transactions or budget amounts.
                If the name is missing, do not invent one.
                """.formatted(
                safeName(userName),
                categoryTotals == null ? "{}" : categoryTotals,
                budgets == null ? "No budgets available" : budgets
        );

        return generateResponse(prompt);
    }

    // 3. Savings advice
    public String getSavingsAdvice(
            String userName,
            List<SavingsGoal> savingsGoals,
            BigDecimal totalIncome,
            BigDecimal totalExpenses) {

        String prompt = """
                You are SpendWise AI, a personal savings assistant.

                User name: %s
                Monthly income: ₹%s
                Monthly expenses: ₹%s
                Savings goals: %s

                Give personalized savings advice.

                Include:
                - A brief assessment of their finances.
                - Two practical saving actions.
                - One next step toward their savings goals.

                Maximum 90 words. Use simple English and bullet points.
                Do not invent financial figures.
                If the name is missing, do not invent one.
                """.formatted(
                safeName(userName),
                amount(totalIncome),
                amount(totalExpenses),
                savingsGoals == null ? "No goals available" : savingsGoals
        );

        return generateResponse(prompt);
    }

    // Generate a concise response using local Ollama.
    private String generateResponse(String prompt) {

        try {
            Map<String, Object> request = new HashMap<>();

            request.put("model", MODEL);
            request.put("prompt", prompt);
            request.put("stream", false);
            request.put("keep_alive", "5m");

            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.2);
            options.put("num_predict", 120);
            options.put("num_ctx", 2048);

            request.put("options", options);

            String responseBody = webClient.post()
                    .uri(OLLAMA_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                return "I couldn't generate a response. Please try again.";
            }

            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode response = json.get("response");

            if (response == null || response.asText().isBlank()) {
                return "I couldn't generate a response. Please try again.";
            }

            return response.asText().trim();

        } catch (Exception e) {

            System.err.println("Ollama error: " + e.getMessage());

            return "The AI service is temporarily unavailable. "
                    + "Please ensure Ollama is running and try again.";
        }
    }

    private String amount(BigDecimal value) {
        return value == null ? "0.00" : value.toPlainString();
    }

    private String safeName(String name) {
        return name == null || name.isBlank() ? "" : name.trim();
    }
}