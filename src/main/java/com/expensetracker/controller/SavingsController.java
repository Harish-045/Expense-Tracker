package com.expensetracker.controller;

import com.expensetracker.model.SavingsGoal;
import com.expensetracker.service.SavingsGoalService;
import com.expensetracker.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/savings")
public class SavingsController {

    @Autowired
    private SavingsGoalService savingsGoalService;

    @Autowired
    private UserService userService;

    // Get all savings goals
    @GetMapping
    public ResponseEntity<List<SavingsGoal>> getAllSavings() {

        List<SavingsGoal> savings =
                savingsGoalService.getAll(
                        userService.findById(1L)
                );

        return ResponseEntity.ok(savings);
    }

    // Add savings goal
    @PostMapping
    public ResponseEntity<?> saveSavingsGoal(
            @RequestBody SavingsGoal savingsGoal) {

        savingsGoal.setUser(userService.findById(1L));

        if (savingsGoal.getTargetAmount() != null) {

            SavingsGoal savedGoal =
                    savingsGoalService.add(savingsGoal);

            return ResponseEntity.ok(savedGoal);
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Savings target amount is required.");
    }

    // Update savings goal
    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoal> updateSavingsGoal(
            @PathVariable Long id,
            @RequestBody SavingsGoal savingsGoal) {

        savingsGoal.setId(id);
        savingsGoal.setUser(userService.findById(1L));

        SavingsGoal updated =
                savingsGoalService.update(savingsGoal);

        return ResponseEntity.ok(updated);
    }

    // Delete savings goal
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSavingsGoalById(
            @PathVariable Long id) {

        savingsGoalService.delete(
                id,
                userService.findById(1L)
        );

        return ResponseEntity.noContent().build();
    }
}