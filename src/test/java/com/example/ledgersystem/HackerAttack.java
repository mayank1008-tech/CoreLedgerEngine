package com.example.ledgersystem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.*; // You might need to add 'okhttp' dependency or use standard HttpURLConnection
//Will send 10 trannsfer request at a time and verify the optimistic locking and retry is working
public class HackerAttack {
	
	// UPDATE THESE WITH REAL IDs FROM YOUR DB
	private static final String ALICE_ID = "cb371150-6eb8-4808-a9a9-22e741bb97e3";
	private static final String BOB_ID = "034af214-1ae1-4de8-970b-87dca1b10ee3";
	private static final String CHARLIE_ID = "0c02f408-3ccc-4809-a167-06c24f0aa7a2";
	private static final String ALICE_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsImlhdCI6MTc2OTg4NzM5NywiZXhwIjozMDE3Njk4ODczOTd9.DzBkhwLFmB4A2eIVBno_o0hzzQHHeNsVrC8Y2tl8_uc";
	
	public static void main(String[] args) throws InterruptedException {
		// We will fire 10 requests at the EXACT same time
		int numberOfThreads = 10;
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
		
		System.out.println("🔥 STARTING DOUBLE-SPEND ATTACK...");
		
		for (int i = 0; i < numberOfThreads; i++) {
			int finalI = i;
			executorService.submit(() -> {
				try {
					sendTransferRequest(finalI);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
		
		executorService.shutdown();
		executorService.awaitTermination(1, TimeUnit.MINUTES);
		System.out.println("✅ ATTACK FINISHED. CHECK ALICE'S BALANCE!");
	}
	
	private static void sendTransferRequest(int index) throws Exception {
		OkHttpClient client = new OkHttpClient();
		
		// JSON Body: Transfer 100.00
		String json = "{"
				+ "\"fromAccountId\": \"" + ALICE_ID + "\","
				+ "\"toAccountId\": \"" + BOB_ID + "\","
				+ "\"amount\": 1500.00,"
				+ "\"referenceId\": \"HACK_ATTACK_" + System.currentTimeMillis() + "_" + index + "\""
				+ "}";
		
		RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
		Request request = new Request.Builder()
				.url("http://localhost:8080/api/transfer")
				.post(body)
				.addHeader("Authorization", "Bearer " + ALICE_TOKEN)
				.build();
		
		Response response = client.newCall(request).execute();
		System.out.println("Thread " + index + ": " + response.code() + " " + response.message());
	}
}
