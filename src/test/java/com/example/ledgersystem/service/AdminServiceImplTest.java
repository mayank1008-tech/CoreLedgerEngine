package com.example.ledgersystem.service;

import com.example.ledgersystem.Payloads.Auditresponse;
import com.example.ledgersystem.model.Account;
import com.example.ledgersystem.model.LedgerEntry;
import com.example.ledgersystem.model.Transaction;
import com.example.ledgersystem.repositories.LedgerEntryRepository;
import com.example.ledgersystem.utils.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {
	
	@Mock
	private LedgerEntryRepository ledgerEntryRepository;
	
	@InjectMocks
	private AdminServiceImpl adminService;
	
	@Test
	void audit_shouldReturnValid_whenChainAndHashesAreCorrect() {
		UUID accountId = UUID.randomUUID();
		
		LedgerEntry e1 = buildEntry(
				accountId,
				UUID.randomUUID(),
				"MANK_1008",
				new BigDecimal("10.00"),
				"ref-1",
				LocalDateTime.parse("2026-01-01T10:00:00")
		);
		
		// hash must match exactly what audit recomputes
		e1.setHash(calcHash("MANK_1008", e1));
		
		LedgerEntry e2 = buildEntry(
				accountId,
				UUID.randomUUID(),
				e1.getHash(),
				new BigDecimal("-5.00"),
				"ref-2",
				LocalDateTime.parse("2026-01-01T10:05:00")
		);
		e2.setHash(calcHash(e1.getHash(), e2));
		
		when(ledgerEntryRepository.findLedgerEntryByAccount_AccountIdOrderByLoggedAtAsc(accountId))
				.thenReturn(List.of(e1, e2));
		
		Auditresponse resp = adminService.audit(accountId);
		
		assertEquals("VALID", resp.getStatus());
		assertNull(resp.getBrokenAt());
	}
	
	@Test
	void audit_shouldReturnBrokenChain_whenPrevHashDoesNotMatchExpected() {
		UUID accountId = UUID.randomUUID();
		
		LedgerEntry e1 = buildEntry(
				accountId,
				UUID.randomUUID(),
				"WRONG_GENESIS",
				new BigDecimal("10.00"),
				"ref-1",
				LocalDateTime.parse("2026-01-01T10:00:00")
		);
		e1.setHash(calcHash("WRONG_GENESIS", e1));
		
		when(ledgerEntryRepository.findLedgerEntryByAccount_AccountIdOrderByLoggedAtAsc(accountId))
				.thenReturn(List.of(e1));
		
		Auditresponse resp = adminService.audit(accountId);
		
		assertEquals("CORRUPTED: BROKEN CHAIN", resp.getStatus());
		assertEquals(e1.getLedgerId(), resp.getBrokenAt());
	}
	
	@Test
	void audit_shouldReturnDataModified_whenHashDoesNotMatchRecalculatedValue() {
		UUID accountId = UUID.randomUUID();
		
		LedgerEntry e1 = buildEntry(
				accountId,
				UUID.randomUUID(),
				"MANK_1008",
				new BigDecimal("10.00"),
				"ref-1",
				LocalDateTime.parse("2026-01-01T10:00:00")
		);
		
		// simulate tampering: store an incorrect hash
		e1.setHash("tampered_hash_value");
		
		when(ledgerEntryRepository.findLedgerEntryByAccount_AccountIdOrderByLoggedAtAsc(accountId))
				.thenReturn(List.of(e1));
		
		Auditresponse resp = adminService.audit(accountId);
		
		assertEquals("CORRUPTED: DATA MODIFIED", resp.getStatus());
		assertEquals(e1.getLedgerId(), resp.getBrokenAt());
	}
	
	// -----------------------
	// Helpers
	// -----------------------
	
	private static LedgerEntry buildEntry(
			UUID accountId,
			UUID ledgerId,
			String prevHash,
			BigDecimal amount,
			String referenceId,
			LocalDateTime loggedAt
	) {
		Account a = new Account();
		a.setAccountId(accountId);
		
		Transaction tx = new Transaction();
		tx.setReferenceId(referenceId);
		
		LedgerEntry e = new LedgerEntry();
		e.setLedgerId(ledgerId);
		e.setAccount(a);
		e.setTransaction(tx);
		e.setPrevHash(prevHash);
		e.setAmount(amount);
		e.setLoggedAt(loggedAt);
		return e;
	}
	
	private static String calcHash(String lastSeenHash, LedgerEntry entry) {
		String amountString = entry.getAmount().stripTrailingZeros().toPlainString();
		String dataContent = lastSeenHash
				+ amountString
				+ entry.getTransaction().getReferenceId()
				+ entry.getLoggedAt().toString();
		return HashUtils.generateHash(dataContent);
	}
}
