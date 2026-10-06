package com.proyecto.servicios.security.onboarding;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    // Secreto predeterminado de al menos 256 bits para HMAC-SHA
    private static final String DEFAULT_SECRET = "mi_clave_secreta_super_segura_para_el_proyecto_bancario_onboarding_2026_jwt!";
    
    // 24 horas en milisegundos
    private static final long DEFAULT_EXPIRATION_MS = 86400000L;

    @Value("${jwt.secret:" + DEFAULT_SECRET + "}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:" + DEFAULT_EXPIRATION_MS + "}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long clienteId, String correo, String nombreCompleto) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("clienteId", clienteId);
        claims.put("correo", correo);
        claims.put("nombreCompleto", nombreCompleto);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(correo)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String correo) {
        final String username = extractUsername(token);
        return (username.equals(correo) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
