package com.example.ledgersystem.service;

import com.example.ledgersystem.Payloads.Auditresponse;

import java.util.UUID;

public interface AdminService {
	Auditresponse audit(UUID accountId);
	
}
