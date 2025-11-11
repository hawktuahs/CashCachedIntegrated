# CashCached Admin Credentials

## Default Admin Account

**Email:** admin@bank.com  
**Password:** admin123

## How to Login

1. Navigate to the CashCached frontend: http://localhost:5173
2. Click on "Login" or navigate to http://localhost:5173/login
3. Enter the credentials above
4. You will have admin access to:
   - View all customer accounts
   - Create and manage products
   - Access admin dashboard
   - View system analytics

## Creating Products

Once logged in as admin:
1. Navigate to "Products & Pricing" page
2. Click "Create New Product"
3. Fill in the product details:
   - Product Code (e.g., FD_1_YEAR)
   - Product Name (e.g., 1 Year Fixed Deposit)
   - Product Type (FIXED_DEPOSIT, SAVINGS, etc.)
   - Min/Max Amount
   - Min/Max Term (in months)
   - Interest Rate
   - Compounding Frequency (MONTHLY, QUARTERLY, YEARLY)
   - Description
   - Active status

## Alternative: Use the Product Creation Script

You can also use the automated script to create 10 sample products:

```bash
node create-products.js
```

Note: This requires the backend services to be running and may need authentication token.

## Security Note

⚠️ **IMPORTANT**: These are default credentials for development/testing only. 
In production, you should:
- Change the default password immediately
- Use strong, unique passwords
- Enable multi-factor authentication
- Implement proper role-based access control
