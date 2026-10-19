package com.expensetracker.repository;

import com.expensetracker.model.CategoryType;
import com.expensetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.expensetracker.model.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserAndType(User user, CategoryType type);
    List<Category> findByUser(User user) ;

}
