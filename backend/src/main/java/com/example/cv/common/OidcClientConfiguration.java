package com.example.cv.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class OidcClientConfiguration {

    @Bean
    @Conditional(OidcClientConfigured.class)
    ClientRegistrationRepository oidcClientRegistrationRepository(Environment environment) {
        String issuer = environment.getRequiredProperty("OIDC_ISSUER_URI");
        ClientRegistration registration = ClientRegistrations.fromIssuerLocation(issuer)
                .registrationId("oidc")
                .clientId(environment.getRequiredProperty("OIDC_CLIENT_ID"))
                .clientSecret(environment.getProperty("OIDC_CLIENT_SECRET", ""))
                .scope("openid", "profile", "email")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .clientName("OpenID Connect")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    static class OidcClientConfigured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();
            return hasText(environment.getProperty("OIDC_ISSUER_URI"))
                    && hasText(environment.getProperty("OIDC_CLIENT_ID"));
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}