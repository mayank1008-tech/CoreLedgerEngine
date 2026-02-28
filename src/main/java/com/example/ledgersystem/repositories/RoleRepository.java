package com.example.ledgersystem.repositories;

import com.example.ledgersystem.model.AppRoles;
import com.example.ledgersystem.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
		Optional<Role> findByRoleName(AppRoles roleName);
}
