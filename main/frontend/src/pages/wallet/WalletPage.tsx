import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Coins,
  ArrowDownToLine,
  ArrowUpFromLine,
  Wallet,
  CreditCard,
  Activity,
  RefreshCw,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useI18n } from "@/context/I18nContext";
import { api } from "@/lib/api";
import { toast } from "sonner";
import { formatCurrency } from "@/lib/currency";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

interface Transaction {
  id: string;
  type: string;
  amount: number;
  description: string;
  timestamp: string;
  balanceAfter: number;
}

export function WalletPage() {
  const { user, preferredCurrency } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();

  const [walletBalance, setWalletBalance] = useState<number>(0);
  const [baseCurrency, setBaseCurrency] = useState<string>("INR");
  const [isLoading, setIsLoading] = useState(false);
  const [addAmount, setAddAmount] = useState<string>("");
  const [withdrawAmount, setWithdrawAmount] = useState<string>("");
  const [isProcessing, setIsProcessing] = useState(false);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [isLoadingTransactions, setIsLoadingTransactions] = useState(false);


  const parseAmount = (value: string) => {
    if (!value) return 0;
    const normalized = value.trim().replace(/,/g, '');
    if (!normalized) return 0;
    const num = parseFloat(normalized);
    if (!Number.isFinite(num) || num <= 0) return 0;
    return num;
  };

  const convertCurrency = (amount: number, fromCurrency: string, toCurrency: string) => {
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
    };

    const from = fromCurrency.toUpperCase();
    const to = toCurrency.toUpperCase();
    
    if (from === to) return amount;
    
    const fromRate = EXCHANGE_RATES[from] || 1;
    const toRate = EXCHANGE_RATES[to] || 1;
    
    const usdAmount = amount / fromRate;
    return usdAmount * toRate;
  };

  const fetchWalletBalance = async () => {
    if (!user?.id) return;
    setIsLoading(true);
    try {
      const response = await api.get(
        `/api/financials/wallet/balance/${user.id}`
      );
      const payload = response?.data?.data ?? response?.data;
      const targetValue = Number(payload?.targetValue ?? payload?.balance ?? 0);
      const displayCurrency = payload?.targetCurrency || payload?.baseCurrency || "INR";
      setWalletBalance(Number.isFinite(targetValue) ? targetValue : 0);
      setBaseCurrency(displayCurrency);
    } catch (error) {
      console.error("Failed to load wallet balance", error);
      toast.error("Unable to load wallet balance");
    } finally {
      setIsLoading(false);
    }
  };

  const fetchTransactions = async () => {
    if (!user?.id) return;
    setIsLoadingTransactions(true);
    try {
      const response = await api.get(
        `/api/financials/wallet/transactions/${user.id}`
      );
      const list = response?.data?.data ?? response?.data ?? [];
      const mapped = (Array.isArray(list) ? list : []).map((item: any) => {
        const changeAmount = Number(item.changeAmount) || 0;
        const balanceAfter = Number(item.balanceAfter) || 0;
        
        const convertedAmount = convertCurrency(changeAmount, baseCurrency, preferredCurrency);
        const convertedBalance = convertCurrency(balanceAfter, baseCurrency, preferredCurrency);
        
        return {
          id: item.id?.toString() || '',
          type: changeAmount >= 0 ? 'CREDIT' : 'DEBIT',
          amount: convertedAmount,
          description: item.reference || 'Transaction',
          timestamp: item.createdAt || new Date().toISOString(),
          balanceAfter: convertedBalance,
        };
      });
      setTransactions(mapped);
    } catch (error) {
      console.error("Failed to load transactions", error);
    } finally {
      setIsLoadingTransactions(false);
    }
  };

  useEffect(() => {
    fetchWalletBalance();
    fetchTransactions();
  }, [user?.id, preferredCurrency]);

  const handleAddMoney = async () => {
    const amount = parseAmount(addAmount);
    console.log("Add money - input:", addAmount, "parsed:", amount);
    if (!amount || amount <= 0 || !user?.id) {
      toast.error("Please enter a valid amount");
      return;
    }

    setIsProcessing(true);
    try {
      await api.post(`/api/financials/wallet/add`, {
        customerId: user.id,
        amount: amount,
        currency: preferredCurrency
      });
      
      toast.success(`Successfully added ${formatCurrency(amount, preferredCurrency)} to your wallet`);
      setAddAmount("");
      await fetchWalletBalance();
      await fetchTransactions();
    } catch (error: any) {
      console.error("Add money failed:", error);
      const errorMsg = error.response?.data?.message || error.message || "Failed to add money to wallet";
      toast.error(errorMsg);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleWithdraw = async () => {
    const amount = parseAmount(withdrawAmount);
    if (!amount || !user?.id) {
      toast.error("Please enter a valid amount");
      return;
    }

    if (amount > walletBalance) {
      toast.error("Insufficient balance");
      return;
    }

    setIsProcessing(true);
    try {
      await api.post(`/api/financials/wallet/withdraw`, {
        customerId: user.id,
        amount: amount,
        currency: preferredCurrency
      });
      
      toast.success(`Successfully withdrew ${formatCurrency(amount, preferredCurrency)} from your wallet`);
      setWithdrawAmount("");
      await fetchWalletBalance();
      await fetchTransactions();
    } catch (error: any) {
      console.error("Withdraw failed:", error);
      toast.error(error.message || "Failed to withdraw money");
    } finally {
      setIsProcessing(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">{t("wallet.title")}</h1>
        <div className="grid gap-6 md:grid-cols-2">
          <Card>
            <CardContent className="pt-6">
              <div className="animate-pulse space-y-4">
                <div className="h-8 bg-gray-200 rounded w-1/2"></div>
                <div className="h-12 bg-gray-200 rounded w-3/4"></div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
            <Wallet className="h-8 w-8" />
            {t("wallet.title")}
          </h1>
          <p className="text-muted-foreground">
            {t("wallet.subtitle")}
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            fetchWalletBalance();
            fetchTransactions();
          }}
        >
          <RefreshCw className="h-4 w-4 mr-2" />
          {t("action.refresh")}
        </Button>
      </div>

      {/* Wallet Balance Card */}
      <Card className="border-primary/20">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Coins className="h-5 w-5" />
            {t("wallet.balance.title")}
          </CardTitle>
          <CardDescription>{t("wallet.balance.your")}</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            <div className="text-4xl font-bold">
              {formatCurrency(walletBalance, preferredCurrency)}
            </div>
            <div className="text-xl text-muted-foreground">
              {t("wallet.balance.available")}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Add/Withdraw Money Tabs */}
      <Tabs defaultValue="add" className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="add">{t("wallet.tabs.add")}</TabsTrigger>
          <TabsTrigger value="withdraw">{t("wallet.tabs.withdraw")}</TabsTrigger>
        </TabsList>

        <TabsContent value="add" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ArrowDownToLine className="h-5 w-5" />
                {t("wallet.add.title")}
              </CardTitle>
              <CardDescription>
                {t("wallet.add.subtitle")}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="add-amount">{t("wallet.add.amount")} ({preferredCurrency})</Label>
                <Input
                  id="add-amount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  placeholder={t("wallet.add.placeholder")}
                  value={addAmount}
                  onChange={(e) => setAddAmount(e.target.value)}
                  disabled={isProcessing}
                />
                {parseAmount(addAmount) > 0 && (
                  <p className="text-sm text-muted-foreground">
                    {t("wallet.add.willBeAdded")}: {formatCurrency(parseAmount(addAmount), preferredCurrency)}
                  </p>
                )}
              </div>

              <div className="grid grid-cols-3 gap-2">
                {[5000, 10000, 50000].map((amount) => (
                  <Button
                    key={amount}
                    variant="outline"
                    size="sm"
                    onClick={() => setAddAmount(String(amount))}
                    disabled={isProcessing}
                  >
                    {formatCurrency(amount, preferredCurrency)}
                  </Button>
                ))}
              </div>

              <Button
                className="w-full"
                onClick={handleAddMoney}
                disabled={isProcessing || parseAmount(addAmount) <= 0}
              >
                <CreditCard className="h-4 w-4 mr-2" />
                {isProcessing ? t("wallet.add.processing") : t("wallet.add.button")}
              </Button>

              <p className="text-xs text-muted-foreground text-center">
                {t("wallet.add.instant")}
              </p>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="withdraw" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ArrowUpFromLine className="h-5 w-5" />
                {t("wallet.withdraw.title")}
              </CardTitle>
              <CardDescription>
                {t("wallet.withdraw.subtitle")}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="withdraw-amount">{t("wallet.withdraw.amount")} ({preferredCurrency})</Label>
                <Input
                  id="withdraw-amount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  max={walletBalance}
                  placeholder={t("wallet.withdraw.placeholder")}
                  value={withdrawAmount}
                  onChange={(e) => setWithdrawAmount(e.target.value)}
                  disabled={isProcessing}
                />
                {parseAmount(withdrawAmount) > 0 && (
                  <p className="text-sm text-muted-foreground">
                    {t("wallet.withdraw.willBeWithdrawn")}: {formatCurrency(parseAmount(withdrawAmount), preferredCurrency)}
                  </p>
                )}
              </div>

              <div className="grid grid-cols-3 gap-2">
                {[5000, 10000, walletBalance].map((amount, idx) => (
                  <Button
                    key={idx}
                    variant="outline"
                    size="sm"
                    onClick={() => setWithdrawAmount(String(Math.floor(amount)))}
                    disabled={isProcessing || amount <= 0}
                  >
                    {idx === 2 ? t("wallet.withdraw.all") : formatCurrency(amount, preferredCurrency)}
                  </Button>
                ))}
              </div>

              <Button
                className="w-full"
                variant="destructive"
                onClick={handleWithdraw}
                disabled={
                  isProcessing ||
                  parseAmount(withdrawAmount) <= 0 ||
                  parseAmount(withdrawAmount) > walletBalance
                }
              >
                <ArrowUpFromLine className="h-4 w-4 mr-2" />
                {isProcessing ? t("wallet.add.processing") : t("wallet.withdraw.button")}
              </Button>

              <p className="text-xs text-muted-foreground text-center">
                {t("wallet.withdraw.availableBalance")}: {formatCurrency(walletBalance, preferredCurrency)}
              </p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Transaction History */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5" />
            {t("wallet.transactions.title")}
          </CardTitle>
          <CardDescription>{t("wallet.transactions.subtitle")}</CardDescription>
        </CardHeader>
        <CardContent>
          {isLoadingTransactions ? (
            <div className="text-center py-8">
              <RefreshCw className="h-8 w-8 animate-spin mx-auto text-muted-foreground" />
            </div>
          ) : transactions.length === 0 ? (
            <div className="text-center py-8">
              <Activity className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
              <p className="text-muted-foreground">{t("wallet.transactions.empty")}</p>
            </div>
          ) : (
            <div className="space-y-3">
              {transactions.slice(0, 10).map((txn) => (
                <div
                  key={txn.id}
                  className="flex items-center justify-between p-4 border rounded-lg hover:bg-muted/50 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={`p-2 rounded-full ${
                        txn.type === "CREDIT"
                          ? "bg-green-100 text-green-600"
                          : "bg-red-100 text-red-600"
                      }`}
                    >
                      {txn.type === "CREDIT" ? (
                        <ArrowDownToLine className="h-4 w-4" />
                      ) : (
                        <ArrowUpFromLine className="h-4 w-4" />
                      )}
                    </div>
                    <div>
                      <p className="font-medium">{txn.description}</p>
                      <p className="text-xs text-muted-foreground">
                        {new Date(txn.timestamp).toLocaleString()}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p
                      className={`font-semibold ${
                        txn.type === "CREDIT"
                          ? "text-green-600"
                          : "text-red-600"
                      }`}
                    >
                      {txn.type === "CREDIT" ? "+" : "-"}
                      {formatCurrency(Math.abs(txn.amount), preferredCurrency)}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {t("wallet.transactions.balance")}: {formatCurrency(txn.balanceAfter, preferredCurrency)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>{t("wallet.quickActions.title")}</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3 sm:grid-cols-2">
          <Button
            variant="outline"
            className="w-full"
            onClick={() => navigate("/accounts")}
          >
            <CreditCard className="h-4 w-4 mr-2" />
            {t("wallet.quickActions.viewAccounts")}
          </Button>
          <Button
            variant="outline"
            className="w-full"
            onClick={() => navigate("/fd-calculator")}
          >
            <Activity className="h-4 w-4 mr-2" />
            {t("wallet.quickActions.fdCalculator")}
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
