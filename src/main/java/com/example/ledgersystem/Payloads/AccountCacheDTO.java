package com.example.ledgersystem.Payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountCacheDTO {
	private UUID ownerId;
	private BigDecimal balance;
}
