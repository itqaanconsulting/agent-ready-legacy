package nl.itqaanconsulting.agentready.privacy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactorTest {

    private final PiiRedactor redactor = new PiiRedactor();

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            BSN                | BSN 999993653 opgegeven            | BSN [bsn] opgegeven
            mobile number      | belde op 06-12345678               | belde op [telefoon]
            landline           | bel +31 20 123 4567 terug          | bel [telefoon] terug
            e-mail             | mail naar f.elidrissi@example.nl   | mail naar [e-mail]
            IBAN               | terugbetalen op NL91ABNA0417164300 | terugbetalen op [iban]
            postcode           | adres 1234 AB                      | adres [postcode]
            known name         | Fatima el Idrissi belde            | [persoon] belde
            nothing personal   | Tekening is goedgekeurd            | Tekening is goedgekeurd
            """)
    void redactsPersonalData(String kind, String input, String expected) {
        assertThat(redactor.redact(input, List.of("Fatima el Idrissi"))).isEqualTo(expected);
    }
}
