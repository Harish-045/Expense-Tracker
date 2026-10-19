package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.model.Category;
import com.expensetracker.model.CategoryType;
import com.expensetracker.model.User;
import com.expensetracker.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryService categoryService;

    public List<Budget> getAll(User user) {
        return budgetRepository.findByUser(user);
    }

    public Budget add(Budget budget) {

        Long categoryId = budget.getCategory().getId();

        Category category =
                categoryService.findById(categoryId);

        if (category.getType() != CategoryType.EXPENSE) {
            throw new RuntimeException(
                    "Budget can only be created for expense categories"
            );
        }

        budget.setCategory(category);

        return budgetRepository.save(budget);
    }

    public Budget update(Budget budget) {

        Long categoryId = budget.getCategory().getId();

        Category category =
                categoryService.findById(categoryId);

        if (category.getType() != CategoryType.EXPENSE) {
            throw new RuntimeException(
                    "Budget can only be created for expense categories"
            );
        }

        budget.setCategory(category);

        return budgetRepository.save(budget);
    }

    public void delete(Long id, User currentUser) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (budget.getUser() == null || !budget.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        budgetRepository.delete(budget);
    }

    public List<Budget> getByMonthAndYear(User user, Integer month, Integer year) {
        return budgetRepository.findByUserAndMonthAndYear(user, month, year);
    }
}
