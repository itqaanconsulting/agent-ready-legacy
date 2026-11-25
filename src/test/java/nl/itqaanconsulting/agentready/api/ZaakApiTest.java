package nl.itqaanconsulting.agentready.api;

import nl.itqaanconsulting.agentready.LegacySimulator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ZaakApiTest {

    @Autowired
    MockMvc mockMvc;

    @DynamicPropertySource
    static void legacy(DynamicPropertyRegistry registry) {
        registry.add("gateway.zaaksysteem.url", LegacySimulator::url);
    }

    @Test
    void returnsCaseWithoutPersonalDataByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/zaken/ZAAK-2026-000123").with(scopes("zaken.lezen")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zaaktype").value("Omgevingsvergunning bouwen"))
                .andExpect(jsonPath("$.status").value("IN_BEHANDELING"))
                .andExpect(jsonPath("$.afdeling").value("Vergunningen"))
                .andExpect(jsonPath("$.initiator").doesNotExist())
                .andExpect(content().string(allOf(
                        not(containsString("999993653")),
                        not(containsString("Fatima")),
                        not(containsString("06-12345678")),
                        not(containsString("example.nl")),
                        not(containsString("NL91ABNA")),
                        not(containsString("M. Jansen")))));
    }

    @Test
    void includesPersonalDataOnlyWithPersonalDataScope() throws Exception {
        mockMvc.perform(get("/api/v1/zaken/ZAAK-2026-000123").with(scopes("zaken.lezen", "zaken.persoonsgegevens")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initiator.bsn").value("999993653"))
                .andExpect(jsonPath("$.toelichting").value(containsString("06-12345678")));
    }

    @Test
    void requiresReadScope() throws Exception {
        mockMvc.perform(get("/api/v1/zaken/ZAAK-2026-000123")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/zaken/ZAAK-2026-000123").with(scopes("mcp"))).andExpect(status().isForbidden());
    }

    @Test
    void mapsLegacyFaultsToProblemDetails() throws Exception {
        mockMvc.perform(get("/api/v1/zaken/ZAAK-2026-000999").with(scopes("zaken.lezen")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/zaken/ZAAK-2026-000500").with(scopes("zaken.lezen")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Het zaaksysteem is tijdelijk niet beschikbaar"));
    }

    @Test
    void rejectsMalformedCaseNumbersBeforeCallingTheLegacySystem() throws Exception {
        mockMvc.perform(get("/api/v1/zaken/ZAAK-1'or'1").with(scopes("zaken.lezen")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refusesXmlWithExternalEntitiesFromTheLegacySystem() throws Exception {
        mockMvc.perform(get("/api/v1/zaken/ZAAK-2026-000666").with(scopes("zaken.lezen")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(not(containsString("root:"))));
    }

    private static RequestPostProcessor scopes(String... scopes) {
        return jwt().authorities(java.util.Arrays.stream(scopes)
                .<org.springframework.security.core.GrantedAuthority>map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList());
    }
}
