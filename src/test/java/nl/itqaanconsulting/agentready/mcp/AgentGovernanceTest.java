package nl.itqaanconsulting.agentready.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.McpHttpClientTransportAuthorizationException;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import nl.itqaanconsulting.agentready.LegacySimulator;
import nl.itqaanconsulting.agentready.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** End to end: a real MCP client acting as the agent, over HTTP, with a JWT per agent. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgentGovernanceTest {

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void legacy(DynamicPropertyRegistry registry) {
        registry.add("gateway.zaaksysteem.url", LegacySimulator::url);
    }

    @Test
    void customerContactAgentGetsARedactedSummary() {
        CallToolResult result = call("klantcontact-agent", "zaak_samenvatting", "ZAAK-2026-000123", "KLANTCONTACT");

        assertThat(result.isError()).isFalse();
        assertThat(text(result))
                .contains("Omgevingsvergunning bouwen", "Vergunningen", "[telefoon]", "[e-mail]", "[iban]", "[persoon]")
                .doesNotContain("999993653", "Fatima", "06-12345678", "example.nl", "NL91ABNA", "M. Jansen");
    }

    @Test
    void reportingAgentMayOnlyUseTheStatusToolForReporting() {
        assertThat(call("rapportage-agent", "zaak_status", "ZAAK-2026-000456", "RAPPORTAGE").isError()).isFalse();
        assertDenied(call("rapportage-agent", "zaak_samenvatting", "ZAAK-2026-000456", "RAPPORTAGE"),
                "TOOL_NOT_ALLOWED");
        assertDenied(call("rapportage-agent", "zaak_status", "ZAAK-2026-000456", "KLANTCONTACT"),
                "PURPOSE_NOT_ALLOWED");
        assertDenied(call("rapportage-agent", "zaak_status", "ZAAK-2026-000456", "nieuwsgierigheid"),
                "PURPOSE_NOT_ALLOWED");
    }

    @Test
    void unregisteredAgentGetsNothingEvenWithAValidToken() {
        assertDenied(call("onbekende-agent", "zaak_status", "ZAAK-2026-000123", "KLANTCONTACT"), "UNKNOWN_AGENT");
    }

    @Test
    void agentsAreRateLimited() {
        try (McpSyncClient client = client(TestTokens.forClient("beperkte-agent", "mcp"))) {
            Map<String, Object> args = Map.of("zaaknummer", "ZAAK-2026-000456", "doel", "KLANTCONTACT");
            assertThat(client.callTool(new CallToolRequest("zaak_status", args)).isError()).isFalse();
            assertThat(client.callTool(new CallToolRequest("zaak_status", args)).isError()).isFalse();
            assertDenied(client.callTool(new CallToolRequest("zaak_status", args)), "RATE_LIMITED");
        }
    }

    @Test
    void legacyFailuresReachTheAgentAsToolErrorsWithoutDetails() {
        CallToolResult result = call("klantcontact-agent", "zaak_status", "ZAAK-2026-000500", "KLANTCONTACT");
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("Het zaaksysteem is tijdelijk niet beschikbaar");
    }

    @Test
    void tokenWithoutMcpScopeCannotConnect() {
        assertThatThrownBy(() -> {
            try (McpSyncClient client = client(TestTokens.forClient("klantcontact-agent", "zaken.lezen"))) {
                client.listTools();
            }
        }).rootCause().isInstanceOf(McpHttpClientTransportAuthorizationException.class);
    }

    @Test
    void everyCallIsAuditedWithoutCaseContent() {
        call("klantcontact-agent", "zaak_samenvatting", "ZAAK-2026-000123", "KLANTCONTACT");
        call("klantcontact-agent", "zaak_status", "BSN 999993653", "KLANTCONTACT");
        call("klantcontact-agent", "zaak_status", "ZAAK-2026-000123", "RAPPORTAGE");

        List<Map<String, Object>> audit = RestClient.create("http://localhost:" + port).get()
                .uri("/api/v1/audit?agent=klantcontact-agent&limit=3")
                .header("Authorization", "Bearer " + TestTokens.forClient("privacy-officer", "audit.lezen"))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(audit).extracting("uitkomst", "reden", "zaaknummer").containsExactly(
                org.assertj.core.groups.Tuple.tuple("GEWEIGERD", "PURPOSE_NOT_ALLOWED", "ZAAK-2026-000123"),
                org.assertj.core.groups.Tuple.tuple("FOUT", "ONGELDIG_ZAAKNUMMER", "[ongeldig]"),
                org.assertj.core.groups.Tuple.tuple("TOEGESTAAN", null, "ZAAK-2026-000123"));
        assertThat(audit.toString()).doesNotContain("999993653", "Fatima");
    }

    private CallToolResult call(String agent, String tool, String zaaknummer, String doel) {
        try (McpSyncClient client = client(TestTokens.forClient(agent, "mcp"))) {
            return client.callTool(new CallToolRequest(tool, Map.of("zaaknummer", zaaknummer, "doel", doel)));
        }
    }

    private McpSyncClient client(String token) {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/mcp")
                .httpRequestCustomizer((request, method, uri, body, context) ->
                        request.header("Authorization", "Bearer " + token))
                .build();
        McpSyncClient client = McpClient.sync(transport).build();
        client.initialize();
        return client;
    }

    private static void assertDenied(CallToolResult result, String reason) {
        assertThat(result.isError()).isTrue();
        assertThat(text(result)).startsWith("Geweigerd (" + reason + ")");
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }
}
