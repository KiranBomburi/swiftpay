package com.swiftpay.ledger.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Single ledger entry in a user's transaction history")
public class TransactionHistoryResponse {

    private String transactionId;
    private String entryType;       // DEBIT or CREDIT
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String currency;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
