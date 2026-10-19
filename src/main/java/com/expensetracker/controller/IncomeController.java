package com.expensetracker.controller;

import com.expensetracker.model.Income;
import com.expensetracker.service.IncomeService;
import com.expensetracker.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/income")
public class IncomeController {

    @Autowired
    private IncomeService incomeService;

    @Autowired
    private UserService userService;

    // Get all income records
    @GetMapping
    public ResponseEntity<List<Income>> getAllIncome() {

        List<Income> incomes =
                incomeService.getAll(
                        userService.findById(1L)
                );

        return ResponseEntity.ok(incomes);
    }

    // Add income
    @PostMapping
    public ResponseEntity<?> saveIncome(
            @RequestBody Income income) {

        income.setUser(userService.findById(1L));

        if (income.getAmount() != null) {

            Income savedIncome =
                    incomeService.add(income);

            return ResponseEntity.ok(savedIncome);
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Income amount is required.");
    }

    // Update income
    @PutMapping("/{id}")
    public ResponseEntity<Income> updateIncome(
            @PathVariable Long id,
            @RequestBody Income income) {

        income.setId(id);
        income.setUser(userService.findById(1L));

        Income updated =
                incomeService.update(income);

        return ResponseEntity.ok(updated);
    }

    // Delete income
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncomeById(
            @PathVariable Long id) {

        incomeService.delete(
                id,
                userService.findById(1L)
        );

        return ResponseEntity.noContent().build();
    }

    // Get income for a particular month and year
    @GetMapping("/by-month")
    public ResponseEntity<List<Income>> getIncomeByMonth(
            @RequestParam int month,
            @RequestParam int year) {

        List<Income> incomes =
                incomeService.getIncomesByMonth(
                        userService.findById(1L),
                        month,
                        year
                );

        return ResponseEntity.ok(incomes);
    }
}