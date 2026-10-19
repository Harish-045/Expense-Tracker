package com.expensetracker.service;

import com.expensetracker.model.Budget;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    public boolean sendPasswordResetEmail(
            String recipient,
            String resetLink) {

        return sendHtmlEmail(
                recipient,
                "Reset your SpendWise password",
                """
                <h2>SpendWise Password Reset</h2>
                <p>We received a request to reset your password.</p>
                <p>
                    <a href="%s">Reset my password</a>
                </p>
                <p>This link expires in 30 minutes and can only be used once.</p>
                <p>If you did not request a reset, you can ignore this email.</p>
                """.formatted(resetLink)
        );
    }

    public boolean sendBudgetExceededAlert(
            String recipient,
            String category,
            int month,
            int year,
            BigDecimal spent,
            BigDecimal limit) {

        String subject =
                "SpendWise Budget Alert: " + category;

        String body = """
                <h2>Budget Limit Exceeded</h2>
                <p>Your spending has exceeded a budget in SpendWise.</p>
                <p><b>Category:</b> %s</p>
                <p><b>Period:</b> %02d/%d</p>
                <p><b>Budget:</b> ₹%s</p>
                <p><b>Total spent:</b> ₹%s</p>
                <p><b>Amount over budget:</b> ₹%s</p>
                <p>Review your expenses in SpendWise to manage your spending.</p>
                """.formatted(
                escapeHtml(category),
                month,
                year,
                limit.toPlainString(),
                spent.toPlainString(),
                spent.subtract(limit).toPlainString()
        );

        return sendHtmlEmail(recipient, subject, body);
    }

    private boolean sendHtmlEmail(
            String recipient,
            String subject,
            String htmlBody) {

        if (!mailEnabled) {
            log.warn(
                    "Email sending is disabled. Configure MAIL_ENABLED=true and SMTP credentials."
            );
            return false;
        }

        JavaMailSender sender =
                mailSenderProvider.getIfAvailable();

        if (sender == null) {
            log.error("No JavaMailSender is configured.");
            return false;
        }

        try {
            MimeMessage message = sender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient);

            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }

            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            sender.send(message);

            log.info("Email sent successfully to {}", recipient);

            return true;

        } catch (MessagingException | RuntimeException ex) {
            log.error("Failed to send email: {}", ex.getMessage());
            return false;
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}