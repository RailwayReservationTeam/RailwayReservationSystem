package railway.model;

import railway.exception.SeatNotAvailableException;

/** Array-backed FIFO queue with a fixed upper limit and a growing backing array. */
public class WaitingList {
    private Ticket[] items = new Ticket[2];
    private int size = 0;
    private final int limit;
    private final String classCode;

    public WaitingList(String classCode, int limit) {
        this.classCode = classCode;
        this.limit = limit;
    }

    public int size() { return size; }
    public int limit() { return limit; }

    public void enqueue(Ticket t) throws SeatNotAvailableException {
        if (size >= limit) {
            throw new SeatNotAvailableException(classCode,
                    "Class is full and the waiting list (limit " + limit + ") is closed.");
        }
        if (size == items.length) {
            Ticket[] bigger = new Ticket[items.length * 2];
            System.arraycopy(items, 0, bigger, 0, size);
            items = bigger;
        }
        items[size++] = t;
        renumber();
    }

    /** Removes and returns the head of the queue, or null if the queue is empty. */
    public Ticket dequeue() {
        if (size == 0) {
            return null;
        }
        Ticket head = items[0];
        removeAt(0);
        return head;
    }

    /** Removes the ticket with the given PNR; returns true if it was present. */
    public boolean remove(Integer pnr) {
        for (int i = 0; i < size; i++) {
            if (items[i].getPnr().equals(pnr)) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    public Ticket get(int index) {
        return items[index];
    }

    private void removeAt(int index) {
        for (int i = index; i < size - 1; i++) {
            items[i] = items[i + 1];
        }
        items[--size] = null;
        renumber();
    }

    private void renumber() {
        for (int i = 0; i < size; i++) {
            items[i].setWaitPosition(i + 1);
        }
    }
}
