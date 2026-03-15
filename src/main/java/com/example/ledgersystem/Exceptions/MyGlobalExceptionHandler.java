package com.example.ledgersystem.Exceptions;

import com.example.ledgersystem.Payloads.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class MyGlobalExceptionHandler {
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> myMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		Map<String, String> response = new HashMap<>();
		
		// Extract specific field errors
		e.getBindingResult().getAllErrors().forEach(err -> {
			String fieldName = ((FieldError) err).getField();
			String message = err.getDefaultMessage();
			response.put(fieldName, message);
		});
		log.warn("Validation failed: fields={}", response.keySet());
		
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}
	
	@ExceptionHandler(AccountNotFound.class)  //Our own excep
	public ResponseEntity<ApiResponse> ResNotFound(AccountNotFound e) {
		log.warn("Account not found: {}", e.getMessage());
		String Message = e.getMessage(); //It's in parent class RunTimeExcep that's why we used super
		ApiResponse apires = new ApiResponse(Message, false);
		return new ResponseEntity<>(apires, HttpStatus.NOT_FOUND);
	}
	
	@ExceptionHandler(APIexception.class)   //Our own excep If you try to create an already existing category
	public ResponseEntity<ApiResponse> myapiexcep(APIexception e) {
		log.warn("API exception: {}", e.getMessage());
		String Message = e.getMessage(); //It's in parent class RunTimeExcep that's why we used super
		ApiResponse apires = new ApiResponse(Message, false);
		return new ResponseEntity<>(apires, HttpStatus.BAD_REQUEST);
	}
	
	@ExceptionHandler(DuplicateTransactionException.class)   //Our own excep If you try to create an already existing category
	public ResponseEntity<ApiResponse> duplicTransac(DuplicateTransactionException e) {
		log.warn("Duplicate transaction: {}", e.getMessage());
		String Message = e.getMessage(); //It's in parent class RunTimeExcep that's why we used super
		ApiResponse apires = new ApiResponse(Message, false);
		return new ResponseEntity<>(apires, HttpStatus.CONFLICT);
	}
	
	@ExceptionHandler(InsufficientFundsException.class)   //Our own excep If you try to create an already existing category
	public ResponseEntity<ApiResponse> insufFunda(InsufficientFundsException e) {
		log.warn("Insufficient funds: {}", e.getMessage());
		String Message = e.getMessage(); //It's in parent class RunTimeExcep that's why we used super
		ApiResponse apires = new ApiResponse(Message, false);
		return new ResponseEntity<>(apires, HttpStatus.BAD_REQUEST);
	}
}
