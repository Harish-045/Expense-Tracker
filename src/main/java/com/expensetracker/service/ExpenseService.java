package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Expense;
import com.expensetracker.model.User;

import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ExpenseService {

    private static final BigDecimal ALERT_THRESHOLD =
            new BigDecimal("0.80");

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private EmailService emailService;

    public List<Expense> getAllExpenses(User user) {
        return expenseRepository.findByUser(user);
    }

    @Transactional
    public Expense addExpense(Expense expense) {

        Expense savedExpense = expenseRepository.save(expense);

        checkBudgetAndSendAlert(savedExpense);

        return savedExpense;
    }

    public Expense update(Expense expense) {
        return expenseRepository.save(expense);
    }

    public void deleteExpense(Long id, User currentUser) {

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("Expense not found")
                );

        if (expense.getUser() == null
                || !expense.getUser().getId()
                .equals(currentUser.getId())) {

            throw new RuntimeException("Unauthorized");
        }

        expenseRepository.delete(expense);
    }

    public Map<String, BigDecimal> getExpensesByCategoryFiltered(
            User user,
            Integer month,
            Integer year) {

        List<Expense> expenses =
                (month != null && year != null)
                        ? getExpensesByMonth(user, month, year)
                        : expenseRepository.findByUser(user);

        Map<String, BigDecimal> result = new HashMap<>();

        for (Expense expense : expenses) {

            String categoryName =
                    expense.getCategory() != null
                            ? expense.getCategory().getName()
                            : "Uncategorized";

            result.merge(
                    categoryName,
                    expense.getAmount(),
                    BigDecimal::add
            );
        }

        return result;
    }

    public List<Expense> getExpensesByMonth(
            User user,
            int month,
            int year) {

        List<Expense> allExpenses =
                expenseRepository.findByUser(user);

        List<Expense> filtered = new ArrayList<>();

        for (Expense expense : allExpenses) {

            if (expense.getDate() != null
                    && expense.getDate().getMonthValue() == month
                    && expense.getDate().getYear() == year) {

                filtered.add(expense);
            }
        }

        return filtered;
    }

    public Map<String, BigDecimal> getExpensesByCategory(User user) {
        return getExpensesByCategoryFiltered(user, null, null);
    }

    private void checkBudgetAndSendAlert(Expense expense) {

        if (expense.getUser() == null
                || expense.getDate() == null
                || expense.getCategory() == null
                || expense.getAmount() == null) {

            return;
        }

        User user = expense.getUser();

        int month = expense.getDate().getMonthValue();
        int year = expense.getDate().getYear();

        Long categoryId = expense.getCategory().getId();

        List<Budget> budgets =
                budgetRepository.findByUserAndMonthAndYear(
                        user,
                        month,
                        year
                );

        List<Expense> monthlyExpenses =
                getExpensesByMonth(user, month, year);

        BigDecimal totalForCategory = monthlyExpenses.stream()
                .filter(e -> e.getCategory() != null)
                .filter(e -> Objects.equals(
                        e.getCategory().getId(),
                        categoryId
                ))
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Budget budget : budgets) {

            if (budget.getCategory() == null) {
                continue;
            }

            if (!Objects.equals(
                    budget.getCategory().getId(),
                    categoryId
            )) {
                continue;
            }

            BigDecimal budgetLimit = budget.getLimitAmount();

            if (budgetLimit == null
                    || budgetLimit.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Calculate 80% of the category budget.
            BigDecimal alertLimit =
                    budgetLimit.multiply(ALERT_THRESHOLD);

            // Trigger when spending reaches or crosses 80%.
            boolean thresholdReached =
                    totalForCategory.compareTo(alertLimit) >= 0;

            // Send only one alert per budget period.
            if (!thresholdReached || budget.isAlertSent()) {
                continue;
            }

            String email = user.getEmail();

            if (email == null || email.isBlank()) {
                continue;
            }

            boolean sent = emailService.sendBudgetExceededAlert(
                    email,
                    budget.getCategory().getName(),
                    month,
                    year,
                    totalForCategory,
                    budgetLimit
            );

            // Mark the alert as sent only after successful delivery.
            if (sent) {
                budget.setAlertSent(true);
                budgetRepository.save(budget);
            }
        }
    }
}