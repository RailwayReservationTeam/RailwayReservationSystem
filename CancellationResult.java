package railway.service;

import railway.model.Ticket;

/** Outcome of a cancellation: the cancelled ticket, any promoted ticket and the refund. */
public class CancellationResult {
    private final Ticket cancelled;
    private final Ticket promoted;
    private final Double refund;

    public CancellationResult(Ticket cancelled, Ticket promoted, Double refund) {
        this.cancelled = cancelled;
        this.promoted = promoted;
        this.refund = refund;
    }

    public Ticket getCancelled() { return cancelled; }
    public Ticket getPromoted() { return promoted; }
    public Double getRefund() { return refund; }
}
