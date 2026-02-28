package com.example.ledgersystem.Payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Auditresponse {
	private String status;
	private UUID brokenAt;
}
