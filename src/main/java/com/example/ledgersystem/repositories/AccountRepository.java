package com.example.ledgersystem.repositories;

import com.example.ledgersystem.model.Account;
import com.example.ledgersystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
	
	Optional<Account> findByName(String centralBank);

	List<Account> findAllByUser(User user);
}
