package com.expensetracker.controller;

import com.expensetracker.model.Category;
import com.expensetracker.service.CategoryService;
import com.expensetracker.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    // Get all categories
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {

        List<Category> categories =
                categoryService.getAll(
                        userService.findById(1L)
                );

        return ResponseEntity.ok(categories);
    }

    // Add category
    @PostMapping
    public ResponseEntity<?> saveCategory(
            @RequestBody Category category) {

        category.setUser(userService.findById(1L));

        Category savedCategory =
                categoryService.add(category);

        return ResponseEntity.ok(savedCategory);
    }

    // Update category
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Long id,
            @RequestBody Category category) {

        category.setId(id);
        category.setUser(userService.findById(1L));

        Category updated =
                categoryService.update(category);

        return ResponseEntity.ok(updated);
    }

    // Delete category
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategoryById(
            @PathVariable Long id) {

        categoryService.delete(
                id,
                userService.findById(1L)
        );

        return ResponseEntity.noContent().build();
    }
}