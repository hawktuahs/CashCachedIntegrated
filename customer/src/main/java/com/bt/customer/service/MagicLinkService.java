package com.bt.customer.service;

import com.bt.customer.dto.AuthResponse;
import com.bt.customer.entity.User;
import com.bt.customer.exception.InvalidCredentialsException;
import com.bt.customer.exception.MagicLinkException;
import com.bt.customer.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MagicLinkService {

    private static final String MAGIC_LINK_PREFIX = "magic_link:";
    private static final long MAGIC_LINK_VALIDITY_MINUTES = 15;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RedisSessionService redisSessionService;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail.dev-mode:true}")
    private boolean devMode;

    public void sendMagicLink(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MagicLinkException("No user with this email"));

        String token = UUID.randomUUID().toString();
        String magicLinkKey = MAGIC_LINK_PREFIX + token;

        redisTemplate.opsForValue().set(magicLinkKey, email, MAGIC_LINK_VALIDITY_MINUTES, TimeUnit.MINUTES);

        String magicLink = buildMagicLinkUrl(token);

        if (devMode) {
            log.warn("\n\n" +
                    "===========================================\n" +
                    "DEVELOPMENT MODE: Magic Link Generated\n" +
                    "===========================================\n" +
                    "User: {} ({})\n" +
                    "Magic Link: {}\n" +
                    "Valid for: {} minutes\n" +
                    "===========================================\n",
                    user.getFullName(), email, magicLink, MAGIC_LINK_VALIDITY_MINUTES);
        } else {
            sendMagicLinkEmail(email, magicLink, user.getFullName());
        }
    }

    public String verifyMagicLink(String token) {
        String magicLinkKey = MAGIC_LINK_PREFIX + token;
        String email = redisTemplate.opsForValue().get(magicLinkKey);

        log.info("Attempting to verify magic link token: {}, key: {}, found email: {}", token, magicLinkKey, email);

        if (email == null) {
            log.warn("Magic link token not found or expired: {}", token);
            throw new InvalidCredentialsException("Invalid or expired magic link");
        }

        redisTemplate.delete(magicLinkKey);
        log.info("Magic link token verified and deleted for email: {}", email);
        return email;
    }

    public String authenticateWithMagicLink(String token) {
        String email = verifyMagicLink(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!user.getActive()) {
            throw new InvalidCredentialsException("User account is inactive");
        }

        String sessionId = redisSessionService.createSession(user);
        return sessionId;
    }

    public AuthResponse verifyAndAuthenticateWithMagicLink(String token) {
        String email = verifyMagicLink(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!user.getActive()) {
            throw new InvalidCredentialsException("User account is inactive");
        }

        String sessionId = redisSessionService.createSession(user);
        return new AuthResponse(sessionId, user.getUsername(), email, user.getRole().name(),
                "Authentication successful");
    }

    private String buildMagicLinkUrl(String token) {
        String effectiveFrontendUrl = resolveFrontendUrl();
        String normalizedBase = effectiveFrontendUrl.endsWith("/")
                ? effectiveFrontendUrl.substring(0, effectiveFrontendUrl.length() - 1)
                : effectiveFrontendUrl;
        return normalizedBase + "/auth/magic-link?token=" + token;
    }

    private String resolveFrontendUrl() {
        String envOverride = System.getenv("APP_FRONTEND_URL");
        if (envOverride != null && !envOverride.isBlank()) {
            return envOverride.trim();
        }
        return frontendUrl;
    }

    private void sendMagicLinkEmail(String to, String magicLink, String fullName) {
        try {
            if (fromEmail == null || fromEmail.isEmpty()) {
                throw new MagicLinkException("Email configuration is missing. Please configure SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD in environment variables.");
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Your Magic Link - Sign In to CashCached");
            message.setText(buildMagicLinkEmailBody(magicLink, fullName));
            mailSender.send(message);
            log.info("Magic link email sent successfully to {}", to);
        } catch (MagicLinkException e) {
            log.error("Email configuration error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to send magic link email to {}", to, e);
            throw new MagicLinkException("Failed to send magic link email. Please check email configuration.", e);
        }
    }

    private String buildMagicLinkEmailBody(String magicLink, String fullName) {
        return "Hello " + fullName + ",\n\n" +
                "Click the link below to sign in to your CashCached account. This link will expire in 15 minutes.\n\n" +
                magicLink + "\n\n" +
                "If you didn't request this link, please ignore this email.\n\n" +
                "Best regards,\nCashCached Team";
    }
}
