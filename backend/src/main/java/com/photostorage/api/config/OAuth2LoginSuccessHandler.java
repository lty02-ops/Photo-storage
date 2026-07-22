package com.photostorage.api.config;

import com.photostorage.api.dto.AuthResponse;
import com.photostorage.api.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
        AuthService authService,
        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl
    ) {
        this.authService = authService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            throw new ServletException("Unsupported OAuth2 authentication.");
        }

        String provider = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User principal = oauthToken.getPrincipal();
        SocialProfile profile = extractProfile(provider, principal.getAttributes());
        AuthResponse authResponse = authService.oauthLogin(provider, profile.providerUserId(), profile.email());

        String redirectUrl = frontendUrl + "/oauth/callback"
            + "?token=" + encode(authResponse.token())
            + "&email=" + encode(authResponse.user().email())
            + "&role=" + encode(authResponse.user().role())
            + "&id=" + authResponse.user().id();

        response.sendRedirect(redirectUrl);
    }

    private SocialProfile extractProfile(String provider, Map<String, Object> attributes) throws ServletException {
        return switch (provider) {
            case "google" -> new SocialProfile(
                requiredString(attributes, "sub", "Google account id is missing."),
                requiredString(attributes, "email", "Google email is missing.")
            );
            case "naver" -> {
                Object response = attributes.get("response");
                if (!(response instanceof Map<?, ?> profile)) {
                    throw new ServletException("Naver profile is missing.");
                }
                yield new SocialProfile(
                    requiredString(profile, "id", "Naver account id is missing."),
                    requiredString(profile, "email", "Naver email is missing.")
                );
            }
            case "kakao" -> {
                Object account = attributes.get("kakao_account");
                if (!(account instanceof Map<?, ?> profile)) {
                    throw new ServletException("Kakao account profile is missing.");
                }
                yield new SocialProfile(
                    requiredString(attributes, "id", "Kakao account id is missing."),
                    requiredString(profile, "email", "Kakao email is missing.")
                );
            }
            default -> throw new ServletException("Unsupported OAuth2 provider: " + provider);
        };
    }

    private String requiredString(Map<?, ?> attributes, String key, String message) throws ServletException {
        Object value = attributes.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ServletException(message);
        }
        return value.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record SocialProfile(String providerUserId, String email) {
    }
}
