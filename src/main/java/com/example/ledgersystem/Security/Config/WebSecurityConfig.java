package com.example.ledgersystem.Security.Config;


import com.example.ledgersystem.Security.Services.UserDetailsServiceImpl;
import com.example.ledgersystem.Security.jwt.AuthEntryPointJwt;
import com.example.ledgersystem.Security.jwt.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity

public class WebSecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthTokenFilter authJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider() {
	    // 1. Use the Empty Constructor (as seen in the source code)
	    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
	    
	    // 2. Set the UserDetailsService manually
	    provider.setUserDetailsService(userDetailsServiceImpl); // <--- THIS IS THE FIX
	    
	    // 3. Set the PasswordEncoder
	    provider.setPasswordEncoder(passwordEncoder());
	    
	    return provider;

    }

    @Bean
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration auth) throws Exception {
        return auth.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                //Unauth ko exception mai daal do AuthEntruyPointJwt mai
        .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                //Stateless bnanae ke liye
                .sessionManagement(session  //cookie nhi bnegi
                -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(requests -> requests
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(("/v3/api-docs/**")).permitAll()
                        .requestMatchers(("/swagger-ui/**")).permitAll()
                        .requestMatchers(("/api/test/**")).permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated());

        //This sets our dao as a worker for AuthManager at login
        http.authenticationProvider(daoAuthenticationProvider());   //Defined for calls at login page at AuthManager

        //Setting our filter before every other chod bhangra
        http.addFilterBefore(authJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);


        http.headers(headers ->   //To disable login on h2
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow the frontend URL specifically
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));

        // Allow all HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));

        // Allow all headers (needed for Authorization and Content-Type)
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies/auth headers)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {    //BYpasses Every fuckinn spring filter
        return (web ->  web.ignoring().requestMatchers(
                "/configyration/ui",
                "/swagger-resources/**",
                "/configuration/security",
                "/swagger-ui.html",
                "/webjars/**"));
    }
}
