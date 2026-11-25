package nl.itqaanconsulting.agentready.governance;

public class ToolCallDeniedException extends RuntimeException {

    private final Weigering weigering;

    ToolCallDeniedException(Weigering weigering) {
        super(weigering.uitleg());
        this.weigering = weigering;
    }

    public Weigering weigering() {
        return weigering;
    }
}
