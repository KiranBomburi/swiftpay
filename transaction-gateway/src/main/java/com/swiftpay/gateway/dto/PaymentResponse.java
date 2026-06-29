package com.swiftpay.gateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Response after a payment is initiated")
public class PaymentResponse {

    @Schema(description = "Unique transaction ID", example = "txn-550e8400-e29b-41d4")
    private String transactionId;

    @Schema(description = "Current status of the payment", example = "PENDING")
    private String status;

    private String senderId;

    private String receiverId;

    private BigDecimal amount;

    private String currency;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Message describing the result")
    private String message;
}
