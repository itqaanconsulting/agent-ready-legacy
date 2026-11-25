package nl.itqaanconsulting.agentready;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgentReadyLegacyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentReadyLegacyApplication.class, args);
    }
}
