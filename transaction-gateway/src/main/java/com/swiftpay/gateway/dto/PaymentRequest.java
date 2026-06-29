package com.swiftpay.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request payload for initiating a P2P payment")
public class PaymentRequest {

    @NotBlank(message = "sender_id is required")
    @Schema(description = "Unique ID of the sender", example = "user_001")
    private String senderId;

    @NotBlank(message = "receiver_id is required")
    @Schema(description = "Unique ID of the receiver", example = "user_002")
    private String receiverId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    @Digits(integer = 16, fraction = 2, message = "amount must have at most 16 integer digits and 2 decimal places")
    @Schema(description = "Amount to transfer", example = "500.00")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
    @Schema(description = "3-letter ISO currency code", example = "INR")
    private String currency;

    // Optional client-supplied idempotency key; we generate one if absent
    @Schema(description = "Optional client idempotency key for deduplication", example = "txn-abc-123")
    private String idempotencyKey;
}
