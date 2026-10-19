package com.expensetracker.controller;

import com.expensetracker.model.Expense;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserService userService;

    // Get all expenses
    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpense() {

        List<Expense> expenses =
                expenseService.getAllExpenses(
                        userService.findById(1L)
                );

        return ResponseEntity.ok(expenses);
    }

    // Get expenses grouped by category
    @GetMapping("/by-category")
    public ResponseEntity<Map<String, BigDecimal>> getExpensesByCategory(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        Map<String, BigDecimal> expensesByCategory =
                expenseService.getExpensesByCategoryFiltered(
                        userService.findById(1L),
                        month,
                        year
                );

        return ResponseEntity.ok(expensesByCategory);
    }

    // Add expense
    @PostMapping
    public ResponseEntity<?> saveExpense(
            @RequestBody Expense expense) {

        expense.setUser(userService.findById(1L));

        if (expense.getAmount() != null
                && expense.getCategory() != null) {

            Expense savedExpense =
                    expenseService.addExpense(expense);

            return ResponseEntity.ok(savedExpense);
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Amount and category are required.");
    }

    // Update expense
    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable Long id,
            @RequestBody Expense expense) {

        expense.setId(id);
        expense.setUser(userService.findById(1L));

        Expense updated =
                expenseService.update(expense);

        return ResponseEntity.ok(updated);
    }

    // Delete expense
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpenseById(
            @PathVariable Long id) {

        expenseService.deleteExpense(
                id,
                userService.findById(1L)
        );

        return ResponseEntity.noContent().build();
    }

    // Get expenses for a particular month and year
    @GetMapping("/by-month")
    public ResponseEntity<List<Expense>> getExpensesByMonth(
            @RequestParam int month,
            @RequestParam int year) {

        List<Expense> expenses =
                expenseService.getExpensesByMonth(
                        userService.findById(1L),
                        month,
                        year
                );

        // Return HTTP 200 even when no expenses match.
        return ResponseEntity.ok(expenses);
    }
}