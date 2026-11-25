package nl.itqaanconsulting.agentready.audit;

import nl.itqaanconsulting.agentready.governance.Doel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Writes the audit trail. Values that come from the agent (purpose, case number) are only stored when
 * they are well-formed, so an agent cannot smuggle personal data or log-injection payloads into it.
 */
@Service
public class AuditLog {

    private static final Pattern ZAAKNUMMER = Pattern.compile("ZAAK-\\d{4}-\\d{6}");

    private final AuditEventRepository repository;
    private final Clock clock;

    @Autowired
    AuditLog(AuditEventRepository repository) {
        this(repository, Clock.systemUTC());
    }

    AuditLog(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(String agent, String tool, String doel, String zaaknummer, Uitkomst uitkomst, String reden) {
        repository.save(new AuditEvent(Instant.now(clock), safe(agent), tool, purpose(doel), caseNumber(zaaknummer),
                uitkomst, reden));
    }

    public List<AuditEvent> latest(String agent, int limit) {
        PageRequest page = PageRequest.of(0, Math.clamp(limit, 1, 500));
        return agent == null ? repository.findAllByOrderByIdDesc(page) : repository.findByAgentOrderByIdDesc(agent, page);
    }

    private static String safe(String agent) {
        return agent == null ? "[onbekend]" : agent.replaceAll("[^A-Za-z0-9._-]", "_").substring(0, Math.min(agent.length(), 100));
    }

    private static String purpose(String doel) {
        if (doel == null) {
            return null;
        }
        return Arrays.stream(Doel.values()).map(Enum::name).filter(name -> name.equalsIgnoreCase(doel.strip()))
                .findFirst().orElse("[ongeldig]");
    }

    private static String caseNumber(String zaaknummer) {
        if (zaaknummer == null) {
            return null;
        }
        return ZAAKNUMMER.matcher(zaaknummer).matches() ? zaaknummer : "[ongeldig]";
    }
}
