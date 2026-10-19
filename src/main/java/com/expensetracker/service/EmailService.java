package com.expensetracker.service;

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

    /**
     * Sends a password-reset email.
     */
    public boolean sendPasswordResetEmail(
            String recipient,
            String resetLink) {

        String body = """
                <html>
                <body style="font-family:Arial,sans-serif;color:#333;">
                    <h2>SpendWise Password Reset</h2>
                    <p>We received a request to reset your password.</p>
                    <p>
                        <a href="%s"
                           style="background:#2563eb;color:white;
                                  padding:12px 20px;text-decoration:none;
                                  border-radius:6px;display:inline-block;">
                            Reset my password
                        </a>
                    </p>
                    <p>This link expires in 30 minutes and can only be used once.</p>
                    <p>If you did not request a reset, you can ignore this email.</p>
                    <p>Regards,<br>SpendWise Team</p>
                </body>
                </html>
                """.formatted(escapeHtml(resetLink));

        return sendHtmlEmail(
                recipient,
                "Reset your SpendWise password",
                body
        );
    }

    /**
     * Sends a warning when spending reaches or exceeds 80% of the budget.
     */
    public boolean sendBudgetExceededAlert(
            String recipient,
            String category,
            int month,
            int year,
            BigDecimal spent,
            BigDecimal limit) {

        if (spent == null || limit == null
                || limit.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Cannot send budget alert: invalid spending or budget.");
            return false;
        }

        BigDecimal remaining = limit.subtract(spent);

        BigDecimal percentage = spent
                .multiply(new BigDecimal("100"))
                .divide(limit, 2, java.math.RoundingMode.HALF_UP);

        String subject =
                "SpendWise Alert: " + category + " budget at "
                        + percentage.stripTrailingZeros().toPlainString() + "%";

        String statusMessage;
        String statusColor;

        if (spent.compareTo(limit) >= 0) {
            statusMessage = "Your spending has reached or exceeded your budget.";
            statusColor = "#dc2626";
        } else {
            statusMessage = "Your spending has reached 80% of your budget.";
            statusColor = "#d97706";
        }

        String remainingMessage;

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            remainingMessage =
                    "You have ₹" + remaining.toPlainString()
                            + " remaining in this budget.";
        } else if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            remainingMessage = "You have used your entire budget.";
        } else {
            remainingMessage =
                    "You have exceeded your budget by ₹"
                            + remaining.abs().toPlainString() + ".";
        }

        String body = """
                <html>
                <body style="font-family:Arial,sans-serif;color:#333;
                             line-height:1.6;max-width:600px;margin:auto;">

                    <div style="background:#111827;color:white;
                                padding:20px;border-radius:10px 10px 0 0;">
                        <h2 style="margin:0;">SpendWise Budget Alert</h2>
                    </div>

                    <div style="padding:24px;border:1px solid #e5e7eb;
                                border-radius:0 0 10px 10px;">

                        <h3 style="color:%s;">%s</h3>

                        <p>Your expense tracker has detected that your
                        spending is approaching or has reached your
                        budget limit.</p>

                        <table style="width:100%%;border-collapse:collapse;">
                            <tr>
                                <td style="padding:10px;border-bottom:1px solid #eee;">
                                    Category
                                </td>
                                <td style="padding:10px;border-bottom:1px solid #eee;">
                                    <b>%s</b>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:10px;border-bottom:1px solid #eee;">
                                    Budget period
                                </td>
                                <td style="padding:10px;border-bottom:1px solid #eee;">
                                    <b>%02d/%d</b>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:10px;border-bottom:1px solid #eee;">
                                    Monthly budget
                                </td>
                                <td style="padding:10px;border-bottom:1px solid #eee;">
                                    <b>₹%s</b>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:10px;border-bottom:1px solid #eee;">
                                    Total spent
                                </td>
                                <td style="padding:10px;border-bottom:1px solid #eee;">
                                    <b>₹%s</b>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:10px;border-bottom:1px solid #eee;">
                                    Budget used
                                </td>
                                <td style="padding:10px;border-bottom:1px solid #eee;">
                                    <b>%s%%</b>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:10px;">
                                    Remaining budget
                                </td>
                                <td style="padding:10px;">
                                    <b>%s</b>
                                </td>
                            </tr>
                        </table>

                        <p>%s</p>

                        <p>Please review your expenses in SpendWise
                        to help manage your spending.</p>

                        <p style="margin-top:24px;">
                            Regards,<br>
                            <b>SpendWise AI Expense Tracker</b>
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(
                statusColor,
                escapeHtml(statusMessage),
                escapeHtml(category),
                month,
                year,
                limit.toPlainString(),
                spent.toPlainString(),
                percentage.stripTrailingZeros().toPlainString(),
                escapeHtml(remainingMessage),
                escapeHtml(remainingMessage)
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

        if (recipient == null || recipient.isBlank()) {
            log.warn("Email not sent because the recipient address is empty.");
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

            helper.setTo(recipient.trim());

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