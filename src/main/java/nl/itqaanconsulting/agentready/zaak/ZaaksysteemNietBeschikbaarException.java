package nl.itqaanconsulting.agentready.zaak;

/** The legacy system failed, timed out or answered something we refuse to process. Details go to the log only. */
public class ZaaksysteemNietBeschikbaarException extends RuntimeException {

    public ZaaksysteemNietBeschikbaarException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
