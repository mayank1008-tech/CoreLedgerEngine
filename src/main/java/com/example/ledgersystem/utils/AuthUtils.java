package com.example.ledgersystem.utils;

import com.example.ledgersystem.Security.Services.UserDetailsImpl;
import com.example.ledgersystem.model.User;
import com.example.ledgersystem.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthUtils {
    @Autowired
    UserRepository userRepository;

    public String loggedInEmail(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + authentication.getName()));

        return user.getEmail();
    }
	
	public UUID loggedInUserId(){
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		// Cast the "Principal" to your custom class
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		
		// Return the ID directly from memory (0ms latency)
		return userDetails.getId();
	}

    public User loggedInUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + authentication.getName()));
        return user;

    }
}
