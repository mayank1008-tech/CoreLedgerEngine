package com.example.ledgersystem.security;

import com.example.ledgersystem.Security.jwt.AuthEntryPointJwt;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthEntryPointJwtTest {
	
	@Test
	void commence_shouldReturn401JsonBody() throws Exception {
		AuthEntryPointJwt entryPoint = new AuthEntryPointJwt();
		
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		AuthenticationException authException = mock(AuthenticationException.class);
		
		when(request.getServletPath()).thenReturn("/api/transfer");
		when(authException.getMessage()).thenReturn("Bad credentials");
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		when(response.getOutputStream()).thenReturn(new TestServletOutputStream(out));
		
		entryPoint.commence(request, response, authException);
		
		verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
		verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		
		String body = out.toString(StandardCharsets.UTF_8);
		assertTrue(body.contains("\"status\":401"));
		assertTrue(body.contains("\"error\":\"Authentication Failed\""));
		assertTrue(body.contains("\"message\":\"Bad credentials\""));
		assertTrue(body.contains("\"path\":\"/api/transfer\""));
	}
	
	// Minimal ServletOutputStream wrapper to capture written bytes
	static class TestServletOutputStream extends ServletOutputStream {
		private final ByteArrayOutputStream delegate;
		
		TestServletOutputStream(ByteArrayOutputStream delegate) {
			this.delegate = delegate;
		}
		
		@Override
		public void write(int b) throws IOException {
			delegate.write(b);
		}
		
		@Override
		public boolean isReady() {
			return true;
		}
		
		@Override
		public void setWriteListener(WriteListener writeListener) {
			// not needed for unit tests
		}
	}
}
