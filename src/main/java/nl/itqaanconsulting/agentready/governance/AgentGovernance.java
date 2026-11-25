package nl.itqaanconsulting.agentready.governance;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Policy decision point for agent tool calls. Checks run cheapest-first, and the rate limit applies
 * before the tool and purpose checks, so an agent probing for forbidden tools is throttled as well.
 */
@Service
public class AgentGovernance {

    private final AgentRegistryProperties registry;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    AgentGovernance(AgentRegistryProperties registry) {
        this.registry = registry;
    }

    /** Returns the validated purpose, or throws {@link ToolCallDeniedException}. */
    public Doel authorize(String agent, String tool, String doel) {
        AgentPolicy policy = agent == null ? null : registry.agents().get(agent);
        if (policy == null) {
            throw new ToolCallDeniedException(Weigering.UNKNOWN_AGENT);
        }
        if (!bucket(agent, policy).tryConsume(1)) {
            throw new ToolCallDeniedException(Weigering.RATE_LIMITED);
        }
        if (!policy.tools().contains(tool)) {
            throw new ToolCallDeniedException(Weigering.TOOL_NOT_ALLOWED);
        }
        return Doel.parse(doel)
                .filter(policy.doelen()::contains)
                .orElseThrow(() -> new ToolCallDeniedException(Weigering.PURPOSE_NOT_ALLOWED));
    }

    private Bucket bucket(String agent, AgentPolicy policy) {
        return buckets.computeIfAbsent(agent, key -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(policy.requestsPerMinute())
                        .refillGreedy(policy.requestsPerMinute(), Duration.ofMinutes(1))
                        .build())
                .build());
    }
}
