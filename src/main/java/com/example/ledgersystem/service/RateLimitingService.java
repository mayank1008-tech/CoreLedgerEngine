package com.example.ledgersystem.service;

import com.example.ledgersystem.enums.RateLimitType;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@Slf4j
public class RateLimitingService {
	
	@Autowired
	private ProxyManager<String> proxyManager;
	
	public Bucket resolveBucket(UUID userId, RateLimitType type) {
		// 1. DIFFERENT KEYS: "rate_limit:GENERAL:uuid" vs "rate_limit:TRANSACTION:uuid"
		String key = "rate_limit:" + type.name() + ":" + userId.toString();
		log.debug("Resolving rate limit bucket: userId={}, type={}, key={}", userId, type, key);
		
		return proxyManager.builder().build(key, () -> getConfig(type));
	}
	
	private BucketConfiguration getConfig(RateLimitType type) {
		log.debug("Creating new rate limit bucket config: type={}", type);
		if (type == RateLimitType.TRANSACTION) {
			// 🛡️ STRICT: 1 request per minute (No bursts)
			return BucketConfiguration.builder()
					.addLimit(Bandwidth.classic(1, Refill.intervally(1, Duration.ofMinutes(1))))
					.build();
		} else {
			// 📖 LOOSE: 10 requests per minute (Bursts allowed)
			return BucketConfiguration.builder()
					.addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
					.build();
		}
	}
}
