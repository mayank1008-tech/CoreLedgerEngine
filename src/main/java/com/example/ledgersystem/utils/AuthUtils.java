package com.example.ledgersystem.utils;

import com.example.ledgersystem.Security.Services.UserDetailsImpl;
import com.example.ledgersystem.model.User;
import com.example.ledgersystem.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class AuthUtils {
    @Autowired
    UserRepository userRepository;

    public String loggedInEmail(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Fetching email for user: {}", authentication.getName());
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + authentication.getName()));

        return user.getEmail();
    }
	
	public UUID loggedInUserId(){
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		// Cast the "Principal" to your custom class
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		
		log.debug("Fetching userId from security context: userId={}", userDetails.getId());
		// Return the ID directly from memory (0ms latency)
		return userDetails.getId();
	}

    public User loggedInUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Fetching full user object for: {}", authentication.getName());

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + authentication.getName()));
        return user;

    }
}
