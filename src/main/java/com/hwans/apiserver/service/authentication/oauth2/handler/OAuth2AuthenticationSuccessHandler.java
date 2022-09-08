package com.hwans.apiserver.service.authentication.oauth2.handler;

import com.hwans.apiserver.common.security.jwt.JwtTokenProvider;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.service.authentication.oauth2.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider tokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        var customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        var providerAttributes = customOAuth2User.getProviderAttributes();

        if(customOAuth2User.isNeedRegister()) {
            addCookie(response, "registerToken", "!!", 300);
        } else {
            var token = tokenProvider.createToken(providerAttributes.getEmail(), customOAuth2User.getAuthorities());
            addCookie(response, "accessToken", token.getAccessToken(), 300);
            addCookie(response, "refreshToken", token.getRefreshToken(), 300);
        }

        return UriComponentsBuilder.fromUriString("/")
                .queryParam("email", customOAuth2User.getName())
                .build().toUriString();
    }

    protected static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);

        response.addCookie(cookie);
    }
}
