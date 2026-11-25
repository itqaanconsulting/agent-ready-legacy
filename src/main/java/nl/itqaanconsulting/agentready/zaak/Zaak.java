package nl.itqaanconsulting.agentready.zaak;

import java.time.LocalDate;

/** A case ("zaak") as the legacy case system returns it, including personal data of the initiator. */
public record Zaak(
        String zaaknummer,
        Zaaktype zaaktype,
        Status status,
        LocalDate startdatum,
        LocalDate uiterlijkeEinddatum,
        Behandelaar behandelaar,
        Initiator initiator,
        String toelichting
) {

    public record Zaaktype(String code, String omschrijving) {
    }

    public record Status(String code, String omschrijving, LocalDate datum) {
    }

    /** Employee handling the case. The name is personal data as well. */
    public record Behandelaar(String naam, String afdeling) {
    }

    /** Citizen who started the case: personal data in the sense of the GDPR (AVG). */
    public record Initiator(String bsn, String naam, String adres, String telefoon, String email) {
    }
}
