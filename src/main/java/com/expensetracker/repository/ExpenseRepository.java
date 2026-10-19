package com.expensetracker.repository;

import com.expensetracker.model.Category;
import com.expensetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.expensetracker.model.Expense;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUser(User user);
    List<Expense>findByUserAndCategory(User user, Category category);

}
