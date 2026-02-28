package com.example.ledgersystem.controller;

import com.example.ledgersystem.Payloads.AccountCacheDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
public class SerializerTestController {
	
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	
	@GetMapping("/test-serializer")
	public String testSerializer() {
		String key = "test:serializer";
		
		// 1. Create a complex object
		AccountCacheDTO originalDto = new AccountCacheDTO(UUID.randomUUID(), new BigDecimal("500.00"));
		
		// 2. Save it to Redis
		System.out.println("Saving: " + originalDto);
		redisTemplate.opsForValue().set(key, originalDto);
		
		// 3. Try to Read it back
		Object fromRedis = redisTemplate.opsForValue().get(key);
		System.out.println("Raw Object from Redis: " + fromRedis.getClass().getName());
		
		try {
			// 4. THE MOMENT OF TRUTH: Try to cast it
			AccountCacheDTO loadedDto = (AccountCacheDTO) fromRedis;
			return "✅ SUCCESS! Serializer is working. Loaded: " + loadedDto.getBalance();
		} catch (ClassCastException e) {
			// 5. If this happens, your config is broken
			e.printStackTrace();
			return "❌ FAIL! Serializer broke. It returned a LinkedHashMap instead of AccountCacheDTO.";
		}
	}
}
