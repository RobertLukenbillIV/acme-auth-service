package com.acme.auth.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Monitoring configuration for tracking authentication events.
 * Integrates with Micrometer for metrics collection.
 */
@Component
public class AuthenticationMonitoring {

    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;

    public AuthenticationMonitoring(MeterRegistry registry) {
        this.authSuccessCounter = Counter.builder("auth.login.success")
                .description("Number of successful authentication attempts")
                .register(registry);
        
        this.authFailureCounter = Counter.builder("auth.login.failure")
                .description("Number of failed authentication attempts")
                .tag("reason", "bad_credentials")
                .register(registry);
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        authSuccessCounter.increment();
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        authFailureCounter.increment();
    }
}
