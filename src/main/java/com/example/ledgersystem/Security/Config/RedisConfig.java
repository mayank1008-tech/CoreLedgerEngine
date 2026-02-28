package com.example.ledgersystem.Security.Config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {
	
	// ADD THESE FIELDS
	@Value("${spring.data.redis.host:localhost}")
	private String redisHost;
	
	@Value("${spring.data.redis.port:6379}")
	private int redisPort;
	
	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		config.setHostName(redisHost);
		config.setPort(redisPort);
		
		return new LettuceConnectionFactory(config);
	}
	
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(RedisSerializer.json());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(RedisSerializer.json());
		
		return template;
	}
	
	@Bean(destroyMethod = "shutdown")
	public RedisClient bucket4jRedisClient() {
		RedisURI redisUri = RedisURI.builder()
				.withHost(redisHost)
				.withPort(redisPort)
				.build();
		
		return RedisClient.create(redisUri);
	}
	
	@Bean(destroyMethod = "close")
	public StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(RedisClient bucket4jRedisClient) {
		return bucket4jRedisClient.connect(RedisCodec.of(
				StringCodec.UTF8,
				ByteArrayCodec.INSTANCE
		));
	}
	
	@Bean
	public ProxyManager<String> proxyManager(StatefulRedisConnection<String, byte[]> bucket4jRedisConnection) {
		return Bucket4jLettuce.casBasedBuilder(bucket4jRedisConnection)
				.expirationAfterWrite(ExpirationAfterWriteStrategy
						.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
				.build();
	}
}
