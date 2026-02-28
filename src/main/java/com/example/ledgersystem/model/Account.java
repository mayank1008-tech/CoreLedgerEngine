package com.example.ledgersystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "accounts")
public class Account {
	@Id
	@GeneratedValue
	private UUID accountId;
	
	@Column(nullable = false)
	private String name;
	
	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal balance = BigDecimal.ZERO;
	
	@Column(nullable = false)
	private String currency = "INR";
	
	@Version //Optimistic Locking ke liye.
	private Long version;
	
	@CreationTimestamp
	private LocalDateTime createdDate;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;
	

}
