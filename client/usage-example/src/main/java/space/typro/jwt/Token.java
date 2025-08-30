package space.typro.jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Record for immutability, matching your server's Token class structure
public record Token(UUID id, String subject, List<String> authorities, Instant createdAt, Instant expiresAt) {
}