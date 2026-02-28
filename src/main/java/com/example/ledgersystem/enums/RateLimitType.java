package com.example.ledgersystem.enums;

public enum RateLimitType {
	GENERAL,    // For Balance, Statements (Loose)
	TRANSACTION // For Transfer, Withdraw, Deposit (Strict)
}
