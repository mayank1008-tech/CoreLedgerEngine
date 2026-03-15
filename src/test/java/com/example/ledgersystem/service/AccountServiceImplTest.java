package com.example.ledgersystem.service;

import com.example.ledgersystem.Exceptions.APIexception;
import com.example.ledgersystem.Exceptions.AccountNotFound;
import com.example.ledgersystem.Exceptions.DuplicateTransactionException;
import com.example.ledgersystem.Exceptions.InsufficientFundsException;
import com.example.ledgersystem.Payloads.AccountCacheDTO;
import com.example.ledgersystem.Payloads.ApiResponse;
import com.example.ledgersystem.Payloads.StatementResponse;
import com.example.ledgersystem.enums.TransactionStatus;
import com.example.ledgersystem.enums.TransactionType;
import com.example.ledgersystem.model.Account;
import com.example.ledgersystem.model.LedgerEntry;
import com.example.ledgersystem.model.Transaction;
import com.example.ledgersystem.model.User;
import com.example.ledgersystem.repositories.AccountRepository;
import com.example.ledgersystem.repositories.LedgerEntryRepository;
import com.example.ledgersystem.repositories.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {
	
	// Use Spy so we can verify deposit() calls transfer() without testing transfer again
	@Spy @InjectMocks
	private AccountServiceImpl accountService;
	
	@Mock private LedgerEntryRepository ledgerEntryRepository;
	@Mock private TransactionRepository transactionRepository;
	@Mock private AccountRepository accountRepository;
	@Mock private RedisTemplate<String, Object> redisTemplate;
	
	// needed for getBalance()
	@Mock private ValueOperations<String, Object> valueOps;
	
	// ---------------------------------------------------------------------
	// TRANSFER tests (keep yours here; I included the central-bank one too)
	// ---------------------------------------------------------------------
	
	@Test
	void transfer_shouldThrow_whenAmountIsZero() {
		assertThrowsExactly(APIexception.class, () ->
				accountService.transfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO, "ref-1", UUID.randomUUID())
		);
		
		verifyNoInteractions(transactionRepository, accountRepository, ledgerEntryRepository, redisTemplate);
	}
	
	@Test
	void transfer_shouldThrow_whenAmountIsNegative() {
		assertThrowsExactly(APIexception.class, () ->
				accountService.transfer(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("-1"), "ref-1", UUID.randomUUID())
		);
		
		verifyNoInteractions(transactionRepository, accountRepository, ledgerEntryRepository, redisTemplate);
	}
	
	@Test
	void transfer_shouldThrow_whenReferenceIdAlreadyExists() {
		when(transactionRepository.findByReferenceId("ref-dup"))
				.thenReturn(Optional.of(new Transaction()));
		
		assertThrowsExactly(DuplicateTransactionException.class, () ->
				accountService.transfer(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "ref-dup", UUID.randomUUID())
		);
		
		verify(transactionRepository, never()).save(any());
		verifyNoInteractions(accountRepository, ledgerEntryRepository, redisTemplate);
	}
	
	@Test
	void transfer_shouldThrow_whenFromAccountNotFound() {
		UUID fromId = UUID.randomUUID();
		
		when(transactionRepository.findByReferenceId(anyString()))
				.thenReturn(Optional.empty());
		when(accountRepository.findById(fromId))
				.thenReturn(Optional.empty());
		
		assertThrowsExactly(AccountNotFound.class, () ->
				accountService.transfer(fromId, UUID.randomUUID(), BigDecimal.TEN, "ref-1", UUID.randomUUID())
		);
	}
	
	@Test
	void transfer_shouldThrow_whenToAccountNotFound() {
		UUID fromId = UUID.randomUUID();
		UUID toId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		
		when(transactionRepository.findByReferenceId(anyString()))
				.thenReturn(Optional.empty());
		
		Account sender = buildAccount(fromId, ownerId, "Alice", new BigDecimal("1000.00"));
		when(accountRepository.findById(fromId)).thenReturn(Optional.of(sender));
		when(accountRepository.findById(toId)).thenReturn(Optional.empty());
		
		assertThrowsExactly(AccountNotFound.class, () ->
				accountService.transfer(fromId, toId, BigDecimal.TEN, "ref-1", ownerId)
		);
	}
	
	@Test
	void transfer_shouldThrow_whenUserDoesNotOwnFromAccount_andNotCentralBank() {
		UUID fromId = UUID.randomUUID();
		UUID toId = UUID.randomUUID();
		
		UUID realOwnerId = UUID.randomUUID();
		UUID attackerId = UUID.randomUUID();
		
		when(transactionRepository.findByReferenceId(anyString()))
				.thenReturn(Optional.empty());
		
		Account sender = buildAccount(fromId, realOwnerId, "Alice", new BigDecimal("1000.00"));
		when(accountRepository.findById(fromId))
				.thenReturn(Optional.of(sender));
		
		assertThrowsExactly(APIexception.class, () ->
				accountService.transfer(fromId, toId, BigDecimal.TEN, "ref-1", attackerId)
		);
		
		verify(transactionRepository, never()).save(any());
		verify(accountRepository, never()).save(any());
		verify(ledgerEntryRepository, never()).save(any());
		verifyNoInteractions(redisTemplate);
	}
	
	@Test
	void transfer_shouldThrow_whenInsufficientFunds_andNotCentralBank() {
		UUID fromId = UUID.randomUUID();
		UUID toId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		
		when(transactionRepository.findByReferenceId(anyString()))
				.thenReturn(Optional.empty());
		
		Account senderLow = buildAccount(fromId, ownerId, "Alice", new BigDecimal("5.00"));
		Account receiver = buildAccount(toId, UUID.randomUUID(), "Bob", new BigDecimal("0.00"));
		
		when(accountRepository.findById(fromId)).thenReturn(Optional.of(senderLow));
		when(accountRepository.findById(toId)).thenReturn(Optional.of(receiver));
		
		assertThrowsExactly(InsufficientFundsException.class, () ->
				accountService.transfer(fromId, toId, new BigDecimal("10.00"), "ref-1", ownerId)
		);
		
		verify(transactionRepository, never()).save(any());
		verify(accountRepository, never()).save(any());
		verify(ledgerEntryRepository, never()).save(any());
	}
	
	@Test
	void transfer_shouldSucceed_andCreateTwoLedgerEntries_andUpdateBalances_andInvalidateCache() {
		UUID fromId = UUID.randomUUID();
		UUID toId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		
		when(transactionRepository.findByReferenceId(anyString()))
				.thenReturn(Optional.empty());
		
		Account sender = buildAccount(fromId, ownerId, "Alice", new BigDecimal("1000.00"));
		Account receiver = buildAccount(toId, UUID.randomUUID(), "Bob", new BigDecimal("100.00"));
		
		when(accountRepository.findById(fromId)).thenReturn(Optional.of(sender));
		when(accountRepository.findById(toId)).thenReturn(Optional.of(receiver));
		
		when(transactionRepository.save(any(Transaction.class)))
				.thenAnswer(inv -> inv.getArgument(0));
		when(ledgerEntryRepository.findTopByAccountOrderByLoggedAtDesc(any(Account.class)))
				.thenReturn(Optional.empty());
		when(ledgerEntryRepository.save(any(LedgerEntry.class)))
				.thenAnswer(inv -> inv.getArgument(0));
		when(accountRepository.save(any(Account.class)))
				.thenAnswer(inv -> inv.getArgument(0));
		
		ApiResponse response = accountService.transfer(fromId, toId, new BigDecimal("100.00"), "ref-ok", ownerId);
		
		assertTrue(response.isStatus());
		assertEquals("Transfer successful", response.getMessage());
		
		ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(txCaptor.capture());
		assertEquals(TransactionStatus.COMPLETED, txCaptor.getValue().getStatus());
		assertEquals(TransactionType.TRANSFER, txCaptor.getValue().getType());
		assertEquals("ref-ok", txCaptor.getValue().getReferenceId());
		assertNotNull(txCaptor.getValue().getTimestamp());
		
		verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
		verify(accountRepository, times(2)).save(any(Account.class));
		
		assertEquals(new BigDecimal("900.00"), sender.getBalance());
		assertEquals(new BigDecimal("200.00"), receiver.getBalance());
		
		verify(redisTemplate).delete("balance:" + fromId);
		verify(redisTemplate).delete("balance:" + toId);
	}
	
	@Test
	void transfer_shouldSucceed_whenFromAccountIsCentralBank_evenIfNotOwner_andEvenIfLowBalance() {
		UUID fromId = UUID.randomUUID();
		UUID toId = UUID.randomUUID();
		UUID authenticatedUserId = UUID.randomUUID();
		
		when(transactionRepository.findByReferenceId(anyString()))
				.thenReturn(Optional.empty());
		
		Account sender = buildAccount(fromId, UUID.randomUUID(), "CENTRAL_BANK", new BigDecimal("10.00"));
		Account receiver = buildAccount(toId, UUID.randomUUID(), "Bob", new BigDecimal("100.00"));
		
		when(accountRepository.findById(fromId)).thenReturn(Optional.of(sender));
		when(accountRepository.findById(toId)).thenReturn(Optional.of(receiver));
		
		when(transactionRepository.save(any(Transaction.class)))
				.thenAnswer(inv -> inv.getArgument(0));
		when(ledgerEntryRepository.findTopByAccountOrderByLoggedAtDesc(any(Account.class)))
				.thenReturn(Optional.empty());
		when(ledgerEntryRepository.save(any(LedgerEntry.class)))
				.thenAnswer(inv -> inv.getArgument(0));
		when(accountRepository.save(any(Account.class)))
				.thenAnswer(inv -> inv.getArgument(0));
		
		ApiResponse response = accountService.transfer(fromId, toId, new BigDecimal("100.00"), "ref-ok", authenticatedUserId);
		
		assertTrue(response.isStatus());
		verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
		verify(redisTemplate).delete("balance:" + fromId);
		verify(redisTemplate).delete("balance:" + toId);
	}
	
	// ---------------------------------------------------------------------
	// DEPOSIT tests
	// ---------------------------------------------------------------------
	
	@Test
	void deposit_shouldThrow_whenTargetAccountNotFound() {
		UUID toAccountId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		
		when(accountRepository.findById(toAccountId)).thenReturn(Optional.empty());
		
		assertThrowsExactly(AccountNotFound.class, () ->
				accountService.deposit(toAccountId, new BigDecimal("10.00"), "dep-1", userId)
		);
		
		verify(accountRepository, never()).findByName(anyString());
		verify(accountService, never()).transfer(any(), any(), any(), anyString(), any());
	}
	
	@Test
	void deposit_shouldThrow_whenUserDoesNotOwnTargetAccount() {
		UUID toAccountId = UUID.randomUUID();
		UUID realOwner = UUID.randomUUID();
		UUID attacker = UUID.randomUUID();
		
		Account target = buildAccount(toAccountId, realOwner, "Alice", BigDecimal.ZERO);
		when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(target));
		
		assertThrowsExactly(APIexception.class, () ->
				accountService.deposit(toAccountId, new BigDecimal("10.00"), "dep-1", attacker)
		);
		
		verify(accountRepository, never()).findByName(anyString());
		verify(accountService, never()).transfer(any(), any(), any(), anyString(), any());
	}
	
	@Test
	void deposit_shouldThrow_whenCentralBankMissing() {
		UUID toAccountId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		
		Account target = buildAccount(toAccountId, userId, "Alice", BigDecimal.ZERO);
		when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(target));
		
		when(accountRepository.findByName("CENTRAL_BANK")).thenReturn(Optional.empty());
		
		assertThrowsExactly(APIexception.class, () ->
				accountService.deposit(toAccountId, new BigDecimal("10.00"), "dep-1", userId)
		);
		
		verify(accountService, never()).transfer(any(), any(), any(), anyString(), any());
	}
	
	@Test
	void deposit_shouldCallTransfer_withCentralBankAsSender_onSuccess() {
		UUID toAccountId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID centralBankAccountId = UUID.randomUUID();
		
		Account target = buildAccount(toAccountId, userId, "Alice", BigDecimal.ZERO);
		Account central = buildAccount(centralBankAccountId, UUID.randomUUID(), "CENTRAL_BANK", new BigDecimal("999999.00"));
		
		when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(target));
		when(accountRepository.findByName("CENTRAL_BANK")).thenReturn(Optional.of(central));
		
		ApiResponse ok = new ApiResponse();
		ok.setStatus(true);
		ok.setMessage("Transfer successful");
		
		doReturn(ok).when(accountService).transfer(eq(centralBankAccountId), eq(toAccountId), eq(new BigDecimal("100.00")), eq("dep-1"), eq(userId));
		
		ApiResponse response = accountService.deposit(toAccountId, new BigDecimal("100.00"), "dep-1", userId);
		
		assertTrue(response.isStatus());
		verify(accountService, times(1)).transfer(centralBankAccountId, toAccountId, new BigDecimal("100.00"), "dep-1", userId);
	}
	
	// ---------------------------------------------------------------------
	// GET BALANCE tests
	// ---------------------------------------------------------------------
	
	@Test
	void getBalance_shouldReturnCachedBalance_whenCacheHitAndOwnerMatches() {
		UUID accountId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		String key = "balance:" + accountId;
		
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get(key)).thenReturn(new AccountCacheDTO(ownerId, new BigDecimal("123.45")));
		
		BigDecimal balance = accountService.getBalance(accountId, ownerId);
		assertEquals(new BigDecimal("123.45"), balance);
		
		verifyNoInteractions(accountRepository);
		verify(valueOps, never()).set(anyString(), any(), anyLong(), any());
	}
	
	@Test
	void getBalance_shouldThrow_whenCacheHitButOwnerMismatch() {
		UUID accountId = UUID.randomUUID();
		UUID realOwner = UUID.randomUUID();
		UUID attacker = UUID.randomUUID();
		String key = "balance:" + accountId;
		
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get(key)).thenReturn(new AccountCacheDTO(realOwner, new BigDecimal("123.45")));
		
		assertThrowsExactly(APIexception.class, () -> accountService.getBalance(accountId, attacker));
		
		verifyNoInteractions(accountRepository);
	}
	
	@Test
	void getBalance_shouldFetchFromDb_andPopulateCache_whenCacheMiss() {
		UUID accountId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		String key = "balance:" + accountId;
		
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get(key)).thenReturn(null);
		
		Account account = buildAccount(accountId, ownerId, "Alice", new BigDecimal("500.00"));
		when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
		
		BigDecimal balance = accountService.getBalance(accountId, ownerId);
		
		assertEquals(new BigDecimal("500.00"), balance);
		verify(valueOps, times(1)).set(eq(key), any(AccountCacheDTO.class), eq(10L), eq(TimeUnit.MINUTES));
	}
	
	@Test
	void getBalance_shouldFallbackToDb_whenRedisThrowsConnectionFailure_onRead() {
		UUID accountId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get(anyString())).thenThrow(new RedisConnectionFailureException("redis down"));
		
		Account account = buildAccount(accountId, ownerId, "Alice", new BigDecimal("77.00"));
		when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
		
		BigDecimal balance = accountService.getBalance(accountId, ownerId);
		assertEquals(new BigDecimal("77.00"), balance);
	}
	
	@Test
	void getBalance_shouldReturnDbBalance_evenIfRedisFailsOnWrite() {
		UUID accountId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		String key = "balance:" + accountId;
		
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		when(valueOps.get(key)).thenReturn(null);
		
		Account account = buildAccount(accountId, ownerId, "Alice", new BigDecimal("88.00"));
		when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
		
		doThrow(new RedisConnectionFailureException("redis down"))
				.when(valueOps).set(eq(key), any(AccountCacheDTO.class), eq(10L), eq(TimeUnit.MINUTES));
		
		BigDecimal balance = accountService.getBalance(accountId, ownerId);
		assertEquals(new BigDecimal("88.00"), balance);
	}
	
	// ---------------------------------------------------------------------
	// ACCOUNT STATEMENT tests
	// ---------------------------------------------------------------------
	
	@Test
	void accountStatement_shouldThrow_whenAccountNotFound() {
		UUID accountId = UUID.randomUUID();
		
		when(accountRepository.findById(accountId)).thenReturn(Optional.empty());
		
		assertThrowsExactly(AccountNotFound.class, () ->
				accountService.accountStatement(accountId, 0, 10, "loggedAt", "desc", UUID.randomUUID())
		);
	}
	
	@Test
	void accountStatement_shouldThrow_whenNotOwner() {
		UUID accountId = UUID.randomUUID();
		UUID realOwner = UUID.randomUUID();
		UUID attacker = UUID.randomUUID();
		
		Account account = buildAccount(accountId, realOwner, "Alice", BigDecimal.ZERO);
		when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
		
		assertThrowsExactly(APIexception.class, () ->
				accountService.accountStatement(accountId, 0, 10, "loggedAt", "desc", attacker)
		);
	}
	
	@Test
	void accountStatement_shouldReturnMappedEntries_withCreditDebitDerivedType() {
		UUID accountId = UUID.randomUUID();
		UUID ownerId = UUID.randomUUID();
		
		Account account = buildAccount(accountId, ownerId, "Alice", BigDecimal.ZERO);
		when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
		
		Transaction tx1 = new Transaction();
		tx1.setReferenceId("ref-1");
		tx1.setTimestamp(LocalDateTime.now());
		
		Transaction tx2 = new Transaction();
		tx2.setReferenceId("ref-2");
		tx2.setTimestamp(LocalDateTime.now());
		
		LedgerEntry credit = new LedgerEntry();
		credit.setAmount(new BigDecimal("50.00"));
		credit.setTransaction(tx1);
		
		LedgerEntry debit = new LedgerEntry();
		debit.setAmount(new BigDecimal("-10.00"));
		debit.setTransaction(tx2);
		
		Page<LedgerEntry> page = new PageImpl<>(List.of(credit, debit), PageRequest.of(0, 10), 2);
		
		// If your repository method signature differs, tell me the exact method and I’ll adjust.
		when(ledgerEntryRepository.findLedgerEntryByAccount_AccountId(eq(accountId), any(Pageable.class)))
				.thenReturn(page);
		
		StatementResponse resp = accountService.accountStatement(accountId, 0, 10, "loggedAt", "desc", ownerId);
		
		assertNotNull(resp);
		assertEquals(2, resp.getContent().size());
		assertEquals("CREDIT", resp.getContent().get(0).getType());
		assertEquals("DEBIT", resp.getContent().get(1).getType());
	}
	
	// ---------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------
	
	private static User buildUser(UUID id, String username) {
		User u = new User();
		u.setUser_id(id);
		u.setUsername(username);
		u.setPassword("dummy");
		u.setEmail(username.toLowerCase() + "@test.com");
		return u;
	}
	
	private static Account buildAccount(UUID accountId, UUID ownerId, String name, BigDecimal balance) {
		Account a = new Account();
		a.setAccountId(accountId);
		a.setName(name);
		a.setBalance(balance);
		a.setUser(buildUser(ownerId, name));
		return a;
	}
}
