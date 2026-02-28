package com.example.ledgersystem.Exceptions;

public class DuplicateTransactionException extends RuntimeException {
	
	private String refrenceId;
	
	public DuplicateTransactionException(String refrenceId) {
		super(String.format("Transaction request for refrenceId: %s already exists!!!", refrenceId));
		this.refrenceId = refrenceId;
	}
}
