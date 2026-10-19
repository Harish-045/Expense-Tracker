package com.expensetracker.repository;

import com.expensetracker.model.DefaultCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefaultCategoryRepository extends JpaRepository<DefaultCategory,Long> {
}
