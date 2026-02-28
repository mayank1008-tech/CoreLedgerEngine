package com.example.ledgersystem.model;

import com.example.ledgersystem.enums.TransactionStatus;
import com.example.ledgersystem.enums.TransactionType;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transaction")
public class Transaction {
	
	@Id
	@GeneratedValue
	private UUID transactionId;
	
	@Column(unique = true, nullable = false)//Idempotent key from frontend  same nhi ho sakti for single time click
	private String referenceId;
	
	@Enumerated(EnumType.STRING)  //DB mai actual enum name store honge naki 0 1 2
	private TransactionType type;  //Withdraw Deposit
	
	@Enumerated(EnumType.STRING)
	private TransactionStatus status;  //Processing Successfully Failed

	private LocalDateTime timestamp;
	
	
}
