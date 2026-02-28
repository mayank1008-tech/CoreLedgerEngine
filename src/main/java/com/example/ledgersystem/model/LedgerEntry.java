package com.example.ledgersystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ledgerEntries")
public class LedgerEntry {
	
	@Id
	@GeneratedValue
	private UUID ledgerId;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "transaction_id")
	@ToString.Exclude
	private Transaction transaction;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "account_id")
	@ToString.Exclude
	private Account account;
	
	@Column(nullable = false)
	private BigDecimal amount;

	private LocalDateTime loggedAt;
	
	@Column(nullable = false)
	private String hash;
	
	@Column(name = "previous_hash")
	private String prevHash;
}
