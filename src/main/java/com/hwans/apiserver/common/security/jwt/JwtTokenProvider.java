package com.hwans.apiserver.common.security.jwt;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.authentication.TokenDto;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider implements InitializingBean {
    private static final String AUTHORITIES_KEY = "auth";
    private final String accessTokenSecretKeyBase64Secret;
    private final String refreshTokenSecretKeyBase64Secret;

    private Key accessTokenSecretKey;
    private Key refreshTokenSecretKey;

    public JwtTokenProvider(@Value("${jwt.base64-access-secret}") String accessTokenSecretKeyBase64Secret,
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
        String authorities = authentication
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        var userAuthenticationDetails = (UserAuthenticationDetails) authentication.getPrincipal();
        return createToken(userAuthenticationDetails.getEmail(), authorities);
    }

    public TokenDto createToken(String accountEmail, String authorities) {
        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + Constants.ACCESS_TOKEN_EXPIRES_TIME);
        String accessToken = Jwts.builder()
                .setSubject(accountEmail)
                .claim(AUTHORITIES_KEY, authorities)
                .signWith(accessTokenSecretKey, SignatureAlgorithm.HS256)
                .setExpiration(accessTokenExpiresIn)
                .compact();

        Date refreshTokenExpiresIn = new Date(now + Constants.REFRESH_TOKEN_EXPIRES_TIME);
        String refreshToken = Jwts.builder()
                .setSubject(accountEmail)
                .signWith(refreshTokenSecretKey, SignatureAlgorithm.HS256)
                .setExpiration(refreshTokenExpiresIn)
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

        var principal = new UserAuthenticationDetails(claims.getSubject(), authorities);

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

    public String extractTokenFromHeader(String token) {
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    public Optional<String> getAccountEmailFromAccessToken(String accessToken) {
        try {
            return Optional.ofNullable(
                    Jwts
                            .parserBuilder()
                            .setSigningKey(accessTokenSecretKey)
                            .build()
                            .parseClaimsJws(accessToken)
                            .getBody()
                            .getSubject());

        } catch (ExpiredJwtException | SecurityException | MalformedJwtException | UnsupportedJwtException |
                 IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<String> getAccountEmailForReissueToken(String accessToken, String refreshToken) {
        try {
            var refreshClaims = Jwts
                    .parserBuilder()
                    .setSigningKey(refreshTokenSecretKey)
                    .build()
                    .parseClaimsJws(refreshToken);
            try {
                return Optional.ofNullable(Jwts.parserBuilder()
                        .requireSubject(refreshClaims.getBody().getSubject())
                        .setSigningKey(accessTokenSecretKey)
                        .build()
                        .parseClaimsJws(accessToken)
                        .getBody()
                        .getSubject());
            } catch (ExpiredJwtException e) {
                return Optional.ofNullable(e.getClaims().getSubject());
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
