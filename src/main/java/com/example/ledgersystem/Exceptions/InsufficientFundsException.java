package com.example.ledgersystem.Exceptions;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
	
	private UUID accountId;
	
	public InsufficientFundsException(UUID accountId) {
		super(String.format("Insufficient funds for account id: %s", accountId));
		this.accountId = accountId;
	}
}
