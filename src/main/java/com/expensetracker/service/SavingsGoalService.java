package com.expensetracker.service;

import com.expensetracker.model.SavingsGoal;
import com.expensetracker.model.User;
import com.expensetracker.repository.SavingsGoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SavingsGoalService {
    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    public List<SavingsGoal> getAll(User user) {
        return savingsGoalRepository.findByUser(user);
    }

    public SavingsGoal add(SavingsGoal savingsGoal) {
        return savingsGoalRepository.save(savingsGoal);
    }

    public SavingsGoal update(SavingsGoal savingsGoal) {
        return savingsGoalRepository.save(savingsGoal);
    }

    public void delete(Long id, User currentUser) {
        SavingsGoal savingsGoal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (savingsGoal.getUser() == null || !savingsGoal.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        savingsGoalRepository.delete(savingsGoal);
    }
}
