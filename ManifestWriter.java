package railway.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import railway.model.Coach;
import railway.model.Ticket;
import railway.model.Train;
import railway.model.WaitingList;

/** Writes the passenger manifest (reservation chart) for a train. */
public class ManifestWriter {

    private static final String RULE = "=================================================================";

    public void write(String path, Train train, Ticket[] tickets) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path, false))) {
            pw.println(RULE);
            pw.println(" RESERVATION CHART / PASSENGER MANIFEST");
            pw.println(" Train     : " + train.getNumber() + " - " + train.getName());
            pw.println(" Route     : " + train.getSource() + " -> " + train.getDestination()
                    + " (" + train.getDistanceKm() + " km)");
            pw.println(" Generated : " + HistoryLogger.now());
            pw.println(RULE);
            for (int i = 0; i < Train.CLASS_CODES.length; i++) {
                Coach coach = train.coach(i);
                WaitingList wl = train.waitingList(i);
                pw.println();
                pw.println("CLASS " + coach.getCode() + " (" + coach.getTitle() + ")  Capacity: " + coach.capacity()
                        + "  Booked: " + (coach.capacity() - coach.available())
                        + "  Vacant: " + coach.available() + "  Waiting: " + wl.size());
                pw.println(String.format("%-12s %-8s %-22s %-4s %-3s %10s", "Seat", "PNR", "Name", "Age", "G", "Fare(Rs)"));
                for (int r = 0; r < coach.getRows(); r++) {
                    for (int c = 0; c < coach.getCols(); c++) {
                        Integer pnr = coach.pnrAt(r, c);
                        if (pnr != null) {
                            Ticket t = find(tickets, pnr);
                            if (t != null) {
                                pw.println(String.format("%-12s %-8d %-22s %-4d %-3s %10.2f", coach.seatLabel(r, c),
                                        t.getPnr(), t.getPassenger().getName(), t.getPassenger().getAge(),
                                        t.getPassenger().getGender(), t.getFare()));
                            }
                        }
                    }
                }
                if (wl.size() > 0) {
                    pw.println("-- Waiting list --");
                    for (int k = 0; k < wl.size(); k++) {
                        Ticket t = wl.get(k);
                        pw.println(String.format("%-12s %-8d %-22s %-4d %-3s %10.2f", t.status(), t.getPnr(),
                                t.getPassenger().getName(), t.getPassenger().getAge(),
                                t.getPassenger().getGender(), t.getFare()));
                    }
                }
            }
            pw.println();
            pw.println(RULE);
            pw.println(" End of chart");
            pw.println(RULE);
        }
    }

    private static Ticket find(Ticket[] tickets, Integer pnr) {
        for (int i = 0; i < tickets.length; i++) {
            if (tickets[i].getPnr().equals(pnr)) {
                return tickets[i];
            }
        }
        return null;
    }
}
