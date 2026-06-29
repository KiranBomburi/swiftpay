package com.swiftpay.gateway.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String senderId) {
        super("Insufficient balance for account: " + senderId);
    }
}
