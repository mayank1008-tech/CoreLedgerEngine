package com.example.ledgersystem.service;

import com.example.ledgersystem.Payloads.Auditresponse;
import com.example.ledgersystem.model.LedgerEntry;
import com.example.ledgersystem.repositories.LedgerEntryRepository;
import com.example.ledgersystem.utils.HashUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@Service
public class AdminServiceImpl implements  AdminService {
	@Autowired
	private LedgerEntryRepository ledgerEntryRepository;
	
	@Override
	public Auditresponse audit(UUID accountId) {
		// 1. CRITICAL: Fetch Oldest-to-Newest (ASC) to replay history
		List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findLedgerEntryByAccount_AccountIdOrderByLoggedAtAsc(accountId);
		
		// 2. Start with the Genesis Hash (Same constant you used in Service)
		String lastSeenHash = "MANK_1008";
		
		for (LedgerEntry ledgerEntry : ledgerEntries) {
			
			// --- CHECK 1: The Link (Chain Integrity) ---
			// Does this row point to the correct previous row?
			if (!ledgerEntry.getPrevHash().equals(lastSeenHash)) {
				return new Auditresponse("CORRUPTED: BROKEN CHAIN" , ledgerEntry.getLedgerId());
			}
			
			// --- CHECK 2: The Data (Content Integrity) ---
			// Re-calculate the hash using exactly the same logic as the Create Service
			String amountString = ledgerEntry.getAmount().stripTrailingZeros().toPlainString(); // "150.00" -> "150"
			String dataContent = lastSeenHash +
					amountString +
					ledgerEntry.getTransaction().getReferenceId() +
					ledgerEntry.getLoggedAt().toString();
			System.out.println("AUDIT GENERATED: " + dataContent);
			String calculatedHash = HashUtils.generateHash(dataContent);
			System.out.println("AUDIT: " + calculatedHash);
			
			if (!calculatedHash.equals(ledgerEntry.getHash())) {
				return new Auditresponse("CORRUPTED: DATA MODIFIED" , ledgerEntry.getLedgerId());
			}
			
			// 3. Move the pointer forward
			// The current row's hash becomes the "previous hash" for the next row
			lastSeenHash = ledgerEntry.getHash();
		}
		
		return new Auditresponse("VALID", null);
	}
}
