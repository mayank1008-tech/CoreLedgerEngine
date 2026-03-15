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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
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
		systemAdmin.setEmail("system@gmail.com");
		userRepository.save(systemAdmin);
		log.debug("System admin user created: username=systemAdmin");
		
		Account systemAccount = new Account();
		systemAccount.setName("CENTRAL_BANK");
		systemAccount.setBalance(new BigDecimal("0.00"));
		systemAccount.setCurrency("INR");
		systemAccount.setUser(systemAdmin);
		accountRepository.save(systemAccount);
		log.debug("CENTRAL_BANK account created: accountId={}", systemAccount.getAccountId());
		
		// ---- CREATE USERS ----
		User aliceUser = new User();
		aliceUser.setEmail("alice@gmail.com");
		aliceUser.setPassword(passwordEncoder.encode("pass123"));
		aliceUser.setUsername("alice");
		aliceUser.setRole(List.of(userRole));
		
		User bobUser = new User();
		bobUser.setEmail("bob@gmail.com");
		bobUser.setPassword(passwordEncoder.encode("pass123"));
		bobUser.setUsername("bob");
		bobUser.setRole(List.of(userRole, adminRole));
		
		User charlieUser = new User();
		charlieUser.setEmail("charlie@gmail.com");
		charlieUser.setPassword(passwordEncoder.encode("pass123"));
		charlieUser.setUsername("charlie");
		charlieUser.setRole(List.of(userRole));
		
		userRepository.saveAll(List.of(aliceUser, bobUser, charlieUser));
		log.debug("Test users created: alice, bob, charlie");
		
		// 2. Create Dummy Accounts
		Account alice = new Account();
		alice.setName("Alice");
		alice.setCurrency("INR");
		alice.setBalance(new BigDecimal("1000.0000")); // Initial "Gift"
		alice.setUser(aliceUser);

		Account bob = new Account();
		bob.setName("Bob");
		bob.setCurrency("INR");
		bob.setBalance(new BigDecimal("1000.0000"));
		bob.setUser(bobUser);

		Account charlie = new Account();
		charlie.setName("Charlie");
		charlie.setCurrency("INR");
		charlie.setBalance(new BigDecimal("5000.0000"));
		charlie.setUser(charlieUser);

		// 3. Save to DB
		accountRepository.saveAll(Arrays.asList(alice, bob, charlie));

		log.info("Database seeded successfully. Accounts: alice={}, bob={}, charlie={}",
				alice.getAccountId(), bob.getAccountId(), charlie.getAccountId());
	}
}
