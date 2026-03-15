package com.example.ledgersystem.controller;

import com.example.ledgersystem.Exceptions.APIexception;
import com.example.ledgersystem.Payloads.ApiResponse;
import com.example.ledgersystem.Payloads.CreateAccountDTO;
import com.example.ledgersystem.Payloads.DepositRequestDTO;
import com.example.ledgersystem.Payloads.MoneyTransferDTO;
import com.example.ledgersystem.Payloads.StatementResponse;
import com.example.ledgersystem.Payloads.WithdrawRequestDTO;
import com.example.ledgersystem.config.AppConst;
import com.example.ledgersystem.enums.RateLimitType;
import com.example.ledgersystem.model.Account;
import com.example.ledgersystem.model.User;
import com.example.ledgersystem.repositories.AccountRepository;
import com.example.ledgersystem.repositories.UserRepository;
import com.example.ledgersystem.service.AccountService;
import com.example.ledgersystem.service.RateLimitingService;
import com.example.ledgersystem.utils.AuthUtils;
import io.github.bucket4j.Bucket; // Import Bucket
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Slf4j
public class AccountController {
	
	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountRepository accountRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AuthUtils authUtils;
	@Autowired
	private RateLimitingService rateLimitingService;
	
	// --- HELPER METHOD TO CHECK RATE LIMIT ---
	private boolean isRateLimited(UUID userId, RateLimitType type) {
		Bucket bucket = rateLimitingService.resolveBucket(userId, type);
		//Try to consume 1 token from the available ones and when initiating this redis
		//auto initiates new tokens like User made a request 30 sec ago limit is 10 per min
		//so for next 30 sec I shall add 5 more and also reduce the one that he made request for.
		//Then proxy manager converts this calc in a Lua Script which is forwarded to Redis to update.
		return !bucket.tryConsume(1);
	}
	
	@PostMapping("/account/create")
	public ResponseEntity<ApiResponse> createAccount(@Valid @RequestBody CreateAccountDTO createAccountDTO) {
		UUID userId = authUtils.loggedInUserId();
		log.info("Create account API called: accountName={}, currency={}, user={}", createAccountDTO.getAccountName(), createAccountDTO.getCurrency(), userId);

		// 🛡️ SHIELD
		if (isRateLimited(userId, RateLimitType.GENERAL)) {
			log.warn("Rate limit exceeded: userId={}, type=GENERAL, endpoint=/api/account/create", userId);
			return new ResponseEntity<>(
					new ApiResponse("Too many requests! Please wait a minute.", false),
					HttpStatus.TOO_MANY_REQUESTS
			);
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new APIexception("User not found"));

		Account account = new Account();
		account.setName(createAccountDTO.getAccountName());
		account.setCurrency(createAccountDTO.getCurrency());
		account.setBalance(BigDecimal.ZERO);
		account.setUser(user);
		accountRepository.save(account);

		log.info("Create account API completed: accountId={}, user={}", account.getAccountId(), userId);
		return new ResponseEntity<>(new ApiResponse("Account created successfully!", true), HttpStatus.CREATED);
	}

	@GetMapping("/account/list")
	public ResponseEntity<?> listAccounts() {
		UUID userId = authUtils.loggedInUserId();
		log.info("List accounts API called: user={}", userId);

		// 🛡️ SHIELD
		if (isRateLimited(userId, RateLimitType.GENERAL)) {
			log.warn("Rate limit exceeded: userId={}, type=GENERAL, endpoint=/api/account/list", userId);
			return new ResponseEntity<>(
					new ApiResponse("Too many requests! Please wait a minute.", false),
					HttpStatus.TOO_MANY_REQUESTS
			);
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new APIexception("User not found"));

		List<Account> accounts = accountRepository.findAllByUser(user);
		log.debug("List accounts API completed: user={}, count={}", userId, accounts.size());
		return new ResponseEntity<>(accounts, HttpStatus.OK);
	}

	@PostMapping("/transfer")
	public ResponseEntity<ApiResponse> transferMoney(@Valid @RequestBody MoneyTransferDTO moneyTransferDTO) {
		UUID userId = authUtils.loggedInUserId();
		log.info("Transfer API called: fromAccount={}, toAccount={}, amount={}, reference={}, user={}",
				moneyTransferDTO.getFromAccountId(), moneyTransferDTO.getToAccountId(),
				moneyTransferDTO.getAmount(), moneyTransferDTO.getReferenceId(), userId);
		
		// 🛡️ SHIELD
		if (isRateLimited(userId, RateLimitType.TRANSACTION)) {
			log.warn("Rate limit exceeded: userId={}, type=TRANSACTION, endpoint=/api/transfer", userId);
			return new ResponseEntity<>(
					new ApiResponse("Too many requests! Please wait a minute.", false),
					HttpStatus.TOO_MANY_REQUESTS
			);
		}
		
		ApiResponse apiResponse = accountService.transfer(
				moneyTransferDTO.getFromAccountId(),
				moneyTransferDTO.getToAccountId(),
				moneyTransferDTO.getAmount(),
				moneyTransferDTO.getReferenceId(),
				userId
		);
		log.info("Transfer API completed: reference={}, user={}", moneyTransferDTO.getReferenceId(), userId);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK);
	}
	
	// Changed return type to <?> to handle both StatementResponse AND ApiResponse (Error)
	@GetMapping("/statement/{accountId}")
	public ResponseEntity<?> getStatement(@PathVariable("accountId") UUID accountId,
			@RequestParam(name = "pageNumber", defaultValue = AppConst.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConst.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = "loggedAt", required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConst.SORT_ORDER, required = false) String sortOrder) {
		UUID userId = authUtils.loggedInUserId();
		log.info("Statement API called: accountId={}, pageNumber={}, pageSize={}, user={}", accountId, pageNumber, pageSize, userId);
		
		// 🛡️ SHIELD
		if (isRateLimited(userId, RateLimitType.GENERAL)) {
			log.warn("Rate limit exceeded: userId={}, type=GENERAL, endpoint=/api/statement/{}", userId, accountId);
			return new ResponseEntity<>(
					new ApiResponse("Too many requests! Please wait a minute.", false),
					HttpStatus.TOO_MANY_REQUESTS
			);
		}
		
		StatementResponse statementResponse = accountService.accountStatement(accountId, pageNumber, pageSize, sortBy, sortOrder, userId);
		log.debug("Statement API completed: accountId={}, user={}", accountId, userId);
		return new ResponseEntity<>(statementResponse, HttpStatus.OK);
	}
	
	@PostMapping("/deposit")
	public ResponseEntity<ApiResponse> deposit(@Valid @RequestBody DepositRequestDTO depositRequestDTO) {
		UUID userId = authUtils.loggedInUserId();
		log.info("Deposit API called: toAccount={}, amount={}, reference={}, user={}",
				depositRequestDTO.getToAccountId(), depositRequestDTO.getAmount(),
				depositRequestDTO.getReferenceId(), userId);
		
		// 🛡️ SHIELD
		if (isRateLimited(userId, RateLimitType.TRANSACTION)) {
			log.warn("Rate limit exceeded: userId={}, type=TRANSACTION, endpoint=/api/deposit", userId);
			return new ResponseEntity<>(
					new ApiResponse("Too many requests! Please wait a minute.", false),
					HttpStatus.TOO_MANY_REQUESTS
			);
		}
		
		ApiResponse apiResponse = accountService.deposit(
				depositRequestDTO.getToAccountId(),
				depositRequestDTO.getAmount(),
				depositRequestDTO.getReferenceId(),
				userId
		);
		log.info("Deposit API completed: reference={}, user={}", depositRequestDTO.getReferenceId(), userId);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK);
	}
	
	@PostMapping("/withdraw")
	public ResponseEntity<ApiResponse> withdraw(@Valid @RequestBody WithdrawRequestDTO withdrawRequestDTO) {
		UUID userId = authUtils.loggedInUserId();
		log.info("Withdraw API called: fromAccount={}, amount={}, reference={}, user={}",
				withdrawRequestDTO.getFromAccountId(), withdrawRequestDTO.getAmount(),
				withdrawRequestDTO.getReferenceId(), userId);
		
		// 🛡️ SHIELD
		if (isRateLimited(userId, RateLimitType.TRANSACTION)) {
			log.warn("Rate limit exceeded: userId={}, type=TRANSACTION, endpoint=/api/withdraw", userId);
			return new ResponseEntity<>(
					new ApiResponse("Too many requests! Please wait a minute.", false),
					HttpStatus.TOO_MANY_REQUESTS
			);
		}
		
		Optional<Account> toAccount = accountRepository.findByName("CENTRAL_BANK");
		if (toAccount.isEmpty()) {
			log.error("System error: CENTRAL_BANK account not found during withdrawal, reference={}", withdrawRequestDTO.getReferenceId());
			throw new APIexception("System Error: Central Bank Vault missing");
		}
		
		ApiResponse apiResponse = accountService.transfer(
				withdrawRequestDTO.getFromAccountId(),
				toAccount.get().getAccountId(),
				withdrawRequestDTO.getAmount(),
				withdrawRequestDTO.getReferenceId(),
				userId
		);
		log.info("Withdraw API completed: reference={}, user={}", withdrawRequestDTO.getReferenceId(), userId);
		return new ResponseEntity<>(apiResponse, HttpStatus.OK);
	}
	
	// Changed return type to <?> to handle both BigDecimal AND ApiResponse (Error)
	@GetMapping("/balance/{accountId}")
	public ResponseEntity<?> getBalance(@PathVariable UUID accountId) {
		UUID authenticatedUserId = authUtils.loggedInUserId();
		log.info("Balance API called: accountId={}, user={}", accountId, authenticatedUserId);
		
		// 🛡️ SHIELD
		if (isRateLimited(authenticatedUserId, RateLimitType.GENERAL)) {
			log.warn("Rate limit exceeded: userId={}, type=GENERAL, endpoint=/api/balance/{}", authenticatedUserId, accountId);
			return new ResponseEntity<>(
					new ApiResponse("Too many requests! Please wait a minute.", false),
					HttpStatus.TOO_MANY_REQUESTS
			);
		}
		
		BigDecimal balance = accountService.getBalance(accountId, authenticatedUserId);
		log.debug("Balance API completed: accountId={}, user={}", accountId, authenticatedUserId);
		return new ResponseEntity<>(balance, HttpStatus.OK);
	}
}
