package nl.itqaanconsulting.agentready.governance;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * The agent registry, keyed by OAuth2 client id. An agent that is not registered here gets nothing,
 * even with a valid token: deny by default.
 */
@ConfigurationProperties("gateway")
public record AgentRegistryProperties(Map<String, AgentPolicy> agents) {

    public AgentRegistryProperties {
        agents = agents == null ? Map.of() : Map.copyOf(agents);
    }
}
