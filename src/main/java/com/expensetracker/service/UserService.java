package com.expensetracker.service;

import com.expensetracker.model.Category;
import com.expensetracker.model.DefaultCategory;
import com.expensetracker.model.User;

import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.DefaultCategoryRepository;
import com.expensetracker.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DefaultCategoryRepository defaultCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public UserService(
            UserRepository userRepository,
            DefaultCategoryRepository defaultCategoryRepository,
            CategoryRepository categoryRepository,
            ObjectProvider<HttpServletRequest> requestProvider) {

        this.userRepository = userRepository;
        this.defaultCategoryRepository = defaultCategoryRepository;
        this.categoryRepository = categoryRepository;
        this.requestProvider = requestProvider;
    }

    /*
     * Save a user and initialize default categories.
     */
    @Transactional
    public User saveUser(User user) {

        User savedUser = userRepository.save(user);

        List<DefaultCategory> defaults =
                defaultCategoryRepository.findAll();

        if (categoryRepository.findByUser(savedUser).isEmpty()) {

            List<Category> categories = defaults.stream()
                    .map(defaultCategory -> {

                        Category category = new Category();

                        category.setName(defaultCategory.getName());
                        category.setType(defaultCategory.getType());
                        category.setUser(savedUser);

                        return category;
                    })
                    .toList();

            categoryRepository.saveAll(categories);
        }

        return savedUser;
    }

    /*
     * Resolve the current user from Spring Security.
     *
     * Existing controllers can continue calling findById(1L).
     * For authenticated application requests, the ID argument is
     * ignored and the user is resolved from the verified JWT.
     *
     * Authentication endpoints retain their original ID lookup.
     * Calls outside an HTTP request retain their original behavior.
     */
    public User findById(Long id) {

        HttpServletRequest request;

        try {
            request = requestProvider.getIfAvailable();

            // Preserve existing behavior outside HTTP requests.
            if (request == null) {
                return userRepository.findById(id).orElse(null);
            }

            String uri = request.getRequestURI();

            // Preserve direct user lookup for authentication endpoints.
            if (uri.startsWith("/api/auth/")) {
                return userRepository.findById(id).orElse(null);
            }

            Authentication authentication =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal()
                    instanceof UserDetails userDetails)) {

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Please log in to continue."
                );
            }

            String email = userDetails.getUsername();

            return userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED,
                                    "Authenticated user was not found."
                            )
                    );

        } catch (IllegalStateException exception) {

            // No active HTTP request: preserve the original lookup.
            return userRepository.findById(id).orElse(null);
        }
    }
}