package com.bt.accounts.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bt.accounts.dto.ApiResponse;
import com.bt.accounts.dto.CashCachedBalanceResponse;
import com.bt.accounts.dto.CashCachedIssueRequest;
import com.bt.accounts.dto.CashCachedLedgerEntryResponse;
import com.bt.accounts.dto.CashCachedRedeemRequest;
import com.bt.accounts.dto.CashCachedSummaryResponse;
import com.bt.accounts.dto.CashCachedTransferRequest;
import com.bt.accounts.entity.CashCachedLedgerEntry;
import com.bt.accounts.service.CashCachedService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/financials/wallet")
@RequiredArgsConstructor
@Validated
@Tag(name = "Wallet Management", description = "APIs for managing customer wallet operations")
@SecurityRequirement(name = "Bearer Authentication")
public class CashCachedController {

        private final CashCachedService cashCachedService;

        @PostMapping("/issue")
        @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
        @Operation(summary = "Issue funds", description = "Issues new funds to a customer wallet")
        public ResponseEntity<ApiResponse<CashCachedLedgerEntryResponse>> issue(
                        @Valid @RequestBody CashCachedIssueRequest request) {
                CashCachedLedgerEntry entry = cashCachedService.issue(request);
                return new ResponseEntity<>(ApiResponse.<CashCachedLedgerEntryResponse>builder()
                                .success(true)
                                .message("Funds issued")
                                .data(CashCachedLedgerEntryResponse.fromEntity(entry))
                                .build(), HttpStatus.CREATED);
        }

        @PostMapping("/transfer")
        @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
        @Operation(summary = "Transfer funds", description = "Transfers funds between two customer wallets")
        public ResponseEntity<ApiResponse<List<CashCachedLedgerEntryResponse>>> transfer(
                        @Valid @RequestBody CashCachedTransferRequest request) {
                CashCachedService.TransferResult result = cashCachedService.transfer(request);
                List<CashCachedLedgerEntryResponse> entries = List.of(
                                CashCachedLedgerEntryResponse.fromEntity(result.getDebitEntry()),
                                CashCachedLedgerEntryResponse.fromEntity(result.getCreditEntry()));
                return ResponseEntity.ok(ApiResponse.<List<CashCachedLedgerEntryResponse>>builder()
                                .success(true)
                                .message("Transfer recorded")
                                .data(entries)
                                .build());
        }

        @PostMapping("/redeem")
        @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
        @Operation(summary = "Redeem funds", description = "Redeems funds from a customer wallet")
        public ResponseEntity<ApiResponse<CashCachedLedgerEntryResponse>> redeem(
                        @Valid @RequestBody CashCachedRedeemRequest request) {
                CashCachedLedgerEntry entry = cashCachedService.redeem(request);
                return ResponseEntity.ok(ApiResponse.<CashCachedLedgerEntryResponse>builder()
                                .success(true)
                                .message("Funds redeemed")
                                .data(CashCachedLedgerEntryResponse.fromEntity(entry))
                                .build());
        }

        @GetMapping("/balance/{customerId}")
        @Operation(summary = "Get wallet balance", description = "Retrieves the wallet balance for a specific customer")
        public ResponseEntity<ApiResponse<CashCachedBalanceResponse>> balance(@PathVariable String customerId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
                CashCachedBalanceResponse response = cashCachedService.balance(customerId, authHeader);
                return ResponseEntity.ok(ApiResponse.<CashCachedBalanceResponse>builder()
                                .success(true)
                                .message("Balance retrieved")
                                .data(response)
                                .build());
        }

        @PostMapping("/wallet/{customerId}/admin/credit")
        @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
        @Operation(summary = "Credit customer wallet", description = "Credits funds to a customer's wallet with admin privileges")
        public ResponseEntity<ApiResponse<CashCachedLedgerEntryResponse>> creditWallet(@PathVariable String customerId,
                        @Valid @RequestBody CashCachedIssueRequest request) {
                CashCachedLedgerEntry entry = cashCachedService.creditWallet(customerId, request.getAmount(),
                                request.getReference());
                return ResponseEntity.ok(ApiResponse.<CashCachedLedgerEntryResponse>builder()
                                .success(true)
                                .message("Wallet credited")
                                .data(CashCachedLedgerEntryResponse.fromEntity(entry))
                                .build());
        }

        @PostMapping("/wallet/{customerId}/admin/debit")
        @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
        @Operation(summary = "Debit customer wallet", description = "Debits funds from a customer's wallet with admin privileges")
        public ResponseEntity<ApiResponse<CashCachedLedgerEntryResponse>> debitWallet(@PathVariable String customerId,
                        @Valid @RequestBody CashCachedRedeemRequest request) {
                CashCachedLedgerEntry entry = cashCachedService.debitWallet(customerId, request.getAmount(),
                                request.getReference());
                return ResponseEntity.ok(ApiResponse.<CashCachedLedgerEntryResponse>builder()
                                .success(true)
                                .message("Wallet debited")
                                .data(CashCachedLedgerEntryResponse.fromEntity(entry))
                                .build());
        }

        @GetMapping("/history/{customerId}")
        @Operation(summary = "Get customer transaction history", description = "Retrieves wallet transaction history for a specific customer")
        public ResponseEntity<ApiResponse<List<CashCachedLedgerEntryResponse>>> history(
                        @PathVariable String customerId) {
                List<CashCachedLedgerEntry> history = cashCachedService.history(customerId);
                List<CashCachedLedgerEntryResponse> payload = history.stream()
                                .map(CashCachedLedgerEntryResponse::fromEntity)
                                .toList();
                return ResponseEntity.ok(ApiResponse.<List<CashCachedLedgerEntryResponse>>builder()
                                .success(true)
                                .message("History retrieved")
                                .data(payload)
                                .build());
        }

        @GetMapping("/history/all")
        @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
        @Operation(summary = "Get all transaction history", description = "Retrieves paginated wallet transaction history for all customers")
        public ResponseEntity<ApiResponse<Page<CashCachedLedgerEntryResponse>>> historyAll(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "25") int size) {

                int safePage = Math.max(page, 0);
                int boundedSize = Math.max(1, Math.min(size, 100));
                Pageable pageable = PageRequest.of(safePage, boundedSize);
                Page<CashCachedLedgerEntryResponse> payload = cashCachedService.historyAll(pageable)
                                .map(CashCachedLedgerEntryResponse::fromEntity);

                return ResponseEntity.ok(ApiResponse.<Page<CashCachedLedgerEntryResponse>>builder()
                                .success(true)
                                .message("History retrieved")
                                .data(payload)
                                .build());
        }

        @PostMapping("/wallet/transfer")
        @Operation(summary = "Transfer between wallets", description = "Transfers funds between customer wallets")
        public ResponseEntity<ApiResponse<List<CashCachedLedgerEntryResponse>>> walletTransfer(
                        @Valid @RequestBody CashCachedTransferRequest request) {
                CashCachedService.TransferResult result = cashCachedService.transfer(request);
                List<CashCachedLedgerEntryResponse> entries = List.of(
                                CashCachedLedgerEntryResponse.fromEntity(result.getDebitEntry()),
                                CashCachedLedgerEntryResponse.fromEntity(result.getCreditEntry()));
                return ResponseEntity.ok(ApiResponse.<List<CashCachedLedgerEntryResponse>>builder()
                                .success(true)
                                .message("Wallet transfer recorded")
                                .data(entries)
                                .build());
        }

        @GetMapping("/summary")
        @PreAuthorize("hasAnyRole('ADMIN','BANKOFFICER')")
        @Operation(summary = "Get wallet summary", description = "Retrieves a comprehensive summary of wallet operations")
        public ResponseEntity<ApiResponse<CashCachedSummaryResponse>> summary() {
                CashCachedSummaryResponse response = cashCachedService.summary();
                return ResponseEntity.ok(ApiResponse.<CashCachedSummaryResponse>builder()
                                .success(true)
                                .message("Summary retrieved")
                                .data(response)
                                .build());
        }

        @PostMapping("/add")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Add money to wallet", description = "Allows customers to add money to their wallet")
        public ResponseEntity<ApiResponse<CashCachedLedgerEntryResponse>> addMoney(
                        @Valid @RequestBody CashCachedIssueRequest request,
                        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
                String customerId = request.getCustomerId() != null ? request.getCustomerId() : userIdHeader;
                CashCachedLedgerEntry entry = cashCachedService.creditWallet(customerId, request.getAmount(),
                                request.getCurrency(), "Customer wallet top-up");
                return ResponseEntity.ok(ApiResponse.<CashCachedLedgerEntryResponse>builder()
                                .success(true)
                                .message("Money added to wallet successfully")
                                .data(CashCachedLedgerEntryResponse.fromEntity(entry))
                                .build());
        }

        @PostMapping("/withdraw")
        @PreAuthorize("hasRole('CUSTOMER')")
        @Operation(summary = "Withdraw money from wallet", description = "Allows customers to withdraw money from their wallet")
        public ResponseEntity<ApiResponse<CashCachedLedgerEntryResponse>> withdrawMoney(
                        @Valid @RequestBody CashCachedRedeemRequest request,
                        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
                String customerId = request.getCustomerId() != null ? request.getCustomerId() : userIdHeader;
                CashCachedLedgerEntry entry = cashCachedService.debitWallet(customerId, request.getAmount(),
                                request.getCurrency(), "Customer wallet withdrawal");
                return ResponseEntity.ok(ApiResponse.<CashCachedLedgerEntryResponse>builder()
                                .success(true)
                                .message("Money withdrawn from wallet successfully")
                                .data(CashCachedLedgerEntryResponse.fromEntity(entry))
                                .build());
        }

        @GetMapping("/transactions/{customerId}")
        @Operation(summary = "Get wallet transactions", description = "Retrieves wallet transaction history for a customer")
        public ResponseEntity<ApiResponse<List<CashCachedLedgerEntryResponse>>> getTransactions(
                        @PathVariable String customerId,
                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
                List<CashCachedLedgerEntry> history = cashCachedService.history(customerId);
                List<CashCachedLedgerEntryResponse> payload = history.stream()
                                .map(CashCachedLedgerEntryResponse::fromEntity)
                                .toList();
                return ResponseEntity.ok(ApiResponse.<List<CashCachedLedgerEntryResponse>>builder()
                                .success(true)
                                .message("Transactions retrieved")
                                .data(payload)
                                .build());
        }
}
