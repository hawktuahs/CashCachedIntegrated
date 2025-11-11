#!/bin/bash

API_BASE="http://localhost:8080"
ADMIN_TOKEN=""

echo "=========================================="
echo "TIME TRAVEL TESTING DEMONSTRATION"
echo "Testing FD Account Maturity & Batch Processing"
echo "=========================================="
echo ""

echo "Step 1: Login as Admin to get token"
echo "------------------------------------"
LOGIN_RESPONSE=$(curl -s -X POST "$API_BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@bank.com",
    "password": "admin123"
  }')

ADMIN_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token // .token // empty')

if [ -z "$ADMIN_TOKEN" ]; then
  echo "❌ Failed to get admin token. Please check credentials."
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi

echo "✅ Admin token obtained"
echo ""

echo "Step 2: Get Current System Time"
echo "--------------------------------"
CURRENT_TIME=$(curl -s -X GET "$API_BASE/api/accounts/admin/time" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data.systemTime')

echo "Current System Time: $CURRENT_TIME"
OFFSET=$(curl -s -X GET "$API_BASE/api/accounts/admin/time" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data.offsetSeconds')
echo "Current Offset: ${OFFSET}s"
echo ""

echo "Step 3: Create a Test FD Account (2-year tenure)"
echo "------------------------------------------------"
CUSTOMER_ID=$(curl -s -X GET "$API_BASE/api/customers/all" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data[0].customerId // .data[0].id // empty')

if [ -z "$CUSTOMER_ID" ]; then
  echo "❌ No customers found. Please create a customer first."
  exit 1
fi

echo "Using Customer ID: $CUSTOMER_ID"

PRODUCT_CODE=$(curl -s -X GET "$API_BASE/api/products/all" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data[0].productCode // empty')

if [ -z "$PRODUCT_CODE" ]; then
  echo "❌ No products found. Please create products first."
  exit 1
fi

echo "Using Product Code: $PRODUCT_CODE"

ACCOUNT_RESPONSE=$(curl -s -X POST "$API_BASE/api/accounts/create" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"productCode\": \"$PRODUCT_CODE\",
    \"principalAmount\": 100000,
    \"tenureMonths\": 24,
    \"interestRate\": 7.5
  }")

ACCOUNT_NO=$(echo $ACCOUNT_RESPONSE | jq -r '.data.accountNo // .accountNo // empty')

if [ -z "$ACCOUNT_NO" ]; then
  echo "❌ Failed to create account"
  echo "Response: $ACCOUNT_RESPONSE"
  exit 1
fi

echo "✅ Created FD Account: $ACCOUNT_NO"
echo "   Principal: ₹100,000"
echo "   Tenure: 24 months (2 years)"
echo "   Interest Rate: 7.5%"
echo ""

echo "Step 4: Check Initial Account Status"
echo "------------------------------------"
ACCOUNT_DETAILS=$(curl -s -X GET "$API_BASE/api/accounts/$ACCOUNT_NO" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

STATUS=$(echo $ACCOUNT_DETAILS | jq -r '.data.status // .status')
CREATED_AT=$(echo $ACCOUNT_DETAILS | jq -r '.data.createdAt // .createdAt')
MATURITY_DATE=$(echo $ACCOUNT_DETAILS | jq -r '.data.maturityDate // .maturityDate')
NEXT_ACCRUAL=$(echo $ACCOUNT_DETAILS | jq -r '.data.nextInterestAccrualAt // .nextInterestAccrualAt')

echo "Account Status: $STATUS"
echo "Created At: $CREATED_AT"
echo "Maturity Date: $MATURITY_DATE"
echo "Next Interest Accrual: $NEXT_ACCRUAL"
echo ""

echo "Step 5: Time Travel - Advance 1 Year (for first interest accrual)"
echo "------------------------------------------------------------------"
ONE_YEAR_SECONDS=31536000
curl -s -X POST "$API_BASE/api/accounts/admin/time/advance" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"seconds\": $ONE_YEAR_SECONDS}" > /dev/null

NEW_TIME=$(curl -s -X GET "$API_BASE/api/accounts/admin/time" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data.systemTime')

echo "✅ Advanced time by 1 year"
echo "New System Time: $NEW_TIME"
echo ""

echo "Waiting 65 seconds for batch processing to run..."
sleep 65

echo ""
echo "Step 6: Check Account After 1 Year (First Interest Accrual)"
echo "-----------------------------------------------------------"
ACCOUNT_AFTER_1Y=$(curl -s -X GET "$API_BASE/api/accounts/$ACCOUNT_NO" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

INTEREST_ACCRUED=$(echo $ACCOUNT_AFTER_1Y | jq -r '.data.totalInterestAccrued // .totalInterestAccrued')
LAST_ACCRUAL=$(echo $ACCOUNT_AFTER_1Y | jq -r '.data.lastInterestAccrualAt // .lastInterestAccrualAt')
BALANCE=$(echo $ACCOUNT_AFTER_1Y | jq -r '.data.currentBalance // .currentBalance')

echo "Total Interest Accrued: ₹$INTEREST_ACCRUED"
echo "Last Accrual At: $LAST_ACCRUAL"
echo "Current Balance: ₹$BALANCE"
echo ""

TRANSACTIONS=$(curl -s -X GET "$API_BASE/api/accounts/$ACCOUNT_NO/transactions" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Recent Transactions:"
echo $TRANSACTIONS | jq -r '.data[] | "  - \(.transactionType): ₹\(.amount) on \(.transactionDate)"' | head -3
echo ""

echo "Step 7: Time Travel - Advance Another 1 Year (to maturity)"
echo "-----------------------------------------------------------"
curl -s -X POST "$API_BASE/api/accounts/admin/time/advance" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"seconds\": $ONE_YEAR_SECONDS}" > /dev/null

MATURITY_TIME=$(curl -s -X GET "$API_BASE/api/accounts/admin/time" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data.systemTime')

echo "✅ Advanced time by another 1 year (total 2 years)"
echo "System Time at Maturity: $MATURITY_TIME"
echo ""

echo "Waiting 65 seconds for batch processing to run maturity..."
sleep 65

echo ""
echo "Step 8: Check Account After Maturity"
echo "------------------------------------"
ACCOUNT_MATURED=$(curl -s -X GET "$API_BASE/api/accounts/$ACCOUNT_NO" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

FINAL_STATUS=$(echo $ACCOUNT_MATURED | jq -r '.data.status // .status')
FINAL_INTEREST=$(echo $ACCOUNT_MATURED | jq -r '.data.totalInterestAccrued // .totalInterestAccrued')
CLOSED_AT=$(echo $ACCOUNT_MATURED | jq -r '.data.closedAt // .closedAt')

echo "Final Account Status: $FINAL_STATUS"
echo "Total Interest Earned: ₹$FINAL_INTEREST"
echo "Closed At: $CLOSED_AT"
echo ""

FINAL_TRANSACTIONS=$(curl -s -X GET "$API_BASE/api/accounts/$ACCOUNT_NO/transactions" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "All Transactions:"
echo $FINAL_TRANSACTIONS | jq -r '.data[] | "  - \(.transactionType): ₹\(.amount) on \(.transactionDate)"'
echo ""

echo "Step 9: Reset Time to Present"
echo "-----------------------------"
curl -s -X POST "$API_BASE/api/accounts/admin/time/reset" \
  -H "Authorization: Bearer $ADMIN_TOKEN" > /dev/null

RESET_TIME=$(curl -s -X GET "$API_BASE/api/accounts/admin/time" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data.systemTime')
RESET_OFFSET=$(curl -s -X GET "$API_BASE/api/accounts/admin/time" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data.offsetSeconds')

echo "✅ Time reset to present"
echo "Current System Time: $RESET_TIME"
echo "Current Offset: ${RESET_OFFSET}s"
echo ""

echo "=========================================="
echo "TIME TRAVEL TEST COMPLETED"
echo "=========================================="
echo ""
echo "Summary:"
echo "--------"
echo "1. ✅ Created FD account with ₹100,000 principal"
echo "2. ✅ Advanced 1 year → First interest accrual processed"
echo "3. ✅ Advanced another 1 year → Account matured automatically"
echo "4. ✅ Maturity payout processed by batch job"
echo "5. ✅ Time reset to present"
echo ""
echo "The account is now in '$FINAL_STATUS' status with ₹$FINAL_INTEREST total interest earned."
