package nl.itqaanconsulting.agentready.zaak;

public class ZaakNietGevondenException extends RuntimeException {

    public ZaakNietGevondenException(String zaaknummer) {
        super("Zaak niet gevonden: " + zaaknummer);
    }
}
