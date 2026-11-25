package nl.itqaanconsulting.agentready.mcp;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import nl.itqaanconsulting.agentready.audit.AuditLog;
import nl.itqaanconsulting.agentready.audit.Uitkomst;
import nl.itqaanconsulting.agentready.governance.AgentGovernance;
import nl.itqaanconsulting.agentready.governance.ToolCallDeniedException;
import nl.itqaanconsulting.agentready.privacy.PiiRedactor;
import nl.itqaanconsulting.agentready.zaak.OngeldigZaaknummerException;
import nl.itqaanconsulting.agentready.zaak.Zaak;
import nl.itqaanconsulting.agentready.zaak.ZaakNietGevondenException;
import nl.itqaanconsulting.agentready.zaak.ZaaksysteemClient;
import nl.itqaanconsulting.agentready.zaak.ZaaksysteemNietBeschikbaarException;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * The MCP tools agents can call. Every call passes the same gate: identify the agent from its token,
 * check the registry (tool, purpose, rate limit), call the legacy system, minimise the data, audit.
 * Agents never receive personal data: there is no MCP equivalent of the zaken.persoonsgegevens scope.
 */
@Component
class ZaakTools {

    static final String ZAAK_STATUS = "zaak_status";
    static final String ZAAK_SAMENVATTING = "zaak_samenvatting";

    private final AgentGovernance governance;
    private final AuditLog auditLog;
    private final ZaaksysteemClient zaaksysteem;
    private final PiiRedactor redactor;
    private final JsonMapper json;

    ZaakTools(AgentGovernance governance, AuditLog auditLog, ZaaksysteemClient zaaksysteem, PiiRedactor redactor,
              JsonMapper json) {
        this.governance = governance;
        this.auditLog = auditLog;
        this.zaaksysteem = zaaksysteem;
        this.redactor = redactor;
        this.json = json;
    }

    @McpTool(name = ZAAK_STATUS, description = """
            Geeft de actuele status van een zaak van Gemeente Voorbeeldstad: zaaktype, status, \
            uiterlijke einddatum en de afdeling die de zaak behandelt. Bevat geen persoonsgegevens.""")
    CallToolResult zaakStatus(
            @McpToolParam(description = "Zaaknummer, bijvoorbeeld ZAAK-2026-000123") String zaaknummer,
            @McpToolParam(description = "Doel van de aanvraag: KLANTCONTACT, RAPPORTAGE of BEHANDELING") String doel) {
        return handle(ZAAK_STATUS, zaaknummer, doel, zaak -> new ZaakStatus(
                zaak.zaaknummer(), zaak.zaaktype().omschrijving(), zaak.status().code(), zaak.status().omschrijving(),
                zaak.status().datum(), zaak.uiterlijkeEinddatum(), zaak.behandelaar().afdeling()));
    }

    @McpTool(name = ZAAK_SAMENVATTING, description = """
            Geeft de status van een zaak plus de toelichting van de behandelaar, met persoonsgegevens \
            (namen, BSN, telefoon, e-mail, IBAN, postcode) vervangen door placeholders.""")
    CallToolResult zaakSamenvatting(
            @McpToolParam(description = "Zaaknummer, bijvoorbeeld ZAAK-2026-000123") String zaaknummer,
            @McpToolParam(description = "Doel van de aanvraag: KLANTCONTACT, RAPPORTAGE of BEHANDELING") String doel) {
        return handle(ZAAK_SAMENVATTING, zaaknummer, doel, zaak -> new ZaakSamenvatting(
                zaak.zaaknummer(), zaak.zaaktype().omschrijving(), zaak.status().code(), zaak.status().omschrijving(),
                zaak.startdatum(), zaak.uiterlijkeEinddatum(), zaak.behandelaar().afdeling(),
                redactor.redact(zaak.toelichting(), List.of(zaak.initiator().naam(), zaak.behandelaar().naam()))));
    }

    private CallToolResult handle(String tool, String zaaknummer, String doel, Function<Zaak, Object> view) {
        String agent = currentAgent();
        try {
            governance.authorize(agent, tool, doel);
        } catch (ToolCallDeniedException denied) {
            auditLog.record(agent, tool, doel, zaaknummer, Uitkomst.GEWEIGERD, denied.weigering().name());
            return error("Geweigerd (" + denied.weigering().name() + "): " + denied.getMessage());
        }
        try {
            Object result = view.apply(zaaksysteem.geefZaakdetails(zaaknummer));
            auditLog.record(agent, tool, doel, zaaknummer, Uitkomst.TOEGESTAAN, null);
            return CallToolResult.builder()
                    .content(List.of(new TextContent(json.writeValueAsString(result))))
                    .structuredContent(result)
                    .isError(false)
                    .build();
        } catch (OngeldigZaaknummerException exception) {
            return failed(agent, tool, doel, zaaknummer, "ONGELDIG_ZAAKNUMMER", exception.getMessage());
        } catch (ZaakNietGevondenException exception) {
            return failed(agent, tool, doel, zaaknummer, "ZAAK_NIET_GEVONDEN", exception.getMessage());
        } catch (ZaaksysteemNietBeschikbaarException exception) {
            return failed(agent, tool, doel, zaaknummer, "ZAAKSYSTEEM_NIET_BESCHIKBAAR",
                    "Het zaaksysteem is tijdelijk niet beschikbaar");
        }
    }

    private CallToolResult failed(String agent, String tool, String doel, String zaaknummer, String reden,
                                  String message) {
        auditLog.record(agent, tool, doel, zaaknummer, Uitkomst.FOUT, reden);
        return error(message);
    }

    private static CallToolResult error(String message) {
        return CallToolResult.builder().content(List.of(new TextContent(message))).isError(true).build();
    }

    /** The OAuth2 client the agent authenticated as (azp claim; client_id or sub as fallback). */
    private static String currentAgent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            for (String claim : List.of("azp", "client_id", "sub")) {
                String value = token.getToken().getClaimAsString(claim);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    record ZaakStatus(String zaaknummer, String zaaktype, String status, String statusOmschrijving,
                      LocalDate statusdatum, LocalDate uiterlijkeEinddatum, String afdeling) {
    }

    record ZaakSamenvatting(String zaaknummer, String zaaktype, String status, String statusOmschrijving,
                            LocalDate startdatum, LocalDate uiterlijkeEinddatum, String afdeling,
                            String toelichting) {
    }
}
