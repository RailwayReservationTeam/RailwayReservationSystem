package railway.model;

import railway.exception.SeatNotAvailableException;

/**
 * Abstract base for a coach class (AC, Sleeper, General). The seat matrix is
 * a two-dimensional array of Integer wrappers: a null cell means the seat is
 * vacant, a non-null cell holds the PNR that occupies it.
 */
public abstract class Coach {
    private final String code;
    private final String title;
    private final int rows;
    private final int cols;
    private final Integer[][] seats;

    protected Coach(String code, String title, int rows, int cols) {
        this.code = code;
        this.title = title;
        this.rows = rows;
        this.cols = cols;
        this.seats = new Integer[rows][cols];
    }

    /** Fare in rupees per kilometre for this class. */
    public abstract double ratePerKm();

    /** Berth type label for a column inside a compartment. */
    public abstract String berthName(int col);

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int capacity() { return rows * cols; }

    public int available() {
        int free = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (seats[r][c] == null) {
                    free++;
                }
            }
        }
        return free;
    }

    /** Allocates the first vacant seat (row-major order) and returns {row, col}. */
    public int[] allocate(Integer pnr) throws SeatNotAvailableException {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (seats[r][c] == null) {
                    seats[r][c] = pnr;
                    return new int[] { r, c };
                }
            }
        }
        throw new SeatNotAvailableException(code, "All " + capacity() + " seats in " + title + " are booked.");
    }

    /** Places a PNR in a specific seat; used when restoring saved tickets. */
    public void occupy(int r, int c, Integer pnr) throws SeatNotAvailableException {
        if (r < 0 || r >= rows || c < 0 || c >= cols) {
            throw new SeatNotAvailableException(code, "Seat position (" + r + "," + c + ") is outside the coach.");
        }
        if (seats[r][c] != null) {
            throw new SeatNotAvailableException(code, "Seat " + seatLabel(r, c) + " is already occupied.");
        }
        seats[r][c] = pnr;
    }

    public void release(int r, int c) {
        if (r >= 0 && r < rows && c >= 0 && c < cols) {
            seats[r][c] = null;
        }
    }

    public Integer pnrAt(int r, int c) {
        return seats[r][c];
    }

    public String seatLabel(int r, int c) {
        return code + "-C" + (r + 1) + "-" + berthName(c) + (c + 1);
    }
}
