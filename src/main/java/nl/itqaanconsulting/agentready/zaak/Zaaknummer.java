package nl.itqaanconsulting.agentready.zaak;

import java.util.regex.Pattern;

/** Case numbers are validated before they reach the legacy system: no injection into the SOAP request. */
public final class Zaaknummer {

    private static final Pattern FORMAT = Pattern.compile("ZAAK-\\d{4}-\\d{6}");

    private Zaaknummer() {
    }

    public static String validated(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new OngeldigZaaknummerException(value);
        }
        return value;
    }
}
