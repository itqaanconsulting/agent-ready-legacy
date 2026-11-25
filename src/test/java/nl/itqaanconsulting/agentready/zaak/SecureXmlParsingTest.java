package nl.itqaanconsulting.agentready.zaak;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The parser itself must refuse DOCTYPEs, so external entities (XXE) are never resolved. */
class SecureXmlParsingTest {

    @Test
    void refusesDocumentsWithDoctype() throws Exception {
        String xxe = Files.readString(Path.of("legacy-simulator/__files/xxe.xml"));
        assertThatThrownBy(() -> ZaaksysteemClient.secureParser(xxe))
                .hasMessageContaining("DOCTYPE");
    }

    @Test
    void parsesRegularResponses() throws Exception {
        String regular = Files.readString(Path.of("legacy-simulator/__files/zaak-000456.xml"));
        assertThat(ZaaksysteemClient.secureParser(regular)
                .getElementsByTagNameNS(ZaaksysteemClient.NAMESPACE, "zaaknummer").item(0).getTextContent())
                .isEqualTo("ZAAK-2026-000456");
    }
}
