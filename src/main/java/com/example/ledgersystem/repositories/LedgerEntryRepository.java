package com.example.ledgersystem.repositories;

import com.example.ledgersystem.model.Account;
import com.example.ledgersystem.model.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
	
	Page<LedgerEntry> findLedgerEntryByAccount_AccountId(UUID accountAccountId, Pageable pageable);
	
	Optional<LedgerEntry> findTopByAccountOrderByLoggedAtDesc(Account account);
	
	List<LedgerEntry> findLedgerEntryByAccount_AccountIdOrderByLoggedAtAsc(UUID accountId);
	
	List<LedgerEntry> findLedgerEntryByAccount_AccountId(UUID accountAccountId);
}
