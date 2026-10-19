package com.expensetracker.controller;

import com.expensetracker.model.Budget;
import com.expensetracker.service.BudgetService;
import com.expensetracker.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private UserService userService;

    // Get all budgets
    @GetMapping
    public ResponseEntity<List<Budget>> getAllBudgets() {

        List<Budget> budgets =
                budgetService.getAll(
                        userService.findById(1L)
                );

        return ResponseEntity.ok(budgets);
    }

    // Add budget
    @PostMapping
    public ResponseEntity<?> saveBudget(
            @RequestBody Budget budget) {

        budget.setUser(userService.findById(1L));

        if (budget.getLimitAmount() != null) {

            Budget savedBudget =
                    budgetService.add(budget);

            return ResponseEntity.ok(savedBudget);
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Budget limit is required.");
    }

    // Update budget
    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(
            @PathVariable Long id,
            @RequestBody Budget budget) {

        budget.setId(id);
        budget.setUser(userService.findById(1L));

        Budget updated =
                budgetService.update(budget);

        return ResponseEntity.ok(updated);
    }

    // Delete budget
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudgetById(
            @PathVariable Long id) {

        budgetService.delete(
                id,
                userService.findById(1L)
        );

        return ResponseEntity.noContent().build();
    }
}