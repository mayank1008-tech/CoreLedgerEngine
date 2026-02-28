package com.example.ledgersystem.Exceptions;

import java.util.UUID;

public class AccountNotFound extends RuntimeException {
	UUID uuid;
	
	public AccountNotFound() {
	}
	
	public AccountNotFound(UUID uuid) {
		super(String.format("Account not found with accountID: %s",uuid));
		this.uuid = uuid;
	}
	
	
}
