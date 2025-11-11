USE product_db;

INSERT INTO products (product_code, product_name, product_type, description, min_interest_rate, max_interest_rate, min_term_months, max_term_months, min_amount, max_amount, currency, status, premature_penalty_rate, premature_penalty_grace_days, effective_date, compounding_frequency, requires_approval, created_at, updated_at) VALUES
('FD_1_YEAR', '1 Year Fixed Deposit', 'FIXED_DEPOSIT', 'Secure investment with guaranteed returns in 1 year', 6.50, 6.50, 12, 12, 5000.00, 1000000.00, 'INR', 'ACTIVE', 0.0100, 30, '2024-01-01', 'YEARLY', false, NOW(), NOW()),
('FD_2_YEAR', '2 Year Fixed Deposit', 'FIXED_DEPOSIT', 'Better returns with 2-year lock-in period', 7.00, 7.00, 24, 24, 5000.00, 1000000.00, 'INR', 'ACTIVE', 0.0100, 30, '2024-01-01', 'YEARLY', false, NOW(), NOW()),
('FD_3_YEAR', '3 Year Fixed Deposit', 'FIXED_DEPOSIT', 'Excellent returns for medium-term goals', 7.50, 7.50, 36, 36, 10000.00, 2000000.00, 'INR', 'ACTIVE', 0.0100, 30, '2024-01-01', 'YEARLY', false, NOW(), NOW()),
('FD_5_YEAR', '5 Year Fixed Deposit', 'FIXED_DEPOSIT', 'Maximum returns for long-term wealth building', 8.00, 8.00, 60, 60, 25000.00, 5000000.00, 'INR', 'ACTIVE', 0.0100, 30, '2024-01-01', 'YEARLY', false, NOW(), NOW()),
('SENIOR_CITIZEN', 'Senior Citizen Fixed Deposit', 'FIXED_DEPOSIT', 'Special rates for senior citizens with flexible tenure', 8.50, 8.50, 12, 60, 10000.00, 3000000.00, 'INR', 'ACTIVE', 0.0050, 30, '2024-01-01', 'QUARTERLY', false, NOW(), NOW()),
('FLEXI_FD', 'Flexible Fixed Deposit', 'FIXED_DEPOSIT', 'Flexible tenure with monthly compounding benefits', 6.80, 6.80, 6, 48, 15000.00, 1500000.00, 'INR', 'ACTIVE', 0.0100, 30, '2024-01-01', 'MONTHLY', false, NOW(), NOW()),
('CORPORATE_FD', 'Corporate Fixed Deposit', 'FIXED_DEPOSIT', 'High-value deposits for corporate clients', 7.20, 7.20, 12, 60, 100000.00, 10000000.00, 'INR', 'ACTIVE', 0.0100, 30, '2024-01-01', 'QUARTERLY', false, NOW(), NOW()),
('SAVINGS_BASIC', 'Basic Savings Account', 'SAVINGS_ACCOUNT', 'Perfect for first-time savers with competitive rates', 4.50, 4.50, 12, 60, 1000.00, 100000.00, 'INR', 'ACTIVE', 0.0050, 30, '2024-01-01', 'YEARLY', false, NOW(), NOW()),
('SAVINGS_PREMIUM', 'Premium Savings Account', 'SAVINGS_ACCOUNT', 'Higher returns for serious savers with quarterly compounding', 5.50, 5.50, 12, 60, 10000.00, 500000.00, 'INR', 'ACTIVE', 0.0050, 30, '2024-01-01', 'QUARTERLY', false, NOW(), NOW()),
('STUDENT_SAVER', 'Student Saver Account', 'SAVINGS_ACCOUNT', 'Designed for students with low minimum and flexible terms', 3.50, 3.50, 6, 36, 500.00, 50000.00, 'INR', 'ACTIVE', 0.0050, 30, '2024-01-01', 'MONTHLY', false, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
