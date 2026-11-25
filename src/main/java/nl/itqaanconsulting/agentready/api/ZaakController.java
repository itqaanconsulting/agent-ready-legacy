package nl.itqaanconsulting.agentready.api;

import nl.itqaanconsulting.agentready.privacy.PiiRedactor;
import nl.itqaanconsulting.agentready.zaak.Zaak;
import nl.itqaanconsulting.agentready.zaak.ZaaksysteemClient;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Modern REST facade over the legacy SOAP case system, for internal applications. */
@RestController
@RequestMapping("/api/v1/zaken")
class ZaakController {

    static final String PERSONAL_DATA_SCOPE = "SCOPE_zaken.persoonsgegevens";

    private final ZaaksysteemClient zaaksysteem;
    private final PiiRedactor redactor;

    ZaakController(ZaaksysteemClient zaaksysteem, PiiRedactor redactor) {
        this.zaaksysteem = zaaksysteem;
        this.redactor = redactor;
    }

    @GetMapping("/{zaaknummer}")
    ZaakResponse zaak(@PathVariable String zaaknummer, Authentication caller) {
        Zaak zaak = zaaksysteem.geefZaakdetails(zaaknummer);
        boolean personalData = caller.getAuthorities().stream()
                .anyMatch(authority -> PERSONAL_DATA_SCOPE.equals(authority.getAuthority()));
        String toelichting = personalData ? zaak.toelichting()
                : redactor.redact(zaak.toelichting(), List.of(zaak.initiator().naam(), zaak.behandelaar().naam()));
        return new ZaakResponse(
                zaak.zaaknummer(),
                zaak.zaaktype().omschrijving(),
                zaak.status().code(),
                zaak.status().omschrijving(),
                zaak.status().datum(),
                zaak.startdatum(),
                zaak.uiterlijkeEinddatum(),
                zaak.behandelaar().afdeling(),
                toelichting,
                personalData ? zaak.initiator() : null);
    }
}
