package nl.itqaanconsulting.agentready.zaak;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("gateway.zaaksysteem")
public record ZaaksysteemProperties(
        String url,
        Duration connectTimeout,
        Duration readTimeout
) {

    public ZaaksysteemProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(3) : readTimeout;
    }
}
