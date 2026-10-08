package com.notesapi.notes_api.auth.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.notesapi.notes_api.user.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    public enum TokenType {
        ACCESS, REFRESH
    }

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.in.minutes}")
    private int JWT_EXPIRATION_IN_MINUTES;

    @Value("${jwt.refresh.expiration.in.days}")
    private int JWT_REFRESH_EXPIRATION_IN_DAYS;

    private static final String ISSUER = "notes-api";

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(secret);
    }

    public Long getRefreshTokenExpirationInSeconds() {
        return JWT_REFRESH_EXPIRATION_IN_DAYS * 24 * 60 * 60L;
    }

    private Instant getNowInTimestamp() {
        return Instant.now();
    }

    private Instant generateExpiresAt(TokenType type) {
        if (type == TokenType.ACCESS) {
            return getNowInTimestamp().plus(Duration.ofMinutes(JWT_EXPIRATION_IN_MINUTES));
        }
        return getNowInTimestamp().plus(Duration.ofDays(JWT_REFRESH_EXPIRATION_IN_DAYS));
    }

    public String generateAccess(User user) {
        try {
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(user.getEmail())
                    .withClaim("type", TokenType.ACCESS.name().toLowerCase())
                    .withIssuedAt(getNowInTimestamp())
                    .withExpiresAt(generateExpiresAt(TokenType.ACCESS))
                    .sign(getAlgorithm());
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error ao gerar access token: ", exception);
        }
    }

    public String generateRefresh(User user) {
        try {
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(user.getEmail())
                    .withClaim("type", TokenType.REFRESH.name().toLowerCase())
                    .withIssuedAt(getNowInTimestamp())
                    .withExpiresAt(generateExpiresAt(TokenType.REFRESH))
                    .sign(getAlgorithm());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error ao gerar refresh token: ", e);
        }
    }

    public String validate(String token, TokenType expectedType) {
        try {
            return JWT.require(getAlgorithm())
                    .withIssuer(ISSUER)
                    .withClaim("type", expectedType.name().toLowerCase())
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public boolean needsNewRefresh(String token) {
        try {
            var decodedJWT = JWT.decode(token);
            Instant issuedAt = decodedJWT.getIssuedAtAsInstant();
            Instant expiresAt = decodedJWT.getExpiresAtAsInstant();

            if (issuedAt == null || expiresAt == null) {
                return true;
            }

            Duration totalLife = Duration.between(issuedAt, expiresAt);
            Duration halfLife = totalLife.dividedBy(2);
            Instant midPoint = issuedAt.plus(halfLife);

            return Instant.now().isAfter(midPoint);
        } catch (JWTDecodeException e) {
            return true;
        }
    }

}
