package railway.model;

import railway.exception.InvalidInputException;

/** A reservation record. Confirmed tickets hold a seat; waiting tickets hold a queue position. */
public class Ticket {
    private final Integer pnr;
    private final Passenger passenger;
    private final String classCode;
    private Boolean confirmed;
    private int row;
    private int col;
    private Integer waitPosition;
    private final Double fare;
    private final String bookedAt;

    public Ticket(Integer pnr, Passenger passenger, String classCode, Boolean confirmed,
                  int row, int col, Integer waitPosition, Double fare, String bookedAt) {
        this.pnr = pnr;
        this.passenger = passenger;
        this.classCode = classCode;
        this.confirmed = confirmed;
        this.row = row;
        this.col = col;
        this.waitPosition = waitPosition;
        this.fare = fare;
        this.bookedAt = bookedAt;
    }

    public Integer getPnr() { return pnr; }
    public Passenger getPassenger() { return passenger; }
    public String getClassCode() { return classCode; }
    public Boolean isConfirmed() { return confirmed; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public Integer getWaitPosition() { return waitPosition; }
    public Double getFare() { return fare; }
    public String getBookedAt() { return bookedAt; }

    public void confirmSeat(int row, int col) {
        this.confirmed = Boolean.TRUE;
        this.row = row;
        this.col = col;
        this.waitPosition = Integer.valueOf(0);
    }

    public void setWaitPosition(int position) {
        this.waitPosition = Integer.valueOf(position);
    }

    /** Human-readable status such as CNF or WL/2. */
    public String status() {
        return confirmed ? "CNF" : "WL/" + waitPosition;
    }

    /** Serialises the ticket as one pipe-delimited line. */
    public String toRecord() {
        return pnr + "|" + passenger.getName() + "|" + passenger.getAge() + "|" + passenger.getGender()
                + "|" + classCode + "|" + (confirmed ? "CNF" : "WL") + "|" + row + "|" + col
                + "|" + waitPosition + "|" + fare + "|" + bookedAt;
    }

    /** Parses a line produced by toRecord(). */
    public static Ticket fromRecord(String line) throws InvalidInputException {
        String[] t = line.split("\\|");
        if (t.length != 11) {
            throw new InvalidInputException("Expected 11 fields but found " + t.length + ".");
        }
        try {
            Integer pnr = Integer.valueOf(t[0].trim());
            Passenger p = new Passenger(t[1], Integer.parseInt(t[2].trim()), t[3]);
            Boolean cnf = Boolean.valueOf(t[5].trim().equals("CNF"));
            int row = Integer.parseInt(t[6].trim());
            int col = Integer.parseInt(t[7].trim());
            Integer wl = Integer.valueOf(t[8].trim());
            Double fare = Double.valueOf(t[9].trim());
            return new Ticket(pnr, p, t[4].trim(), cnf, row, col, wl, fare, t[10].trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Numeric field is malformed: " + e.getMessage());
        }
    }
}
