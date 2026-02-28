package com.example.ledgersystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role {
	
	@Id
	@GeneratedValue
	private UUID id;
	
	@Enumerated(EnumType.STRING)
	@Column(length = 20, name = "role_name", nullable = false)
	private AppRoles roleName;
	
	public Role(AppRoles appRoles) {
		this.roleName = appRoles;
	}
}

