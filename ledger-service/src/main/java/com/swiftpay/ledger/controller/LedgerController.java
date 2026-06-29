package com.swiftpay.ledger.controller;

import com.swiftpay.ledger.dto.TransactionHistoryResponse;
import com.swiftpay.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/ledger")
@Tag(name = "Ledger", description = "Balance and transaction history endpoints")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get transaction history for a user",
               description = "Returns all ledger entries (debits and credits) for the given userId, ordered by most recent first.")
    public ResponseEntity<List<TransactionHistoryResponse>> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(ledgerService.getTransactionHistory(userId));
    }
}
