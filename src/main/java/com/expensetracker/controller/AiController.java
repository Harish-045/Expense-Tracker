package com.expensetracker.controller;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Income;
import com.expensetracker.model.SavingsGoal;
import com.expensetracker.model.User;

import com.expensetracker.service.AiService;
import com.expensetracker.service.BudgetService;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.IncomeService;
import com.expensetracker.service.SavingsGoalService;
import com.expensetracker.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private IncomeService incomeService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private SavingsGoalService savingsGoalService;

    @Autowired
    private UserService userService;

    /*
     * Monthly Spending Summary
     */
    @GetMapping("/summary")
    public ResponseEntity<String> getMonthlySummary(
            @RequestParam int month,
            @RequestParam int year) {

        validateMonthAndYear(month, year);

        User user = getCurrentUser();
        String userName = getUserName(user);

        Map<String, BigDecimal> expensesByCategory =
                expenseService.getExpensesByCategoryFiltered(
                        user,
                        month,
                        year
                );

        BigDecimal totalExpenses = sumAmounts(
                expensesByCategory.values().stream().toList()
        );

        List<Income> monthlyIncomes =
                incomeService.getIncomesByMonth(
                        user,
                        month,
                        year
                );

        BigDecimal totalIncome = sumAmounts(
                monthlyIncomes.stream()
                        .map(Income::getAmount)
                        .toList()
        );

        String result = aiService.getMonthlySpendingSummary(
                userName,
                expensesByCategory,
                totalExpenses,
                totalIncome,
                month,
                year
        );

        return ResponseEntity.ok(result);
    }

    /*
     * AI Budget Suggestions
     */
    @GetMapping("/budget-suggestions")
    public ResponseEntity<String> getBudgetSuggestions() {

        User user = getCurrentUser();
        String userName = getUserName(user);

        Map<String, BigDecimal> expensesByCategory =
                expenseService.getExpensesByCategory(user);

        List<Budget> budgets =
                budgetService.getAll(user);

        String result = aiService.getBudgetSuggestions(
                userName,
                expensesByCategory,
                budgets
        );

        return ResponseEntity.ok(result);
    }

    /*
     * AI Savings Advice
     */
    @GetMapping("/savings-advice")
    public ResponseEntity<String> getSavingsAdvice() {

        User user = getCurrentUser();
        String userName = getUserName(user);

        List<SavingsGoal> savingsGoals =
                savingsGoalService.getAll(user);

        List<Income> incomes =
                incomeService.getAll(user);

        BigDecimal totalIncome = sumAmounts(
                incomes.stream()
                        .map(Income::getAmount)
                        .toList()
        );

        Map<String, BigDecimal> expensesByCategory =
                expenseService.getExpensesByCategory(user);

        BigDecimal totalExpenses = sumAmounts(
                expensesByCategory.values().stream().toList()
        );

        String result = aiService.getSavingsAdvice(
                userName,
                savingsGoals,
                totalIncome,
                totalExpenses
        );

        return ResponseEntity.ok(result);
    }

    /*
     * Resolve the logged-in user.
     */
    private User getCurrentUser() {

        User user = userService.findById(1L);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in to continue."
            );
        }

        return user;
    }

    /*
     * Get the user's actual name.
     */
    private String getUserName(User user) {

        if (user.getName() == null || user.getName().isBlank()) {
            return "";
        }

        return user.getName().trim();
    }

    /*
     * Safely calculate totals.
     */
    private BigDecimal sumAmounts(List<BigDecimal> amounts) {

        return amounts.stream()
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /*
     * Validate requested month and year.
     */
    private void validateMonthAndYear(int month, int year) {

        if (month < 1 || month > 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Month must be between 1 and 12."
            );
        }

        if (year < 1900 || year > 9999) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please provide a valid year."
            );
        }
    }
}