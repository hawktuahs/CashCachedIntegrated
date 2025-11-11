const axios = require('axios');

// API configuration
const API_BASE_URL = 'http://localhost:8080/api';

// Sample products to create
const products = [
  {
    productCode: 'SAVINGS_BASIC',
    productName: 'Basic Savings Account',
    productType: 'SAVINGS',
    minAmount: 1000,
    maxAmount: 100000,
    minTermMonths: 12,
    maxTermMonths: 60,
    interestRate: 4.5,
    compoundingFrequency: 'YEARLY',
    description: 'Perfect for first-time savers with competitive rates',
    isActive: true
  },
  {
    productCode: 'SAVINGS_PREMIUM',
    productName: 'Premium Savings Account',
    productType: 'SAVINGS',
    minAmount: 10000,
    maxAmount: 500000,
    minTermMonths: 12,
    maxTermMonths: 60,
    interestRate: 5.5,
    compoundingFrequency: 'QUARTERLY',
    description: 'Higher returns for serious savers with quarterly compounding',
    isActive: true
  },
  {
    productCode: 'FD_1_YEAR',
    productName: '1 Year Fixed Deposit',
    productType: 'FIXED_DEPOSIT',
    minAmount: 5000,
    maxAmount: 1000000,
    minTermMonths: 12,
    maxTermMonths: 12,
    interestRate: 6.5,
    compoundingFrequency: 'YEARLY',
    description: 'Secure investment with guaranteed returns in 1 year',
    isActive: true
  },
  {
    productCode: 'FD_2_YEAR',
    productName: '2 Year Fixed Deposit',
    productType: 'FIXED_DEPOSIT',
    minAmount: 5000,
    maxAmount: 1000000,
    minTermMonths: 24,
    maxTermMonths: 24,
    interestRate: 7.0,
    compoundingFrequency: 'YEARLY',
    description: 'Better returns with 2-year lock-in period',
    isActive: true
  },
  {
    productCode: 'FD_3_YEAR',
    productName: '3 Year Fixed Deposit',
    productType: 'FIXED_DEPOSIT',
    minAmount: 10000,
    maxAmount: 2000000,
    minTermMonths: 36,
    maxTermMonths: 36,
    interestRate: 7.5,
    compoundingFrequency: 'YEARLY',
    description: 'Excellent returns for medium-term goals',
    isActive: true
  },
  {
    productCode: 'FD_5_YEAR',
    productName: '5 Year Fixed Deposit',
    productType: 'FIXED_DEPOSIT',
    minAmount: 25000,
    maxAmount: 5000000,
    minTermMonths: 60,
    maxTermMonths: 60,
    interestRate: 8.0,
    compoundingFrequency: 'YEARLY',
    description: 'Maximum returns for long-term wealth building',
    isActive: true
  },
  {
    productCode: 'SENIOR_CITIZEN',
    productName: 'Senior Citizen Fixed Deposit',
    productType: 'FIXED_DEPOSIT',
    minAmount: 10000,
    maxAmount: 3000000,
    minTermMonths: 12,
    maxTermMonths: 60,
    interestRate: 8.5,
    compoundingFrequency: 'QUARTERLY',
    description: 'Special rates for senior citizens with flexible tenure',
    isActive: true
  },
  {
    productCode: 'STUDENT_SAVER',
    productName: 'Student Saver Account',
    productType: 'SAVINGS',
    minAmount: 500,
    maxAmount: 50000,
    minTermMonths: 6,
    maxTermMonths: 36,
    interestRate: 3.5,
    compoundingFrequency: 'MONTHLY',
    description: 'Designed for students with low minimum and flexible terms',
    isActive: true
  },
  {
    productCode: 'CORPORATE_FD',
    productName: 'Corporate Fixed Deposit',
    productType: 'FIXED_DEPOSIT',
    minAmount: 100000,
    maxAmount: 10000000,
    minTermMonths: 12,
    maxTermMonths: 60,
    interestRate: 7.2,
    compoundingFrequency: 'QUARTERLY',
    description: 'High-value deposits for corporate clients',
    isActive: true
  },
  {
    productCode: 'FLEXI_FD',
    productName: 'Flexible Fixed Deposit',
    productType: 'FIXED_DEPOSIT',
    minAmount: 15000,
    maxAmount: 1500000,
    minTermMonths: 6,
    maxTermMonths: 48,
    interestRate: 6.8,
    compoundingFrequency: 'MONTHLY',
    description: 'Flexible tenure with monthly compounding benefits',
    isActive: true
  }
];

// Function to create a product
async function createProduct(product) {
  try {
    const response = await axios.post(`${API_BASE_URL}/v1/product`, product, {
      headers: {
        'Content-Type': 'application/json',
      },
    });
    console.log(`✅ Created: ${product.productName} (${product.productCode})`);
    return response.data;
  } catch (error) {
    if (error.response?.status === 409) {
      console.log(`⚠️  Already exists: ${product.productName} (${product.productCode})`);
    } else {
      console.error(`❌ Failed to create ${product.productName}:`, error.response?.data || error.message);
    }
    return null;
  }
}

// Main function to create all products
async function createAllProducts() {
  console.log('🚀 Starting to create 10 CashCached products...\n');
  
  let successCount = 0;
  let skipCount = 0;
  let failCount = 0;

  for (const product of products) {
    const result = await createProduct(product);
    if (result) {
      successCount++;
    } else {
      skipCount++;
    }
    // Small delay to avoid overwhelming the server
    await new Promise(resolve => setTimeout(resolve, 100));
  }

  console.log('\n📊 Summary:');
  console.log(`✅ Successfully created: ${successCount} products`);
  console.log(`⚠️  Already existed: ${skipCount} products`);
  console.log(`❌ Failed to create: ${failCount} products`);
  console.log('\n🎉 Products are ready for customers to subscribe!');
}

// Check if server is running before creating products
async function checkServer() {
  try {
    await axios.get(`${API_BASE_URL}/v1/product`);
    return true;
  } catch (error) {
    // Try without auth - 403 means server is running
    if (error.response?.status === 403) {
      console.log('⚠️  Server is running but requires authentication.');
      console.log('📝 Products will be created anyway (may require admin login).\n');
      return true;
    }
    console.log('❌ Server not reachable. Please ensure all services are running:');
    console.log('   - Main Gateway: http://localhost:8080');
    console.log('   - Product Service: http://localhost:8082');
    return false;
  }
}

// Run the script
async function main() {
  const serverRunning = await checkServer();
  if (serverRunning) {
    await createAllProducts();
  }
}

main().catch(console.error);
