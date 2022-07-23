package com.hwans.apiserver.common.security.jwt;

import com.hwans.apiserver.dto.authentication.TokenDto;
import io.jsonwebtoken.*;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TokenProvider implements InitializingBean {
    private static final String AUTHORITIES_KEY = "auth";

    private final String accessTokenSecretKeyBase64Secret;
    private final String refreshTokenSecretKeyBase64Secret;

    private Key accessTokenSecretKey;
    private Key refreshTokenSecretKey;

    public TokenProvider(@Value("${jwt.base64-access-secret}") String accessTokenSecretKeyBase64Secret,
                         @Value("${jwt.base64-refresh-secret}") String refreshTokenSecretKeyBase64Secret) {
        this.accessTokenSecretKeyBase64Secret = accessTokenSecretKeyBase64Secret;
        log.debug("accessTokenSecretKeyBase64Secret -> " + accessTokenSecretKeyBase64Secret);
        this.refreshTokenSecretKeyBase64Secret = refreshTokenSecretKeyBase64Secret;
        log.debug("refreshTokenSecretKeyBase64Secret -> " + refreshTokenSecretKeyBase64Secret);
    }

    @Override
    public void afterPropertiesSet() {
        byte[] accessSecretKeyBytes = Decoders.BASE64.decode(accessTokenSecretKeyBase64Secret);
        this.accessTokenSecretKey = Keys.hmacShaKeyFor(accessSecretKeyBytes);
        byte[] refreshSecretKeyBytes = Decoders.BASE64.decode(refreshTokenSecretKeyBase64Secret);
        this.refreshTokenSecretKey = Keys.hmacShaKeyFor(refreshSecretKeyBytes);
    }

    public TokenDto createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + (10 * 60 * 1000L));
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .signWith(accessTokenSecretKey, SignatureAlgorithm.HS256)
                .setExpiration(accessTokenExpiresIn)
                .compact();

        Date refreshTokenExpiresIn = new Date(now + (60 * 60 * 24 * 30 * 1000L));
        String refreshToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .signWith(refreshTokenSecretKey, SignatureAlgorithm.HS256)
                .setExpiration(accessTokenExpiresIn)
                .compact();

        return TokenDto.builder()
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(refreshTokenExpiresIn.getTime())
                .build();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(accessTokenSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public JwtStatus validateAccessToken(String token) {
        return validateToken(token, accessTokenSecretKey);
    }

    public JwtStatus validateRefreshToken(String token) {
        return validateToken(token, refreshTokenSecretKey);
    }

    private JwtStatus validateToken(String token, Key secretKey) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return JwtStatus.ACCESS;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT signature.");
            log.trace("Invalid JWT signature trace: {}", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token.");
            log.trace("Expired JWT token trace: {}", e);
            return JwtStatus.EXPIRED;
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token.");
            log.trace("Unsupported JWT token trace: {}", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT token compact of handler are invalid.");
            log.trace("JWT token compact of handler are invalid trace: {}", e);
        }
        return JwtStatus.DENIED;
    }

    public void validateTokenWithThrow(String token) {
        Jwts.parserBuilder().setSigningKey(accessTokenSecretKey).build().parseClaimsJws(token);
    }

    public String extractToken(String token) {
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }
}
