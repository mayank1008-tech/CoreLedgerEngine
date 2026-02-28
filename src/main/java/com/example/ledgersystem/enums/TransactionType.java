package com.example.ledgersystem.enums;

public enum TransactionType {
	DEPOSIT,      // Money coming in
	WITHDRAWAL,   // Money going out
	TRANSFER,     // Between two accounts
	REVERSAL      // Undo a previous transaction
}
