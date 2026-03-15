package com.example.ledgersystem.controller;

import com.example.ledgersystem.Payloads.Auditresponse;
import com.example.ledgersystem.repositories.AccountRepository;
import com.example.ledgersystem.repositories.LedgerEntryRepository;
import com.example.ledgersystem.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {
	@Autowired
	private AdminService adminService;
	
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	@GetMapping("/audit/{accountId}")
	ResponseEntity<Auditresponse> audit(@PathVariable UUID accountId){
		log.info("Admin audit API called: accountId={}", accountId);
		Auditresponse auditresponse = adminService.audit(accountId);
		log.info("Admin audit API completed: accountId={}, result={}", accountId, auditresponse.getStatus());
		return  ResponseEntity.ok(auditresponse);
	}
	
	
}
