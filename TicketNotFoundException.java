package railway.exception;

/** Thrown when a PNR does not correspond to any active ticket. */
public class TicketNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;
    private final Integer pnr;

    public TicketNotFoundException(Integer pnr) {
        super("No active ticket found for PNR " + pnr + ".");
        this.pnr = pnr;
    }

    public Integer getPnr() {
        return pnr;
    }
}
