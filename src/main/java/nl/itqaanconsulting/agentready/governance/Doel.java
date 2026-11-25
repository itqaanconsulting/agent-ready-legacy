package nl.itqaanconsulting.agentready.governance;

import java.util.Optional;

/**
 * Purposes ("doelen") an agent may declare. Purpose binding (GDPR art. 5(1)(b)): an agent may only
 * use data for the purposes it was registered for, and has to state its purpose on every call.
 */
public enum Doel {
    KLANTCONTACT,
    RAPPORTAGE,
    BEHANDELING;

    static Optional<Doel> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (Doel doel : values()) {
            if (doel.name().equalsIgnoreCase(value.strip())) {
                return Optional.of(doel);
            }
        }
        return Optional.empty();
    }
}
