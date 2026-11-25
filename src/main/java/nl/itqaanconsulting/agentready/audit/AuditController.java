package nl.itqaanconsulting.agentready.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Read access to the audit trail for the privacy officer or security team (scope audit.lezen). */
@RestController
@RequestMapping("/api/v1/audit")
class AuditController {

    private final AuditLog auditLog;

    AuditController(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @GetMapping
    List<Entry> latest(@RequestParam(required = false) String agent, @RequestParam(defaultValue = "100") int limit) {
        return auditLog.latest(agent, limit).stream()
                .map(event -> new Entry(event.getTijdstip(), event.getAgent(), event.getTool(), event.getDoel(),
                        event.getZaaknummer(), event.getUitkomst(), event.getReden()))
                .toList();
    }

    record Entry(Instant tijdstip, String agent, String tool, String doel, String zaaknummer, Uitkomst uitkomst,
                 String reden) {
    }
}
