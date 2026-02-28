package com.example.ledgersystem.Payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountStatementDTO {
	private LocalDateTime date;
	private String referenceId;
	private BigDecimal amount;
	private String type;
}
