package com.hwans.apiserver.common.security.jwt;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.authentication.TokenDto;
import com.hwans.apiserver.entity.account.role.RoleType;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
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

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JWT 토큰 관련 기능을 제공하는 Provider (JJWT 0.12+ API)
 */
@Component
@Slf4j
public class JwtTokenProvider implements InitializingBean {
    private static final String AUTHORITIES_KEY = "auth";
    private final String accessTokenSecretKeyBase64Secret;
    private final String refreshTokenSecretKeyBase64Secret;
    private final String registerTokenSecretKeyBase64Secret;

    private SecretKey accessTokenSecretKey;
    private SecretKey refreshTokenSecretKey;
    private SecretKey registerTokenSecretKey;

    public JwtTokenProvider(@Value("${jwt.base64-access-secret}") String accessTokenSecretKeyBase64Secret,
                            @Value("${jwt.base64-refresh-secret}") String refreshTokenSecretKeyBase64Secret,
                            @Value("${jwt.base64-register-secret}") String registerTokenSecretKeyBase64Secret) {
        this.accessTokenSecretKeyBase64Secret = accessTokenSecretKeyBase64Secret;
        this.refreshTokenSecretKeyBase64Secret = refreshTokenSecretKeyBase64Secret;
        this.registerTokenSecretKeyBase64Secret = registerTokenSecretKeyBase64Secret;
    }

    @Override
    public void afterPropertiesSet() {
        this.accessTokenSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessTokenSecretKeyBase64Secret));
        this.refreshTokenSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshTokenSecretKeyBase64Secret));
        this.registerTokenSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(registerTokenSecretKeyBase64Secret));
    }

    public TokenDto createToken(Authentication authentication) {
        var authorities = authentication
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        var userAuthenticationDetails = (UserAuthenticationDetails) authentication.getPrincipal();
        return createToken(userAuthenticationDetails.getEmail(), authorities);
    }

    public TokenDto createToken(String accountEmail, Collection<? extends GrantedAuthority> authorities) {
        return createToken(accountEmail, authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(",")));
    }

    public TokenDto createToken(String accountEmail, String authorities) {
        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + Constants.ACCESS_TOKEN_EXPIRES_TIME);
        String accessToken = Jwts.builder()
                .subject(accountEmail)
                .claim(AUTHORITIES_KEY, authorities)
                .expiration(accessTokenExpiresIn)
                .signWith(accessTokenSecretKey)
                .compact();

        Date refreshTokenExpiresIn = new Date(now + Constants.REFRESH_TOKEN_EXPIRES_TIME);
        String refreshToken = Jwts.builder()
                .subject(accountEmail)
                .expiration(refreshTokenExpiresIn)
                .signWith(refreshTokenSecretKey)
                .compact();

        return TokenDto.builder()
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(refreshTokenExpiresIn.getTime())
                .build();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parse(accessTokenSecretKey, token);

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        var principal = new UserAuthenticationDetails(claims.getSubject(), authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public String createRegisterToken(String email) {
        long now = (new Date()).getTime();
        Date registerTokenExpiresIn = new Date(now + Constants.REGISTER_TOKEN_EXPIRES_TIME);
        return Jwts.builder()
                .subject(email)
                .claim(AUTHORITIES_KEY, RoleType.SOCIAL.getName())
                .expiration(registerTokenExpiresIn)
                .signWith(registerTokenSecretKey)
                .compact();
    }

    public String createPasswordResetToken(String email) {
        long now = (new Date()).getTime();
        Date passwordResetTokenExpiresIn = new Date(now + Constants.PASSWORD_RESET_TOKEN_EXPIRES_TIME);
        return Jwts.builder()
                .subject(email)
                .claim(AUTHORITIES_KEY, RoleType.GUEST.getName())
                .expiration(passwordResetTokenExpiresIn)
                .signWith(registerTokenSecretKey)
                .compact();
    }

    public JwtStatus validateAccessToken(String token) {
        return validateToken(token, accessTokenSecretKey);
    }

    public JwtStatus validateRefreshToken(String token) {
        return validateToken(token, refreshTokenSecretKey);
    }

    private JwtStatus validateToken(String token, SecretKey secretKey) {
        try {
            parse(secretKey, token);
            return JwtStatus.ACCESS;
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token.");
            log.trace("Expired JWT token trace: {}", e.getMessage());
            return JwtStatus.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치, 형식 오류, 지원하지 않는 토큰, 빈 토큰
            log.info("Invalid JWT token: {}", e.getClass().getSimpleName());
            log.trace("Invalid JWT token trace: {}", e.getMessage());
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
        return getAccountEmailFromToken(accessTokenSecretKey, accessToken);
    }

    public Optional<String> getAccountEmailFromRegisterToken(String registerToken) {
        return getAccountEmailFromToken(registerTokenSecretKey, registerToken);
    }

    public Optional<String> getAccountEmailFromResetPasswordToken(String resetPasswordToken) {
        return getAccountEmailFromToken(registerTokenSecretKey, resetPasswordToken);
    }

    /**
     * 재발급: 리프레시 토큰이 유효하고, 액세스 토큰이 (만료되었더라도) 같은 주체의 것일 때 그 이메일을 돌려준다.
     */
    public Optional<String> getAccountEmailForReissueToken(String accessToken, String refreshToken) {
        try {
            var refreshSubject = parse(refreshTokenSecretKey, refreshToken).getSubject();
            try {
                return Optional.ofNullable(Jwts.parser()
                        .requireSubject(refreshSubject)
                        .verifyWith(accessTokenSecretKey)
                        .build()
                        .parseSignedClaims(accessToken)
                        .getPayload()
                        .getSubject());
            } catch (ExpiredJwtException e) {
                return Optional.ofNullable(e.getClaims().getSubject());
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<String> getAccountEmailFromToken(SecretKey signingKey, String token) {
        try {
            return Optional.ofNullable(parse(signingKey, token).getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * 서명을 검증하고 클레임을 돌려준다. 만료·서명 오류 등은 JwtException 으로 던진다.
     */
    private static Claims parse(SecretKey key, String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
