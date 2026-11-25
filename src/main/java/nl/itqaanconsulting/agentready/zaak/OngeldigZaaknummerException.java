package nl.itqaanconsulting.agentready.zaak;

public class OngeldigZaaknummerException extends RuntimeException {

    public OngeldigZaaknummerException(String value) {
        super("Ongeldig zaaknummer, verwacht formaat ZAAK-jjjj-nnnnnn");
    }
}
