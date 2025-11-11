import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '@/context/AuthContext'
import { formatCurrency, formatAmount } from '@/lib/currency'
import { api } from '@/lib/api'

const EXCHANGE_RATES: Record<string, number> = {
  USD: 1.0,
  EUR: 0.92,
  GBP: 0.78,
  INR: 83.20,
  KWD: 0.31,
  AED: 3.67,
  CAD: 1.36,
  JPY: 149.50,
  CNY: 7.24,
  MXN: 18.40,
  ZAR: 18.20,
}

export function useStablecoinConversion() {
  const { user, preferredCurrency } = useAuth()
  const [balance, setBalance] = useState<number>(0)
  const [isLoading, setIsLoading] = useState(false)

  // Load user's wallet balance from API
  const loadBalance = useCallback(async () => {
    if (!user?.id) {
      setBalance(0)
      return
    }
    
    setIsLoading(true)
    try {
      const response = await api.get(`/api/financials/wallet/balance/${user.id}`)
      const payload = response?.data?.data ?? response?.data
      const targetValue = Number(payload?.targetValue ?? payload?.balance ?? 0)
      const validBalance = Number.isFinite(targetValue) ? targetValue : 0
      setBalance(validBalance)
    } catch (error) {
      console.error('Failed to load balance:', error)
      setBalance(0)
    } finally {
      setIsLoading(false)
    }
  }, [user?.id, preferredCurrency])

  useEffect(() => {
    loadBalance()
  }, [loadBalance, preferredCurrency])

  // Convert between currencies
  const convertCurrency = useCallback((amount: number, from: string, to: string) => {
    const fromRate = EXCHANGE_RATES[from] || 1
    const toRate = EXCHANGE_RATES[to] || 1
    const usdAmount = amount / fromRate
    return usdAmount * toRate
  }, [])

  // Format amount in user's preferred currency with conversion
  const formatConvertedTokens = useCallback((amount: number, currency = preferredCurrency) => {
    const baseCurrency = 'KWD'
    if (currency === baseCurrency) {
      return formatCurrency(amount, currency)
    }
    const converted = convertCurrency(amount, baseCurrency, currency)
    return formatCurrency(converted, currency)
  }, [preferredCurrency, convertCurrency])

  // Format tokens (now just formats amount without symbol)
  const formatTokens = useCallback((amount: number, maximumFractionDigits = 0) => {
    return formatAmount(amount, maximumFractionDigits)
  }, [])

  // Add money to wallet
  const addMoney = useCallback(async (amount: number, currency = preferredCurrency) => {
    if (!user?.id) return false
    
    try {
      // Call API to add money
      await api.post(`/api/financials/wallet/add`, {
        customerId: user.id,
        amount: amount,
        currency: currency
      })
      
      // Reload balance from server
      await loadBalance()
      
      // Refresh wallet display
      window.dispatchEvent(
        new CustomEvent("cashcached:refresh-wallet", {
          detail: {
            customerId: user.id,
            balance: balance,
            currency: preferredCurrency,
          },
        })
      )
      
      return true
    } catch (error) {
      console.error('Add money failed:', error)
      return false
    }
  }, [user?.id, balance, preferredCurrency, loadBalance])

  // Withdraw money from wallet
  const withdrawMoney = useCallback(async (amount: number, currency = preferredCurrency) => {
    if (!user?.id) return false
    
    if (amount > balance) {
      throw new Error('Insufficient balance')
    }
    
    try {
      // Call API to withdraw money
      await api.post(`/api/financials/wallet/withdraw`, {
        customerId: user.id,
        amount: amount,
        currency: currency
      })
      
      // Reload balance from server
      await loadBalance()
      
      // Refresh wallet display
      window.dispatchEvent(
        new CustomEvent("cashcached:refresh-wallet", {
          detail: {
            customerId: user.id,
            balance: balance,
            currency: preferredCurrency,
          },
        })
      )
      
      return true
    } catch (error) {
      console.error('Withdraw failed:', error)
      return false
    }
  }, [user?.id, balance, preferredCurrency, loadBalance])

  return {
    balance,
    isLoading,
    preferredCurrency,
    convertCurrency,
    formatConvertedTokens,
    formatTokens,
    addMoney,
    withdrawMoney,
    refreshBalance: loadBalance,
  }
}
