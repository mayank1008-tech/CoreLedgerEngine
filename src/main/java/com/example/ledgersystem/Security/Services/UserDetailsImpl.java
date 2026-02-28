package com.example.ledgersystem.Security.Services;

import com.example.ledgersystem.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID; // Import UUID!
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class UserDetailsImpl implements UserDetails {
	
	private static final long serialVersionUID = 1L;
	
	// 1. FIX: Use UUID to match your Database Schema
	private UUID id;
	
	private String username;
	
	@JsonIgnore
	private String password;
	
	private String email;
	
	private Collection<? extends GrantedAuthority> authorities;
	
	public UserDetailsImpl(UUID id, String username, String password, String email, Collection<? extends GrantedAuthority> authorities) {
		this.id = id;
		this.username = username;
		this.password = password;
		this.email = email;
		this.authorities = authorities;
	}
	
	public static UserDetailsImpl build(User user) {
		// 2. FIX: Handle List<AppRoles> correctly
		// Assuming user.getRoles() returns List<AppRoles>
		List<GrantedAuthority> authorities = user.getRole().stream()
				.map(role -> new SimpleGrantedAuthority(role.getRoleName().name())) // Enum.name() gives "ROLE_ADMIN"
				.collect(Collectors.toList());
		
		return new UserDetailsImpl(
				user.getUser_id(), // 3. FIX: Use the actual getter (check your User entity!)
				user.getUsername(),
				user.getPassword(),
				user.getEmail(),
				authorities
		);
	}
	
	// ... (Keep your overrides below, they look fine) ...
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserDetailsImpl user = (UserDetailsImpl) o;
		return Objects.equals(id, user.id);
	}
	
	// Standard getters for interface...
	@Override public String getPassword() { return password; }
	@Override public String getUsername() { return username; }
	@Override public boolean isAccountNonExpired() { return true; }
	@Override public boolean isAccountNonLocked() { return true; }
	@Override public boolean isCredentialsNonExpired() { return true; }
	@Override public boolean isEnabled() { return true; }
}
