package com.bt.accounts.service;

import com.bt.accounts.entity.FdAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountNotificationService {

    private final JavaMailSender mailSender;
    private final AccountOtpService otpService;
    private final CustomerProfileClient customerProfileClient;
    private final RestTemplate restTemplate;
    private final ServiceTokenProvider serviceTokenProvider;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromEmail;

    @Value("${app.mail.dev-mode:false}")
    private boolean devMode;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${services.customer.url:http://localhost:8081}")
    private String customerServiceBaseUrl;

    public void notifyAccountCreated(FdAccount account, String authToken) {
        try {
            CustomerProfileClient.UserProfileDto profile = customerProfileClient
                    .fetchUserProfile(parseCustomerId(account.getCustomerId()), authToken)
                    .orElse(null);
            String email = profile != null ? profile.getEmail() : null;
            String fullName = profile != null ? profile.getFullName() : null;

            if (!StringUtils.hasText(email)) {
                log.warn("Cannot send account created email - missing customer email for {}", account.getCustomerId());
                return;
            }

            String otpKey = account.getAccountNo() + ":create";
            String otp = otpService.generateAndStore(otpKey);

            String subject = "FD Account Created: " + account.getAccountNo();
            String body = buildCreatedBody(account, fullName, otp);

            sendEmail(email, subject, body);
            requestMagicLink(email);
        } catch (Exception ex) {
            log.warn("Failed to send account created notification for {}: {}", account.getAccountNo(), ex.getMessage());
        }
    }

    public void notifyAccountClosed(FdAccount account, String authToken) {
        try {
            CustomerProfileClient.UserProfileDto profile = customerProfileClient
                    .fetchUserProfile(parseCustomerId(account.getCustomerId()), authToken)
                    .orElse(null);
            String email = profile != null ? profile.getEmail() : null;
            String fullName = profile != null ? profile.getFullName() : null;

            if (!StringUtils.hasText(email)) {
                log.warn("Cannot send account closed email - missing customer email for {}", account.getCustomerId());
                return;
            }

            String otpKey = account.getAccountNo() + ":close";
            String otp = otpService.generateAndStore(otpKey);

            String subject = "FD Account Closed: " + account.getAccountNo();
            String body = buildClosedBody(account, fullName, otp);

            sendEmail(email, subject, body);
            requestMagicLink(email);
        } catch (Exception ex) {
            log.warn("Failed to send account closed notification for {}: {}", account.getAccountNo(), ex.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String body) {
        if (!StringUtils.hasText(fromEmail)) {
            log.warn("Email 'from' is not configured; skipping email to {}", to);
            return;
        }
        if (devMode) {
            log.warn("[DEV-MODE] Email to {} | Subject: {}\n{}", to, subject, body);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
        log.info("Notification email sent to {} - {}", to, subject);
    }

    private void requestMagicLink(String email) {
        try {
            String base = StringUtils.trimTrailingCharacter(customerServiceBaseUrl.trim(), '/');
            String url = base + "/api/auth/magic-link/request";
            String token = serviceTokenProvider.getBearerToken();
            var headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            var entity = new org.springframework.http.HttpEntity<>(Map.of("email", email), headers);
            restTemplate.postForEntity(url, entity, Void.class);
            log.info("Requested magic link for {}", email);
        } catch (Exception e) {
            log.warn("Failed to request magic link for {}: {}", email, e.getMessage());
        }
    }

    private String buildCreatedBody(FdAccount a, String fullName, String otp) {
        DecimalFormat df = new DecimalFormat("0.##");
        String greeting = StringUtils.hasText(fullName) ? ("Hello " + fullName + ",") : "Hello,";
        return greeting + "\n\n" +
                "Your Fixed Deposit account has been created successfully.\n" +
                "Account No: " + a.getAccountNo() + "\n" +
                "Principal: " + format(a.getPrincipalAmount()) + " tokens\n" +
                "Interest Rate: " + df.format(a.getInterestRate()) + "%\n" +
                "Tenure: " + a.getTenureMonths() + " months\n\n" +
                "OTP (valid 5 minutes): " + otp + "\n" +
                "You can also sign in using the magic link we just sent.\n\n" +
                "Regards,\nCashCached Team";
    }

    private String buildClosedBody(FdAccount a, String fullName, String otp) {
        String greeting = StringUtils.hasText(fullName) ? ("Hello " + fullName + ",") : "Hello,";
        return greeting + "\n\n" +
                "Your Fixed Deposit account has been closed.\n" +
                "Account No: " + a.getAccountNo() + "\n" +
                "Closure Time: " + (a.getClosedAt() != null ? a.getClosedAt() : "now") + "\n\n" +
                "OTP (valid 5 minutes): " + otp + "\n" +
                "If you didn't request this, please contact support immediately.\n\n" +
                "Regards,\nCashCached Team";
    }

    private String format(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private Long parseCustomerId(String s) {
        try {
            return Long.parseLong(String.valueOf(s).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
