package com.example.ledgersystem.tools;

import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;

public class HackerAttack {
	
	private static final MediaType JSON = MediaType.parse("application/json");
	
	public static void main(String[] args) throws InterruptedException {
		String baseUrl = env("LEDGER_BASE_URL", "http://localhost:80");
		String fromAccountId = requiredEnv("ATTACK_FROM_ACCOUNT_ID"); // Alice accountId
		String toAccountId = requiredEnv("ATTACK_TO_ACCOUNT_ID");     // Bob accountId
		String bearerToken = requiredEnv("ATTACK_BEARER_TOKEN");      // JWT
		
		int threads = Integer.parseInt(env("ATTACK_THREADS", "10"));
		String amount = env("ATTACK_AMOUNT", "1500.00");
		
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		
		System.out.printf("Starting attack: threads=%d amount=%s baseUrl=%s%n", threads, amount, baseUrl);
		
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		
		for (int i = 0; i < threads; i++) {
			int idx = i;
			pool.submit(() -> {
				ready.countDown();
				try {
					start.await(10, TimeUnit.SECONDS);
					sendTransferRequest(baseUrl, fromAccountId, toAccountId, bearerToken, amount, idx);
				} catch (Exception e) {
					System.err.println("Thread " + idx + " failed: " + e.getMessage());
				}
			});
		}
		
		ready.await(10, TimeUnit.SECONDS);
		System.out.println("All threads ready. Firing requests...");
		start.countDown();
		
		pool.shutdown();
		pool.awaitTermination(2, TimeUnit.MINUTES);
		System.out.println("Attack finished. Check sender balance + ledger integrity.");
	}
	
	private static void sendTransferRequest(
			String baseUrl,
			String fromAccountId,
			String toAccountId,
			String bearerToken,
			String amount,
			int index
	) throws IOException {
		
		OkHttpClient client = new OkHttpClient.Builder()
				.callTimeout(Duration.ofSeconds(20))
				.connectTimeout(Duration.ofSeconds(10))
				.readTimeout(Duration.ofSeconds(20))
				.build();
		
		String refId = "HACK_ATTACK_" + System.currentTimeMillis() + "_" + index + "_" + UUID.randomUUID();
		
		String json = "{"
				+ "\"fromAccountId\": \"" + fromAccountId + "\","
				+ "\"toAccountId\": \"" + toAccountId + "\","
				+ "\"amount\": " + amount + ","
				+ "\"referenceId\": \"" + refId + "\""
				+ "}";
		
		RequestBody body = RequestBody.create(json, JSON);
		
		Request request = new Request.Builder()
				.url(baseUrl + "/api/transfer")
				.post(body)
				.addHeader("Authorization", "Bearer " + bearerToken)
				.build();
		
		try (Response response = client.newCall(request).execute()) {
			System.out.println("Thread " + index + " -> HTTP " + response.code() + " refId=" + refId);
		}
	}
	
	private static String env(String name, String defaultValue) {
		String v = System.getenv(name);
		return (v == null || v.isBlank()) ? defaultValue : v.trim();
	}
	
	private static String requiredEnv(String name) {
		String v = System.getenv(name);
		if (v == null || v.isBlank()) {
			throw new IllegalArgumentException("Missing required env var: " + name);
		}
		return v.trim();
	}
}
