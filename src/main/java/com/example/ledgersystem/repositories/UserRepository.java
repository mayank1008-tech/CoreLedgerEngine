package com.example.ledgersystem.repositories;

import com.example.ledgersystem.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByUsername(String username);
	Optional<User> findByEmail(String email);
	boolean existsByUsername(@NotBlank @Size(min = 3, max = 30) String username);
	
	boolean existsByEmail(@Email @NotBlank @Size(min = 3, max = 30) String email);

}
