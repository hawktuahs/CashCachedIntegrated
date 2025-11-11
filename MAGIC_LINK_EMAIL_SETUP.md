# Magic Link Email Configuration

## Quick Setup Guide

I've created a `.env` file in the root directory. You need to edit it with your Gmail credentials.

### Step 1: Get Gmail App Password

1. **Go to Google Account Security**: https://myaccount.google.com/security

2. **Enable 2-Step Verification** (if not already enabled):
   - Click on "2-Step Verification"
   - Follow the setup wizard
   - This is required for App Passwords

3. **Generate App Password**:
   - Search for "App passwords" in the security page
   - Or go directly to: https://myaccount.google.com/apppasswords
   - Click "Select app" → Choose "Mail"
   - Click "Select device" → Choose "Other (Custom name)"
   - Type "CashCached" as the name
   - Click "Generate"
   - **Copy the 16-character password** (format: `xxxx xxxx xxxx xxxx`)
   - **Important**: Remove the spaces when you paste it in the .env file

### Step 2: Edit the .env File

Open the `.env` file in the root directory and replace the placeholders:

```bash
# Open the file
nano .env

# Or use any text editor
code .env
```

**Replace these values:**
```properties
# Replace with your actual Gmail address
SPRING_MAIL_USERNAME=your-actual-email@gmail.com

# Replace with the 16-character app password (no spaces)
SPRING_MAIL_PASSWORD=abcdefghijklmnop

# Replace with your Gmail address (same as above)
APP_MAIL_FROM=your-actual-email@gmail.com
```

**Example of completed .env:**
```properties
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=ishaan@gmail.com
SPRING_MAIL_PASSWORD=abcdefghijklmnop
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
APP_MAIL_DEV_MODE=false
APP_MAIL_FROM=ishaan@gmail.com
```

### Step 3: Restart Customer Service

After saving the `.env` file:

```bash
# Stop the customer service
pkill -f 'customer.*spring-boot:run'

# Start it again
cd customer
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw spring-boot:run
```

### Step 4: Test Magic Link

1. Go to the login page
2. Click "Sign in with Magic Link"
3. Enter your registered email address
4. Click "Send Magic Link"
5. **Check your Gmail inbox** - you should receive an email with the magic link!
6. Click the link in the email to log in

## Troubleshooting

### "Failed to send magic link email"
- **Check your Gmail credentials** in the `.env` file
- **Verify 2-Step Verification is enabled** on your Google Account
- **Make sure you're using an App Password**, not your regular Gmail password
- **Check the app password has no spaces** when pasted in .env

### Email not received
- **Check spam folder** in Gmail
- **Verify the email address** you entered matches your registered account
- **Check customer service logs** for any error messages

### Still using console mode
- **Make sure** `APP_MAIL_DEV_MODE=false` in your `.env` file
- **Restart the customer service** after changing the .env file

## Security Notes

- ✅ The `.env` file is in `.gitignore` - it won't be committed to Git
- ✅ App Passwords are safer than using your main Gmail password
- ✅ You can revoke App Passwords anytime from Google Account settings
- ⚠️ Never share your `.env` file or commit it to version control

## Quick Commands

```bash
# Edit .env file
nano .env

# Check if .env is configured
cat .env | grep SPRING_MAIL_USERNAME

# Restart customer service
pkill -f 'customer.*spring-boot:run' && cd customer && ./mvnw spring-boot:run
```
