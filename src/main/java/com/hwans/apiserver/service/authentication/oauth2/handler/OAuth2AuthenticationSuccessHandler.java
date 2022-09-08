package com.hwans.apiserver.service.authentication.oauth2.handler;

import com.hwans.apiserver.common.security.jwt.JwtTokenProvider;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.service.authentication.oauth2.CustomOAuth2User;
import com.hwans.apiserver.service.authentication.oauth2.utils.CookieUtils;
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
import java.net.URI;
import java.util.Optional;

import static com.hwans.apiserver.service.authentication.oauth2.repository.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
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
        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        var customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        var providerAttributes = customOAuth2User.getProviderAttributes();

        if (customOAuth2User.isNeedRegister()) {
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("needRegister", customOAuth2User.isNeedRegister())
                    .queryParam("registerToken", customOAuth2User.getRegisterToken())
                    .queryParam("email", providerAttributes.getEmail())
                    .queryParam("name", providerAttributes.getName())
                    .queryParam("profileImageUrl", providerAttributes.getProfileImagelUrl())
                    .build().toUriString();
        } else {
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("needRegister", customOAuth2User.isNeedRegister())
                    .queryParam("accessToken", customOAuth2User.getAccessToken())
                    .queryParam("refreshToken", customOAuth2User.getRefreshToken())
                    .build().toUriString();
        }
    }
}
