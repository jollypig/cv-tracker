package com.example.cv.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import com.example.cv.auth.AuthenticatedUserService;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                ObjectProvider<AuthenticatedUserService> authenticatedUsers,
                    ObjectProvider<OwnershipAuthorizationManager> ownershipManager,
                    @Value("${app.frontend.url:http://localhost:5173/}") String frontendUrl) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/status",
                                "/api/v1/auth/config",
                                "/api/v1/public/cv-shares/*",
                                "/actuator/health/**",
                                "/actuator/prometheus",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/oauth2/**",
                                "/login/**",
                                "/error")
                        .permitAll()
                        .requestMatchers("/api/v1/cvs/import")
                        .authenticated()
                        .requestMatchers("/api/v1/cvs/import/*", "/api/v1/cvs/import/*/**")
                        .access((authentication, context) -> {
                            OwnershipAuthorizationManager manager = ownershipManager.getIfAvailable();
                            return manager == null
                                    ? new AuthorizationDecision(false)
                                    : manager.check(authentication, context);
                        })
                        .requestMatchers(
                                "/api/v1/persons/*",
                                "/api/v1/persons/*/**",
                                "/api/v1/cvs",
                                "/api/v1/cvs/*",
                                "/api/v1/cvs/*/**",
                                "/api/v1/cv-versions/*/**",
                                "/api/v1/exports/*/**")
                        .access((authentication, context) -> {
                            OwnershipAuthorizationManager manager = ownershipManager.getIfAvailable();
                            return manager == null
                                    ? new AuthorizationDecision(false)
                                    : manager.check(authentication, context);
                        })
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .cors(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value()))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())));

        if (clientRegistrations.getIfAvailable() != null) {
            OidcUserService delegate = new OidcUserService();
            http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(request -> {
                        OidcUser user = delegate.loadUser(request);
                        AuthenticatedUserService service = authenticatedUsers.getIfAvailable();
                        if (service != null) {
                            service.synchronize(user);
                        }
                        return user;
                                        }))
                                        .defaultSuccessUrl(frontendUrl, true));
        }

        return http.build();
    }

        private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

                private final CsrfTokenRequestAttributeHandler plain = new CsrfTokenRequestAttributeHandler();
                private final XorCsrfTokenRequestAttributeHandler xor = new XorCsrfTokenRequestAttributeHandler();

                @Override
                public void handle(HttpServletRequest request, HttpServletResponse response,
                                Supplier<CsrfToken> csrfToken) {
                        xor.handle(request, response, csrfToken);
                        csrfToken.get();
                }

                @Override
                public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
                        String headerValue = request.getHeader(csrfToken.getHeaderName());
                        return StringUtils.hasText(headerValue)
                                        ? plain.resolveCsrfTokenValue(request, csrfToken)
                                        : xor.resolveCsrfTokenValue(request, csrfToken);
                }
        }
}