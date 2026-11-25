package nl.itqaanconsulting.agentready.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One tool call by an agent: who, what, for which purpose, on which case, and the outcome.
 * Deliberately holds no case content or personal data, so the audit trail itself is not a privacy risk.
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant tijdstip;

    @Column(nullable = false, length = 100)
    private String agent;

    @Column(nullable = false, length = 50)
    private String tool;

    @Column(length = 50)
    private String doel;

    @Column(length = 20)
    private String zaaknummer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Uitkomst uitkomst;

    @Column(length = 50)
    private String reden;

    protected AuditEvent() {
    }

    AuditEvent(Instant tijdstip, String agent, String tool, String doel, String zaaknummer,
               Uitkomst uitkomst, String reden) {
        this.tijdstip = tijdstip;
        this.agent = agent;
        this.tool = tool;
        this.doel = doel;
        this.zaaknummer = zaaknummer;
        this.uitkomst = uitkomst;
        this.reden = reden;
    }

    public Long getId() {
        return id;
    }

    public Instant getTijdstip() {
        return tijdstip;
    }

    public String getAgent() {
        return agent;
    }

    public String getTool() {
        return tool;
    }

    public String getDoel() {
        return doel;
    }

    public String getZaaknummer() {
        return zaaknummer;
    }

    public Uitkomst getUitkomst() {
        return uitkomst;
    }

    public String getReden() {
        return reden;
    }
}
