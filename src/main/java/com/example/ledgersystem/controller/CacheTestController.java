package com.example.ledgersystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CacheTestController {
	
	@Autowired
	private StringRedisTemplate redisTemplate; // Spring's helper to talk to Redis
	
	@GetMapping("/test-cache/{value}")
	public String addToCache(@PathVariable String value) {
		// 1. Save to Redis (Key: "myKey", Value: whatever you typed)
		redisTemplate.opsForValue().set("myKey", value);
		return "Saved to Redis!";
	}
	
	@GetMapping("/get-cache")
	public String getFromCache() {
		// 2. Read from Redis
		String value = redisTemplate.opsForValue().get("myKey");
		return "Retrieved from Redis: " + value;
	}
}
