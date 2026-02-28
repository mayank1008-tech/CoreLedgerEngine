package com.example.ledgersystem.controller;

import com.example.ledgersystem.Security.Request.LoginRequestDTO;
import com.example.ledgersystem.Security.Request.SignUpRequest;
import com.example.ledgersystem.Security.Response.MessageResponse;
import com.example.ledgersystem.Security.Response.UserInfoResponse;
import com.example.ledgersystem.Security.Services.UserDetailsImpl;
import com.example.ledgersystem.Security.jwt.JwtUtils;
import com.example.ledgersystem.model.User;
import com.example.ledgersystem.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequestDTO loginRequestDTO){
        Authentication authentication;  //auth obj
        try{
            authentication = authenticationManager.authenticate(  //.authenticate will check the username and password with the obj provided, and then it will load auth obj with userdetails if correct
                    new UsernamePasswordAuthenticationToken(loginRequestDTO.getUsername(), loginRequestDTO.getPassword()) //Usernamepasstokken is used to describe username pass
            );
        }catch(AuthenticationException e){
            Map<String , Object> map = new HashMap<>();
            map.put("message", "Invalid username or password");
            map.put("status", false);

            return new ResponseEntity<Object>(map, HttpStatus.UNAUTHORIZED);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);  //logs the user for current login request

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal(); //auth obj se data le liya

        ResponseCookie cookie = jwtUtils.generateJwtCookie(userDetails); //generating cookie
        String tokenString = cookie.getValue();
	    
	    assert userDetails != null;
	    UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), tokenString);

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest){

        //Checking for already existing account
        //Checking for already existing account
        if(userRepository.existsByUsername(signUpRequest.getUsername())){
            // FIX: Send the object, not the string
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if(userRepository.existsByEmail(signUpRequest.getEmail())){
            // FIX: Send the object, not the string
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        //Saving User
        User user = new User(  //Creating user object for saving
                signUpRequest.getUsername(),
                passwordEncoder.encode(signUpRequest.getPassword()),
                signUpRequest.getEmail()
        );
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }


    //For Profile pages

    @GetMapping("/username")
    public String currentUsername(Authentication authentication){   //Auth ka object hamesha data store karke rkhta hai
        //as vo ContextHolder mai save rhta har request ke liye
        if(authentication != null){
            return authentication.getName();
        }
        return null;
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication){
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
	    assert userDetails != null;
	    UserInfoResponse response = new UserInfoResponse(userDetails.getId(), userDetails.getUsername());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser(){
        ResponseCookie cookie = jwtUtils.getCleanCookie();
        return  ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                cookie.toString()).body(new MessageResponse("Successfully logged out!"));
    }

}

