package com.example.ledgersystem;

import com.example.ledgersystem.model.Account;
import com.example.ledgersystem.model.AppRoles;
import com.example.ledgersystem.model.Role;
import com.example.ledgersystem.model.User;
import com.example.ledgersystem.repositories.AccountRepository;
import com.example.ledgersystem.repositories.RoleRepository;
import com.example.ledgersystem.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component // <--- Tells Spring to manage this class
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
	private final PasswordEncoder passwordEncoder;

	private final AccountRepository accountRepository;
	
	private final UserRepository userRepository;
	
	private final RoleRepository roleRepository;

	@Override
	public void run(String... args) throws Exception {

		// 1. Check if data already exists so we don't duplicate every restart
		if (accountRepository.count() > 0) {
			log.info("Database already seeded. Skipping data initialization.");
			return;
		}
		
		log.info("Starting database seed...");

		try {
			Role userRole = roleRepository
					.findByRoleName(AppRoles.ROLE_USER)
					.orElseGet(() -> roleRepository.save(new Role(AppRoles.ROLE_USER)));
			log.debug("Role created/found: {}", AppRoles.ROLE_USER);
			
			Role adminRole = roleRepository
					.findByRoleName(AppRoles.ROLE_ADMIN)
					.orElseGet(() -> roleRepository.save(new Role(AppRoles.ROLE_ADMIN)));
			log.debug("Role created/found: {}", AppRoles.ROLE_ADMIN);
			
			User systemAdmin = new User();
			systemAdmin.setUsername("systemAdmin");
			systemAdmin.setPassword(passwordEncoder.encode("pass123"));
			systemAdmin.setRole(List.of(userRole, adminRole));
			systemAdmin.setEmail("system@admin.com");
			userRepository.save(systemAdmin);
			log.debug("System admin user created: username=systemAdmin, email=system@admin.com");
			
			Account centralBank = new Account();
			centralBank.setName("CENTRAL_BANK");
			centralBank.setBalance(new BigDecimal("0.00"));
			centralBank.setCurrency("INR");
			centralBank.setUser(systemAdmin);
			accountRepository.save(centralBank);
			log.debug("CENTRAL_BANK account created: accountId={}", centralBank.getAccountId());
			
			log.info("Database seeded successfully.");
			log.info("=====================================");
			log.info("System ready! Available endpoints:");
			log.info("  POST /api/auth/signup     - Register a new user");
			log.info("  POST /api/auth/signin     - Login");
			log.info("  POST /api/account/create  - Create a new account (auth required)");
			log.info("  GET  /api/account/list    - List your accounts (auth required)");
			log.info("  POST /api/deposit         - Deposit funds (auth required)");
			log.info("  POST /api/transfer        - Transfer funds (auth required)");
			log.info("  POST /api/withdraw        - Withdraw funds (auth required)");
			log.info("=====================================");
		} catch (DataIntegrityViolationException e) {
			log.warn("Data seeding skipped due to concurrent initialization: {}", e.getMessage());
		}
	}
}
