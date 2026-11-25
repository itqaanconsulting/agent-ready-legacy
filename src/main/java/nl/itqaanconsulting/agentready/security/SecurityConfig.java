package nl.itqaanconsulting.agentready.security;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/zaken/**").hasAuthority("SCOPE_zaken.lezen")
                        .requestMatchers("/api/v1/audit/**").hasAuthority("SCOPE_audit.lezen")
                        // AI agents: the MCP endpoint. Per-tool rules are enforced by AgentPolicy.
                        .requestMatchers("/mcp", "/mcp/**").hasAuthority("SCOPE_mcp")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }

    /** Local development and tests: HS256 tokens signed with a shared secret. Production uses issuer-uri. */
    @Bean
    @ConditionalOnProperty("gateway.security.dev-jwt-secret")
    JwtDecoder devJwtDecoder(@Value("${gateway.security.dev-jwt-secret}") String secret) {
        LoggerFactory.getLogger(SecurityConfig.class).warn(
                "Accepting tokens signed with the development secret. Never enable this outside local development.");
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .build();
    }
}
