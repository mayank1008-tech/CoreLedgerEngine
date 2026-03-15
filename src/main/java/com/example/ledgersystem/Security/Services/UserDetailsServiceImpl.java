package com.example.ledgersystem.Security.Services;

import com.example.ledgersystem.model.User;
import com.example.ledgersystem.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {  //Calls DAO and encoder type shit

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: username={}", username);
                    return new UsernameNotFoundException("User not found with given username!!!");
                });
        log.debug("User loaded successfully: username={}", username);
        return UserDetailsImpl.build(user);     //Builds the container which stores user data
    }
}
