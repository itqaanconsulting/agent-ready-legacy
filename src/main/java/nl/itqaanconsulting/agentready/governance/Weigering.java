package nl.itqaanconsulting.agentready.governance;

/** Why a tool call was refused. Recorded in the audit trail and returned to the agent. */
public enum Weigering {
    UNKNOWN_AGENT("Deze agent is niet geregistreerd bij de gateway"),
    RATE_LIMITED("Te veel verzoeken; probeer het over een minuut opnieuw"),
    TOOL_NOT_ALLOWED("Deze agent mag deze tool niet gebruiken"),
    PURPOSE_NOT_ALLOWED("Dit doel is niet toegestaan voor deze agent");

    private final String uitleg;

    Weigering(String uitleg) {
        this.uitleg = uitleg;
    }

    public String uitleg() {
        return uitleg;
    }
}
