package com.example.ledgersystem.service;

import com.example.ledgersystem.Payloads.Auditresponse;
import com.example.ledgersystem.model.LedgerEntry;
import com.example.ledgersystem.repositories.LedgerEntryRepository;
import com.example.ledgersystem.utils.HashUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AdminServiceImpl implements  AdminService {
	@Autowired
	private LedgerEntryRepository ledgerEntryRepository;
	
	@Override
	public Auditresponse audit(UUID accountId) {
		log.info("Audit started: accountId={}", accountId);
		// 1. CRITICAL: Fetch Oldest-to-Newest (ASC) to replay history
		List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findLedgerEntryByAccount_AccountIdOrderByLoggedAtAsc(accountId);
		log.debug("Audit: fetched {} ledger entries for accountId={}", ledgerEntries.size(), accountId);
		
		// 2. Start with the Genesis Hash (Same constant you used in Service)
		String lastSeenHash = "MANK_1008";
		
		for (LedgerEntry ledgerEntry : ledgerEntries) {
			
			// --- CHECK 1: The Link (Chain Integrity) ---
			// Does this row point to the correct previous row?
			if (!ledgerEntry.getPrevHash().equals(lastSeenHash)) {
				log.warn("Audit failed - broken chain: accountId={}, ledgerId={}, expectedPrevHash={}, actualPrevHash={}",
						accountId, ledgerEntry.getLedgerId(), lastSeenHash, ledgerEntry.getPrevHash());
				return new Auditresponse("CORRUPTED: BROKEN CHAIN" , ledgerEntry.getLedgerId());
			}
			
			// --- CHECK 2: The Data (Content Integrity) ---
			// Re-calculate the hash using exactly the same logic as the Create Service
			String amountString = ledgerEntry.getAmount().stripTrailingZeros().toPlainString(); // "150.00" -> "150"
			String dataContent = lastSeenHash +
					amountString +
					ledgerEntry.getTransaction().getReferenceId() +
					ledgerEntry.getLoggedAt().toString();
			log.debug("Audit checking entry: ledgerId={}, reference={}", ledgerEntry.getLedgerId(), ledgerEntry.getTransaction().getReferenceId());
			String calculatedHash = HashUtils.generateHash(dataContent);
			
			if (!calculatedHash.equals(ledgerEntry.getHash())) {
				log.warn("Audit failed - data modified: accountId={}, ledgerId={}, reference={}",
						accountId, ledgerEntry.getLedgerId(), ledgerEntry.getTransaction().getReferenceId());
				return new Auditresponse("CORRUPTED: DATA MODIFIED" , ledgerEntry.getLedgerId());
			}
			
			// 3. Move the pointer forward
			// The current row's hash becomes the "previous hash" for the next row
			lastSeenHash = ledgerEntry.getHash();
		}
		
		log.info("Audit completed: accountId={}, result=VALID, entriesChecked={}", accountId, ledgerEntries.size());
		return new Auditresponse("VALID", null);
	}
}
