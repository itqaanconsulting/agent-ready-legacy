package nl.itqaanconsulting.agentready.privacy;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Removes personal data from free text before it leaves the gateway. Pattern-based for
 * identifiers (BSN, IBAN, phone, e-mail, postcode); names cannot be detected reliably,
 * so names already known from the case record are removed explicitly.
 */
@Component
public class PiiRedactor {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern IBAN = Pattern.compile("\\b[A-Z]{2}\\d{2}\\s?[A-Z]{4}(\\s?\\d){10}\\b");
    private static final Pattern PHONE = Pattern.compile("(\\+31[\\s-]?|\\b0)\\d([\\s-]?\\d){7,9}\\b");
    private static final Pattern BSN = Pattern.compile("\\b\\d{9}\\b");
    private static final Pattern POSTCODE = Pattern.compile("\\b\\d{4}\\s?[A-Z]{2}\\b");

    public String redact(String text, Collection<String> knownPersonalValues) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (String value : knownPersonalValues) {
            if (value != null && !value.isBlank()) {
                result = result.replace(value, "[persoon]");
            }
        }
        result = EMAIL.matcher(result).replaceAll("[e-mail]");
        result = IBAN.matcher(result).replaceAll("[iban]");
        result = PHONE.matcher(result).replaceAll("[telefoon]");
        result = BSN.matcher(result).replaceAll("[bsn]");
        result = POSTCODE.matcher(result).replaceAll("[postcode]");
        return result;
    }
}
