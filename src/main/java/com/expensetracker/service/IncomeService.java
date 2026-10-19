package com.expensetracker.service;

import com.expensetracker.model.Income;
import com.expensetracker.model.User;
import com.expensetracker.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IncomeService {
    @Autowired
    private IncomeRepository incomeRepository;

    public List<Income> getAll(User user) {
        return incomeRepository.findByUser(user);
    }

    public Income add(Income income) {
        return incomeRepository.save(income);
    }

    public Income update(Income income) {
        return incomeRepository.save(income);
    }

    public void delete(Long id, User currentUser) {
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (income.getUser() == null || !income.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        incomeRepository.delete(income);
    }

    public List<Income> getIncomesByMonth(User user, int month, int year) {
        List<Income> allIncomes = incomeRepository.findByUser(user);
        List<Income> filtered = new ArrayList<>();
        for (Income income : allIncomes) {
            if (income.getDate().getMonthValue() == month && income.getDate().getYear() == year) {
                filtered.add(income);
            }
        }
        return filtered;
    }
}
