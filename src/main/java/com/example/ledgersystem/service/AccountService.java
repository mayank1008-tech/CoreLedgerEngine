package com.example.ledgersystem.service;

import com.example.ledgersystem.Payloads.ApiResponse;
import com.example.ledgersystem.Payloads.StatementResponse;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountService {
	ApiResponse transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String refrenceId, UUID authenticatedUserId);
	
	@Transactional
	ApiResponse deposit(UUID toAccountId, BigDecimal amount, String refId, UUID authenticatedUserId);
	
	BigDecimal getBalance(UUID accountId, UUID authenticatedUserId);
	
	StatementResponse accountStatement(UUID accountId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, UUID authenticatedUserId);

}
