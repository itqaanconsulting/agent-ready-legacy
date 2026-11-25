package nl.itqaanconsulting.agentready.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import nl.itqaanconsulting.agentready.zaak.Zaak;

import java.time.LocalDate;

/** REST view of a case. The initiator is only included for callers with the zaken.persoonsgegevens scope. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZaakResponse(
        String zaaknummer,
        String zaaktype,
        String status,
        String statusOmschrijving,
        LocalDate statusdatum,
        LocalDate startdatum,
        LocalDate uiterlijkeEinddatum,
        String afdeling,
        String toelichting,
        Zaak.Initiator initiator
) {
}
