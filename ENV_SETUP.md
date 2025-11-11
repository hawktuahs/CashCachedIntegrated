# Email Configuration for Magic Link

To enable magic link authentication, you need to configure email settings.

## Gmail Setup

1. Create a `.env` file in the root directory with the following content:

```properties
# Gmail Configuration
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true

# Optional: Custom from email
APP_MAIL_FROM=your-email@gmail.com
```

2. **Get Gmail App Password:**
   - Go to https://myaccount.google.com/security
   - Enable 2-Step Verification if not already enabled
   - Go to "App passwords" (search for it)
   - Generate a new app password for "Mail"
   - Use this 16-character password in `SPRING_MAIL_PASSWORD`

3. **Restart the customer service** after creating the `.env` file

## Alternative: Use Console Logging (Development Only)

If you don't want to configure email, you can modify the code to log the magic link to the console instead of sending it via email.

See the instructions below for implementing console-based magic links.
