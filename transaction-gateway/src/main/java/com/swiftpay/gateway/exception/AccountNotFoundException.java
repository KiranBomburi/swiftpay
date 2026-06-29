package com.swiftpay.gateway.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String userId) {
        super("Account not found for user: " + userId);
    }
}
