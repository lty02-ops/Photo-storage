package com.photostorage.api.config;

import com.photostorage.api.service.AuthService;
import com.photostorage.api.common.CurrentUserArgumentResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final boolean oauth2Enabled;

    public SecurityConfig(@Value("${app.oauth2.enabled:false}") boolean oauth2Enabled) {
        this.oauth2Enabled = oauth2Enabled;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        AuthTokenFilter authTokenFilter,
        OAuth2LoginSuccessHandler oauth2LoginSuccessHandler
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {
            })
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        if (oauth2Enabled) {
            http.oauth2Login(oauth2 -> oauth2.successHandler(oauth2LoginSuccessHandler));
        }

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    WebMvcConfigurer webMvcConfigurer(CurrentUserArgumentResolver currentUserArgumentResolver) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(currentUserArgumentResolver);
            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "http://localhost:3000",
                        "http://127.0.0.1:3000",
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://localhost:5174",
                        "http://127.0.0.1:5174"
                    )
                    .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                    .allowedHeaders("*");
            }
        };
    }

    @Bean
    AuthTokenFilter authTokenFilter(AuthService authService) {
        return new AuthTokenFilter(authService);
    }

    static class AuthTokenFilter extends OncePerRequestFilter {

        private final AuthService authService;

        AuthTokenFilter(AuthService authService) {
            this.authService = authService;
        }

        @Override
        protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
        ) throws ServletException, IOException {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                authService.authenticate(token)
                    .ifPresent(user -> request.setAttribute(CurrentUserArgumentResolver.REQUEST_ATTRIBUTE, user));
            }
            filterChain.doFilter(request, response);
        }
    }
}
