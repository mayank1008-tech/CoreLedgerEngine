package com.example.ledgersystem.Security.jwt;

import com.example.ledgersystem.Security.Services.UserDetailsImpl;
import com.example.ledgersystem.Security.Services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try{
            String jwt = parseJwt(request); //Extracting token
            if(jwt != null){
                log.debug("JWT token found in request: path={}", request.getServletPath());
                if(jwtUtils.validateToken(jwt)){ //Validating token
                    String username = jwtUtils.getUserNameFromToken(jwt); //Extracting user
                    log.debug("Valid JWT received for user: {}", username);
                    UserDetailsImpl userDetails  = (UserDetailsImpl) userDetailsServiceImpl.loadUserByUsername(username);  //Loading userdetails from DB to create a new auth obj

/*This is the child class of actual auth obj*/UsernamePasswordAuthenticationToken authentication = //creating container/auth obj which stores usernamepass and roles
                            new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authentication.setDetails( //Adding all request details(ip address) to authetication obj
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("User authenticated: {} with authorities={}", username, userDetails.getAuthorities());
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response); //Telling spring to continue with ita in built filters
    }

    private String parseJwt(HttpServletRequest request) { //Getting jwt
        String jwtFromCookies = jwtUtils.getJwtFromCookies(request);
        if(jwtFromCookies!=null){
            return jwtFromCookies;
        }

        //FOR SWAGGER
        String jwtFromHeader = jwtUtils.getJwtFromHeader(request);
        if(jwtFromHeader!=null){
            return jwtFromHeader;
        }

        return null;
    }
}
