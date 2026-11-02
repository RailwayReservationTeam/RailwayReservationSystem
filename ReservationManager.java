package railway.service;

import railway.exception.InvalidInputException;
import railway.exception.SeatNotAvailableException;
import railway.exception.TicketNotFoundException;
import railway.io.HistoryLogger;
import railway.model.Coach;
import railway.model.Passenger;
import railway.model.Ticket;
import railway.model.Train;
import railway.model.WaitingList;

/** Core booking, cancellation and waiting-list promotion logic. */
public class ReservationManager {
    private static final double CONFIRMED_REFUND = 0.75;
    private static final double WAITING_REFUND = 0.90;

    private final Train train;
    private final HistoryLogger logger;
    private Ticket[] ledger = new Ticket[4];
    private int count = 0;
    private Integer nextPnr = Integer.valueOf(100001);

    public ReservationManager(Train train, HistoryLogger logger) {
        this.train = train;
        this.logger = logger;
    }

    public Train getTrain() { return train; }

    /** Books a ticket: confirmed if a seat is free, otherwise waiting-listed if the queue is open. */
    public Ticket book(Passenger p, String classCode) throws InvalidInputException, SeatNotAvailableException {
        int idx = train.indexOf(classCode);
        Coach coach = train.coach(idx);
        Integer pnr = nextPnr;
        Double fare = computeFare(coach, p);
        String now = HistoryLogger.now();
        Ticket t;
        try {
            int[] pos = coach.allocate(pnr);
            t = new Ticket(pnr, p, coach.getCode(), Boolean.TRUE, pos[0], pos[1], Integer.valueOf(0), fare, now);
        } catch (SeatNotAvailableException full) {
            t = new Ticket(pnr, p, coach.getCode(), Boolean.FALSE, -1, -1, Integer.valueOf(0), fare, now);
            train.waitingList(idx).enqueue(t);
        }
        nextPnr = Integer.valueOf(nextPnr.intValue() + 1);
        addToLedger(t);
        log("BOOKED PNR " + t.getPnr() + " " + p.getName() + " " + coach.getCode() + " " + t.status()
                + (t.isConfirmed() ? " seat " + coach.seatLabel(t.getRow(), t.getCol()) : ""));
        return t;
    }

    /** Cancels a ticket, refunds part of the fare and promotes the head of the waiting list. */
    public CancellationResult cancel(int pnrValue) throws TicketNotFoundException {
        Integer pnr = Integer.valueOf(pnrValue);
        int pos = ledgerIndex(pnr);
        if (pos < 0) {
            throw new TicketNotFoundException(pnr);
        }
        Ticket t = ledger[pos];
        removeFromLedger(pos);
        int idx;
        try {
            idx = train.indexOf(t.getClassCode());
        } catch (InvalidInputException e) {
            throw new IllegalStateException("Stored ticket has unknown class: " + t.getClassCode());
        }
        Coach coach = train.coach(idx);
        WaitingList wl = train.waitingList(idx);
        Ticket promoted = null;
        double rate;
        if (t.isConfirmed()) {
            coach.release(t.getRow(), t.getCol());
            rate = CONFIRMED_REFUND;
            Ticket head = wl.dequeue();
            if (head != null) {
                try {
                    int[] seat = coach.allocate(head.getPnr());
                    head.confirmSeat(seat[0], seat[1]);
                    promoted = head;
                } catch (SeatNotAvailableException e) {
                    throw new IllegalStateException("A seat was released but could not be reallocated.");
                }
            }
        } else {
            wl.remove(pnr);
            rate = WAITING_REFUND;
        }
        Double refund = Double.valueOf(round2(t.getFare().doubleValue() * rate));
        log("CANCELLED PNR " + pnr + " " + t.getPassenger().getName() + " refund Rs " + refund);
        if (promoted != null) {
            log("PROMOTED PNR " + promoted.getPnr() + " " + promoted.getPassenger().getName()
                    + " WL -> CNF seat " + coach.seatLabel(promoted.getRow(), promoted.getCol()));
        }
        return new CancellationResult(t, promoted, refund);
    }

    public Ticket find(int pnrValue) throws TicketNotFoundException {
        int pos = ledgerIndex(Integer.valueOf(pnrValue));
        if (pos < 0) {
            throw new TicketNotFoundException(Integer.valueOf(pnrValue));
        }
        return ledger[pos];
    }

    public Ticket[] getTickets() {
        Ticket[] copy = new Ticket[count];
        System.arraycopy(ledger, 0, copy, 0, count);
        return copy;
    }

    /** Rebuilds seat matrices and queues from saved tickets; returns the number of tickets skipped. */
    public int restore(Ticket[] saved) {
        Ticket[] sorted = new Ticket[saved.length];
        System.arraycopy(saved, 0, sorted, 0, saved.length);
        for (int i = 0; i < sorted.length - 1; i++) {
            for (int j = 0; j < sorted.length - 1 - i; j++) {
                if (sorted[j].getWaitPosition() > sorted[j + 1].getWaitPosition()) {
                    Ticket tmp = sorted[j];
                    sorted[j] = sorted[j + 1];
                    sorted[j + 1] = tmp;
                }
            }
        }
        int skipped = 0;
        int maxPnr = nextPnr.intValue() - 1;
        for (int i = 0; i < sorted.length; i++) {
            Ticket t = sorted[i];
            try {
                int idx = train.indexOf(t.getClassCode());
                if (t.isConfirmed()) {
                    train.coach(idx).occupy(t.getRow(), t.getCol(), t.getPnr());
                } else {
                    train.waitingList(idx).enqueue(t);
                }
                addToLedger(t);
                if (t.getPnr().intValue() > maxPnr) {
                    maxPnr = t.getPnr().intValue();
                }
            } catch (InvalidInputException e) {
                skipped++;
            } catch (SeatNotAvailableException e) {
                skipped++;
            }
        }
        nextPnr = Integer.valueOf(maxPnr + 1);
        return skipped;
    }

    /** Fare = rate x distance, with 50% concession under 12 and 40% concession from 60 years. */
    public Double computeFare(Coach coach, Passenger p) {
        double fare = coach.ratePerKm() * train.getDistanceKm().intValue();
        int age = p.getAge().intValue();
        if (age < 12) {
            fare = fare * 0.50;
        } else if (age >= 60) {
            fare = fare * 0.60;
        }
        return Double.valueOf(round2(fare));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private void log(String s) {
        if (logger != null) {
            logger.log(s);
        }
    }

    private void addToLedger(Ticket t) {
        if (count == ledger.length) {
            Ticket[] bigger = new Ticket[ledger.length * 2];
            System.arraycopy(ledger, 0, bigger, 0, count);
            ledger = bigger;
        }
        ledger[count++] = t;
    }

    private int ledgerIndex(Integer pnr) {
        for (int i = 0; i < count; i++) {
            if (ledger[i].getPnr().equals(pnr)) {
                return i;
            }
        }
        return -1;
    }

    private void removeFromLedger(int pos) {
        for (int i = pos; i < count - 1; i++) {
            ledger[i] = ledger[i + 1];
        }
        ledger[--count] = null;
    }
}
