package nl.itqaanconsulting.agentready.governance;

import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Set;

/** What one registered agent may do: which tools, for which purposes, and how often. */
public record AgentPolicy(
        Set<String> tools,
        Set<Doel> doelen,
        @DefaultValue("30") int requestsPerMinute
) {
    public AgentPolicy {
        tools = tools == null ? Set.of() : Set.copyOf(tools);
        doelen = doelen == null ? Set.of() : Set.copyOf(doelen);
    }
}
