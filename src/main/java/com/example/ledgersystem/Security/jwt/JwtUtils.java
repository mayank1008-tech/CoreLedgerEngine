package com.example.ledgersystem.Security.jwt;

import com.example.ledgersystem.Security.Services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;


@Component
@Slf4j
public class JwtUtils {

    @Value("${spring.app.jwtExpirationMs}")
    private long jwtExpirationMs;

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtCookieName}")
    private String jwtCookie;

    //Getting JWT from header
    //Old Method without cookie
    //FOR SWAGGER AS VO COOKIE NHI SAMJHTA
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken  = request.getHeader("Authorization");
        log.debug("Authorization Header: {}", bearerToken);
        if(bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); //Remove bearer prefix
        }
        return null;
    }

    public String getJwtFromCookies(HttpServletRequest request) {       //Used in AuthTokenFilter
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);  //Give us the cookie with name jwtCookie type
        if (cookie != null) {
            return cookie.getValue();
        }else{
            return null;
        }
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userDetails) { //Used in sign in
        String jwt = generateTokenFromUsername(userDetails);
        log.debug("JWT cookie generated for user: {}", userDetails.getUsername());
        ResponseCookie cookie = ResponseCookie.from(jwtCookie, jwt)
                .path("/api")  //Valid within this
                .maxAge(24*60*60)
                .httpOnly(false) //Allowing js access
                .build();
        return cookie;
    }

    public ResponseCookie getCleanCookie() { //Used in sign in
        ResponseCookie cookie = ResponseCookie.from(jwtCookie, null)
                .path("/api")  //Valid within this
                .build();
        return cookie;
    }

    //Generate token from username
    public String generateTokenFromUsername(UserDetailsImpl userDetails) {
        String username = userDetails.getUsername();
        log.debug("Generating JWT token for user: {}", username);
        return Jwts.builder()
                .subject(username) //setting data
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtExpirationMs)) //Current date + time
                .signWith(key()) //key assign kardi usse
                .compact();
    }

    //Generate Username from JWT Token
    public String getUserNameFromToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key()) //verify the key
                .build().parseSignedClaims(token) //preparing for extracting data
                .getPayload().getSubject();
    }

    //Generate Signed key
    public Key key(){
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );
    }

    //Validate JWT Token
    public boolean validateToken(String token) {
        try{
            log.debug("Validating JWT token");
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(token);
            return true;
        }catch(MalformedJwtException exception){
            log.warn("Invalid JWT token: {}", exception.getMessage());
        } catch (ExpiredJwtException e){
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e){
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e){
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
