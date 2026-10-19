package com.expensetracker.controller;

import com.expensetracker.model.PasswordResetToken;
import com.expensetracker.model.User;

import com.expensetracker.repository.PasswordResetTokenRepository;
import com.expensetracker.repository.UserRepository;

import com.expensetracker.security.JwtService;
import com.expensetracker.service.EmailService;
import com.expensetracker.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import java.time.LocalDateTime;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log =
            LoggerFactory.getLogger(AuthController.class);

    private static final String GUEST_EMAIL =
            "demo@spendwise.com";

    private static final String GUEST_PASSWORD =
            "Demo@123";

    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)"
                    + "(?=.*[^A-Za-z\\d]).{8,72}$";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;

    private final SecureRandom secureRandom =
            new SecureRandom();

    @Value("${app.auth.reset-token-minutes:30}")
    private long resetTokenMinutes;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    public AuthController(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    public record RegisterRequest(
            String name,
            String email,
            String password) {
    }

    public record LoginRequest(
            String email,
            String password) {
    }

    public record ForgotPasswordRequest(
            String email) {
    }

    public record ResetPasswordRequest(
            String token,
            String password) {
    }

    /*
     * REGISTER
     *
     * Creates a new user and returns a JWT.
     */
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        if (request.name() == null
                || request.name().isBlank()
                || request.email() == null
                || request.email().isBlank()
                || request.password() == null) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Name, email and password are required."
                    ));
        }

        String email =
                request.email().trim().toLowerCase();

        if (!email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Enter a valid email address."
                    ));
        }

        if (!request.password().matches(PASSWORD_PATTERN)) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Password must contain at least 8 characters, "
                                    + "including uppercase, lowercase, "
                                    + "a number and a special character."
                    ));
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "An account with this email already exists."
                    ));
        }

        User user = new User();

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        User savedUser = userService.saveUser(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authResponse(savedUser));
    }

    /*
     * LOGIN
     *
     * Verifies credentials and returns a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        if (request.email() == null
                || request.password() == null) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Email and password are required."
                    ));
        }

        Optional<User> optionalUser =
                userRepository.findByEmailIgnoreCase(
                        request.email().trim()
                );

        if (optionalUser.isEmpty()
                || !passwordEncoder.matches(
                request.password(),
                optionalUser.get().getPassword())) {

            return ResponseEntity.status(
                    HttpStatus.UNAUTHORIZED
            ).body(Map.of(
                    "message",
                    "Invalid email or password."
            ));
        }

        User user = optionalUser.get();

        return ResponseEntity.ok(authResponse(user));
    }

    /*
     * GUEST LOGIN
     *
     * Creates the demo account if it does not exist.
     */
    @PostMapping("/guest")
    @Transactional
    public ResponseEntity<?> guestLogin() {

        User guest = userRepository
                .findByEmailIgnoreCase(GUEST_EMAIL)
                .orElse(null);

        if (guest == null) {

            guest = new User();

            guest.setName("SpendWise Guest");
            guest.setEmail(GUEST_EMAIL);
            guest.setPassword(
                    passwordEncoder.encode(GUEST_PASSWORD)
            );

            guest = userService.saveUser(guest);
        }

        return ResponseEntity.ok(authResponse(guest));
    }

    /*
     * LOGOUT
     *
     * With stateless JWT authentication, the server does not
     * maintain an HTTP login session to invalidate.
     *
     * The frontend must discard its stored token.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logged out successfully. Discard your JWT."
                )
        );
    }

    /*
     * CURRENT USER
     *
     * The JWT filter authenticates the request and places
     * the user's email in the Spring Security context.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof UserDetails userDetails)) {

            return ResponseEntity.status(
                    HttpStatus.UNAUTHORIZED
            ).body(Map.of(
                    "message",
                    "Authentication required."
            ));
        }

        String email = userDetails.getUsername();

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElse(null);

        if (user == null) {

            return ResponseEntity.status(
                    HttpStatus.UNAUTHORIZED
            ).body(Map.of(
                    "message",
                    "User account not found."
            ));
        }

        return ResponseEntity.ok(userResponse(user));
    }

    /*
     * FORGOT PASSWORD
     *
     * Existing password-reset functionality is preserved.
     */
    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        String responseMessage =
                "If an account exists for that email, "
                        + "password reset instructions will be sent.";

        if (request.email() == null
                || request.email().isBlank()) {

            return ResponseEntity.ok(
                    Map.of("message", responseMessage)
            );
        }

        Optional<User> optionalUser =
                userRepository.findByEmailIgnoreCase(
                        request.email().trim()
                );

        if (optionalUser.isEmpty()) {

            return ResponseEntity.ok(
                    Map.of("message", responseMessage)
            );
        }

        User user = optionalUser.get();

        tokenRepository.deleteAllByUserBulk(user);

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String rawToken =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(bytes);

        PasswordResetToken resetToken =
                new PasswordResetToken();

        resetToken.setUser(user);
        resetToken.setTokenHash(sha256(rawToken));

        resetToken.setExpiresAt(
                LocalDateTime.now()
                        .plusMinutes(resetTokenMinutes)
        );

        resetToken.setUsed(false);

        tokenRepository.save(resetToken);

        String resetLink =
                frontendUrl
                        + "/reset-password.html?token="
                        + rawToken;

        boolean sent =
                emailService.sendPasswordResetEmail(
                        user.getEmail(),
                        resetLink
                );

        if (!sent && !emailService.isMailEnabled()) {

            log.info(
                    "DEVELOPMENT PASSWORD RESET LINK for {}: {}",
                    user.getEmail(),
                    resetLink
            );
        }

        return ResponseEntity.ok(
                Map.of("message", responseMessage)
        );
    }

    /*
     * RESET PASSWORD
     */
    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        if (request.token() == null
                || request.password() == null) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Token and new password are required."
                    ));
        }

        if (!request.password().matches(PASSWORD_PATTERN)) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Password must contain at least 8 characters, "
                                    + "including uppercase, lowercase, "
                                    + "a number and a special character."
                    ));
        }

        String tokenHash = sha256(request.token());

        PasswordResetToken resetToken =
                tokenRepository.findByTokenHash(tokenHash)
                        .orElse(null);

        if (resetToken == null
                || resetToken.isUsed()
                || resetToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "The reset link is invalid or has expired."
                    ));
        }

        User user = resetToken.getUser();

        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        userRepository.save(user);

        resetToken.setUsed(true);

        tokenRepository.save(resetToken);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password reset successfully. Please log in."
                )
        );
    }

    /*
     * CREATE AUTHENTICATION RESPONSE
     *
     * Generates a JWT using the existing JwtService.
     */
    private Map<String, Object> authResponse(User user) {

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities("ROLE_USER")
                        .build();

        String token =
                jwtService.generateToken(userDetails);

        Map<String, Object> response =
                new HashMap<>();

        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("user", userResponse(user));

        return response;
    }

    /*
     * SAFE USER RESPONSE
     *
     * Never return the password or password hash.
     */
    private Map<String, Object> userResponse(User user) {

        Map<String, Object> response =
                new HashMap<>();

        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());

        return response;
    }

    /*
     * SHA-256 HASHING FOR PASSWORD RESET TOKENS
     */
    private String sha256(String input) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex =
                    new StringBuilder();

            for (byte b : hash) {
                hex.append(
                        String.format("%02x", b & 0xff)
                );
            }

            return hex.toString();

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Could not generate token hash.",
                    ex
            );
        }
    }
}