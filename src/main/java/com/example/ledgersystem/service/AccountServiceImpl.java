package com.example.ledgersystem.service;

import com.example.ledgersystem.Exceptions.APIexception;
import com.example.ledgersystem.Exceptions.DuplicateTransactionException;
import com.example.ledgersystem.Exceptions.AccountNotFound;
import com.example.ledgersystem.Exceptions.InsufficientFundsException;
import com.example.ledgersystem.Payloads.AccountCacheDTO;
import com.example.ledgersystem.Payloads.AccountStatementDTO;
import com.example.ledgersystem.Payloads.ApiResponse;
import com.example.ledgersystem.Payloads.StatementResponse;
import com.example.ledgersystem.enums.TransactionStatus;
import com.example.ledgersystem.enums.TransactionType;
import com.example.ledgersystem.model.Account;
import com.example.ledgersystem.model.LedgerEntry;
import com.example.ledgersystem.model.Transaction;
import com.example.ledgersystem.repositories.AccountRepository;
import com.example.ledgersystem.repositories.LedgerEntryRepository;
import com.example.ledgersystem.repositories.TransactionRepository;
import com.example.ledgersystem.utils.HashUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class AccountServiceImpl implements AccountService {
	@Autowired
	private LedgerEntryRepository ledgerEntryRepository;
	@Autowired
	private TransactionRepository transactionRepository;
	@Autowired
	private AccountRepository accountRepository;
	@Autowired
	private RedisTemplate<String, Object> redisTemplate; // <--- Changed from StringRedisTemplate
	
	@Retryable(
			retryFor = ObjectOptimisticLockingFailureException.class,
			maxAttempts = 3,
			backoff = @Backoff(delay = 1000)
	)
	@Transactional
	@Override
	public ApiResponse transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String refrenceId, UUID authenticatedUserId){
		
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new APIexception("Transfer amount must be greater than zero");
		}
		
		Optional<Transaction> trans = transactionRepository.findByReferenceId(refrenceId);
		if(trans.isPresent()){
			throw new DuplicateTransactionException(refrenceId);
		}
		
		Optional<Account> existingSendersAccount = accountRepository.findById(fromAccountId);
		if(existingSendersAccount.isEmpty()){
			throw new AccountNotFound(fromAccountId);
		}
		
		boolean isCentralVault = existingSendersAccount.get().getName().equals("CENTRAL_BANK");
		UUID ownerId = existingSendersAccount.get().getUser().getUser_id();
		
		if (!isCentralVault && !ownerId.equals(authenticatedUserId)) {
			throw new APIexception("You do not own this account!");
		}
		
		Optional<Account> existingRecieversAccount = accountRepository.findById(toAccountId);
		if(existingRecieversAccount.isEmpty()){
			throw new AccountNotFound(toAccountId);
		}
		
		if(!existingSendersAccount.get().getName().equals("CENTRAL_BANK")
				&& existingSendersAccount.get().getBalance().compareTo(amount) < 0){
			throw new InsufficientFundsException(fromAccountId);
		}
		
		Transaction transaction = new Transaction();
		transaction.setStatus(TransactionStatus.COMPLETED);
		transaction.setType(TransactionType.TRANSFER);
		transaction.setReferenceId(refrenceId);
		transaction.setTimestamp(LocalDateTime.now());
		transactionRepository.save(transaction);
		
		createLedgerEntry(existingSendersAccount.get(), amount.negate(), transaction);
		createLedgerEntry(existingRecieversAccount.get(), amount, transaction);
		
		existingSendersAccount.get().setBalance(existingSendersAccount.get().getBalance().subtract(amount));
		existingRecieversAccount.get().setBalance(existingRecieversAccount.get().getBalance().add(amount));
		
		accountRepository.save(existingSendersAccount.get());
		accountRepository.save(existingRecieversAccount.get());
		
		String senderKey = "balance:" + fromAccountId;
		redisTemplate.delete(senderKey);
		
		// 2. Invalidate Receiver
		String receiverKey = "balance:" + toAccountId;
		redisTemplate.delete(receiverKey);
		
		String message = "Transfer successful";
		ApiResponse response = new ApiResponse();
		response.setMessage(message);
		response.setStatus(true);
		
		return response;
	}
	
	private LedgerEntry createLedgerEntry(Account account, BigDecimal amount, Transaction transaction){
		LedgerEntry entry = new LedgerEntry();
		entry.setAccount(account);
		entry.setAmount(amount);
		entry.setTransaction(transaction);
		LocalDateTime cleanTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
		entry.setLoggedAt(cleanTime);
		
		String prevHash = ledgerEntryRepository.findTopByAccountOrderByLoggedAtDesc(account)
				.map(LedgerEntry::getHash)
				.orElse("MANK_1008"); // If first transaction, use a placeholder
		
		entry.setPrevHash(prevHash);
		String amountString = entry.getAmount().stripTrailingZeros().toPlainString(); // "150.00" -> "150"
		String dataContent = prevHash +
				amountString +
				transaction.getReferenceId() +
				entry.getLoggedAt().toString();
		String myHash = HashUtils.generateHash(dataContent);
		entry.setHash(myHash);
		
		return ledgerEntryRepository.save(entry);
	}
	
	@Transactional
	@Override
	public ApiResponse deposit(UUID toAccountId, BigDecimal amount, String refId, UUID authenticatedUserId) {
		
		// 1. Fetch the Target Account
		Account targetAccount = accountRepository.findById(toAccountId)
				.orElseThrow(() -> new AccountNotFound(toAccountId));
		
		// 2. SECURITY CHECK: Ensure the User is depositing into THEIR OWN account
		UUID ownerId = targetAccount.getUser().getUser_id();
		if (!ownerId.equals(authenticatedUserId)) {
			throw new APIexception("Security Alert: You can only deposit funds into your own account!");
		}
		
		// 3. Fetch Central Bank
		Optional<Account> centralBank = accountRepository.findByName("CENTRAL_BANK");
		if(centralBank.isEmpty()){
			throw new APIexception("System Error: Central Bank Vault missing");
		}
		
		// 4. Perform the Transfer (Using the Bypass Logic we added)
		// We pass "system" or specific logic to bypass the ownership check for the SENDER (Central Bank)
		// But we have already verified the RECEIVER above.
		return transfer(centralBank.get().getAccountId(), toAccountId, amount, refId, authenticatedUserId);
	}
	
	@Override
	public BigDecimal getBalance(UUID accountId, UUID authenticatedUserId) {
		String key = "balance:" + accountId;
		
		// --- 1. FAST PATH (Redis) ---
		// We try to read the whole object (Owner + Balance)
		try {
			Object cachedData = redisTemplate.opsForValue().get(key);
			if (cachedData != null) {
				// Since we configured JSON Serializer, this casts automatically
				AccountCacheDTO cacheDto = (AccountCacheDTO) cachedData;
				
				// 🔒 SECURITY CHECK (In Memory - 0ms)
				if (!cacheDto.getOwnerId().equals(authenticatedUserId)) {
					throw new APIexception("Security Alert: You do not own this account!");
				}
				
				System.out.println("🚀 CACHE HIT: Returning securely from Redis!");
				return cacheDto.getBalance();
			}
			
			// --- 2. SLOW PATH (Database) ---
			System.out.println("🐢 CACHE MISS: Fetching from DB...");
		}catch (RedisConnectionFailureException e){
			log.warn("Redis unavailable, continuing without cache: {}", e.getMessage());
		}
		
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new AccountNotFound(accountId));
		
		// 🔒 SECURITY CHECK (Database Level)
		if (!account.getUser().getUser_id().equals(authenticatedUserId)) {
			throw new APIexception("Security Alert: You do not own this account!");
		}
		
		BigDecimal balance = account.getBalance();
		
		// --- 3. REFILL CACHE ---
		// Save both the balance AND the owner ID so we can check it next time
		AccountCacheDTO newCacheEntry = new AccountCacheDTO(
				account.getUser().getUser_id(),
				balance
		);
		try {
			// Save to Redis (Expires in 10 mins)
			redisTemplate.opsForValue().set(key, newCacheEntry, 10, TimeUnit.MINUTES);
		}catch (RedisConnectionFailureException e){
			log.warn("Redis unavailable, continuing without cache: {}", e.getMessage());
		}
		
		return balance;
	}
	
	@Override
	public StatementResponse accountStatement(UUID accountId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, UUID authenticatedUserId){
		Optional<Account> existingAccount = accountRepository.findById(accountId);
		if(existingAccount.isEmpty()){
			throw new AccountNotFound(accountId);
		}
		UUID ownerId = existingAccount.get().getUser().getUser_id();
		
		if (!ownerId.equals(authenticatedUserId)) {
			throw new APIexception("You do not own this account!");
		}
		
		Sort sorted = sortOrder.equalsIgnoreCase("asc")
				? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
		Pageable pageable = PageRequest.of(pageNumber, pageSize,sorted);
		Page<LedgerEntry> pagedLedgers = ledgerEntryRepository.findLedgerEntryByAccount_AccountId(accountId, pageable);
		List<LedgerEntry> entries = pagedLedgers.getContent();
		
		List<AccountStatementDTO> accountStatements = new ArrayList<>();
		for(LedgerEntry ledgerEntry : entries){
			AccountStatementDTO accountStatementDTO = new AccountStatementDTO();
			accountStatementDTO.setAmount(ledgerEntry.getAmount());
			accountStatementDTO.setDate(ledgerEntry.getTransaction().getTimestamp());
			accountStatementDTO.setReferenceId(ledgerEntry.getTransaction().getReferenceId());
			String derivedType = ledgerEntry.getAmount().compareTo(BigDecimal.ZERO) > 0 ? "CREDIT" : "DEBIT";
			accountStatementDTO.setType(derivedType);
			
			accountStatements.add(accountStatementDTO);
		}
		StatementResponse statementResponse = new StatementResponse();
		statementResponse.setContent(accountStatements);
		statementResponse.setLastPage(pagedLedgers.isLast());
		statementResponse.setPageNumber(pagedLedgers.getNumber());
		statementResponse.setPageSize(pagedLedgers.getSize());
		statementResponse.setTotalElements(pagedLedgers.getTotalElements());
		statementResponse.setTotalPages(pagedLedgers.getTotalPages());
		
		return statementResponse;
	}
}
