export const BASE_CURRENCY = 'KWD'

const getCurrencyDecimals = (currency: string): number => {
  const decimals: { [key: string]: number } = {
    KWD: 3,
    JPY: 0,
    USD: 2,
    EUR: 2,
    GBP: 2,
    INR: 2,
    AED: 2,
    CAD: 2,
    CNY: 2,
    MXN: 2,
    ZAR: 2,
  }
  return decimals[currency] ?? 2
}

export const formatCurrency = (amount: number, currency: string, maximumFractionDigits?: number) => {
  const value = Number.isFinite(amount) ? amount : 0
  const decimals = maximumFractionDigits ?? getCurrencyDecimals(currency)
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value)
}

export const formatAmount = (amount: number, maximumFractionDigits = 2) => {
  const value = Number.isFinite(amount) ? amount : 0
  return value.toLocaleString(undefined, { maximumFractionDigits })
}

export const getCurrencySymbol = (currency: string) => {
  const symbols: { [key: string]: string } = {
    USD: '$',
    EUR: '€',
    GBP: '£',
    INR: '₹',
    KWD: 'KD',
    AED: 'د.إ',
    JPY: '¥',
    CNY: '¥',
  }
  return symbols[currency] || currency
}

// Legacy functions for backward compatibility
export const formatTokens = (amount: number, maximumFractionDigits = 0) => {
  return formatAmount(amount, maximumFractionDigits)
}

export const formatFiat = (amount: number, currency: string) => {
  return formatCurrency(amount, currency)
}
