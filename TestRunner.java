package railway.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import railway.exception.InvalidInputException;
import railway.exception.SeatNotAvailableException;
import railway.exception.TicketNotFoundException;
import railway.io.HistoryLogger;
import railway.io.ManifestWriter;
import railway.io.TicketStore;
import railway.model.Coach;
import railway.model.Passenger;
import railway.model.Ticket;
import railway.model.Train;
import railway.service.CancellationResult;
import railway.service.ReservationManager;

/** Plain-Java unit tests (no external framework). Run: java railway.test.TestRunner */
public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;
    private static final String DIR = "testdata";

    public static void main(String[] args) {
        new File(DIR).mkdirs();
        run("T01", "Valid passenger accepted; name normalised to title case", t01());
        run("T02", "Age 0 rejected with InvalidInputException", t02());
        run("T03", "Name containing digits rejected", t03());
        run("T04", "Gender other than M/F/O rejected", t04());
        run("T05", "First AC booking is CNF at seat AC-C1-LB1", t05());
        run("T06", "Seats are allocated sequentially", t06());
        run("T07", "Booking in a full class joins waiting list as WL/1", t07());
        run("T08", "Closed waiting list throws SeatNotAvailableException", t08());
        run("T09", "Cancelling CNF ticket promotes WL/1 to CNF", t09());
        run("T10", "Remaining waiting tickets are renumbered after promotion", t10());
        run("T11", "Cancelling a WL ticket removes it and renumbers the queue", t11());
        run("T12", "Cancelling unknown PNR throws TicketNotFoundException", t12());
        run("T13", "Unknown class code throws InvalidInputException", t13());
        run("T14", "Senior citizen (age 65) fare concession of 40 percent", t14());
        run("T15", "Child (age 8) fare concession of 50 percent", t15());
        run("T16", "Refund is 75 percent of fare for a confirmed ticket", t16());
        run("T17", "Save, load and restore reproduces seats and queue", t17());
        run("T18", "Malformed record is skipped, valid records load", t18());
        run("T19", "Occupying an already-occupied seat is rejected", t19());
        run("T20", "Waiting list grows past its initial array capacity", t20());
        run("T21", "Manifest file is created and lists the passenger", t21());
        run("T22", "History log records booking and cancellation events", t22());
        System.out.println();
        System.out.println("Tests passed: " + passed + "   Tests failed: " + failed);
    }

    private static void run(String id, String desc, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println(String.format("%-4s %-62s %s", id, desc, ok ? "PASS" : "FAIL"));
    }

    private static ReservationManager manager(int compartments, int waitLimit) {
        Train t = new Train(20001, "Test Express", "A", "B", 50, compartments, waitLimit);
        return new ReservationManager(t, null);
    }

    private static Passenger p(String n, int age) throws InvalidInputException {
        return new Passenger(n, age, "M");
    }

    private static boolean t01() {
        try {
            Passenger x = new Passenger("  rAhUl   sharma ", 25, "m");
            return x.getName().equals("Rahul Sharma") && x.getGender().equals("M");
        } catch (Exception e) { return false; }
    }

    private static boolean t02() {
        try { new Passenger("Amit", 0, "M"); return false; }
        catch (InvalidInputException e) { return true; }
    }

    private static boolean t03() {
        try { new Passenger("Amit99", 30, "M"); return false; }
        catch (InvalidInputException e) { return true; }
    }

    private static boolean t04() {
        try { new Passenger("Amit", 30, "X"); return false; }
        catch (InvalidInputException e) { return true; }
    }

    private static boolean t05() {
        try {
            ReservationManager m = manager(1, 2);
            Ticket t = m.book(p("Amit", 30), "AC");
            Coach c = m.getTrain().coach(0);
            return t.isConfirmed() && c.seatLabel(t.getRow(), t.getCol()).equals("AC-C1-LB1");
        } catch (Exception e) { return false; }
    }

    private static boolean t06() {
        try {
            ReservationManager m = manager(1, 2);
            Ticket a = m.book(p("Amit", 30), "SL");
            Ticket b = m.book(p("Bina", 31), "SL");
            return a.getCol() == 0 && b.getCol() == 1 && a.getRow() == 0 && b.getRow() == 0;
        } catch (Exception e) { return false; }
    }

    private static boolean t07() {
        try {
            ReservationManager m = manager(1, 2);
            for (int i = 0; i < 4; i++) { m.book(p("Pax" + "a", 30), "AC"); }
            Ticket w = m.book(p("Late", 30), "AC");
            return !w.isConfirmed() && w.status().equals("WL/1");
        } catch (Exception e) { return false; }
    }

    private static boolean t08() {
        try {
            ReservationManager m = manager(1, 1);
            for (int i = 0; i < 5; i++) { m.book(p("Pax", 30), "AC"); }
            m.book(p("Extra", 30), "AC");
            return false;
        } catch (SeatNotAvailableException e) { return e.getClassCode().equals("AC"); }
        catch (Exception e) { return false; }
    }

    private static boolean t09() {
        try {
            ReservationManager m = manager(1, 2);
            Ticket first = null;
            for (int i = 0; i < 4; i++) {
                Ticket t = m.book(p("Pax", 30), "AC");
                if (i == 0) { first = t; }
            }
            Ticket w = m.book(p("Wait", 30), "AC");
            CancellationResult r = m.cancel(first.getPnr().intValue());
            return r.getPromoted() != null && r.getPromoted().getPnr().equals(w.getPnr())
                    && w.isConfirmed() && w.getRow() == 0 && w.getCol() == 0;
        } catch (Exception e) { return false; }
    }

    private static boolean t10() {
        try {
            ReservationManager m = manager(1, 3);
            Ticket first = null;
            for (int i = 0; i < 4; i++) {
                Ticket t = m.book(p("Pax", 30), "AC");
                if (i == 0) { first = t; }
            }
            Ticket w1 = m.book(p("W one", 30), "AC");
            Ticket w2 = m.book(p("W two", 30), "AC");
            m.cancel(first.getPnr().intValue());
            return w1.isConfirmed() && w2.status().equals("WL/1");
        } catch (Exception e) { return false; }
    }

    private static boolean t11() {
        try {
            ReservationManager m = manager(1, 3);
            for (int i = 0; i < 4; i++) { m.book(p("Pax", 30), "AC"); }
            Ticket w1 = m.book(p("W one", 30), "AC");
            Ticket w2 = m.book(p("W two", 30), "AC");
            m.cancel(w1.getPnr().intValue());
            return w2.status().equals("WL/1") && m.getTrain().waitingList(0).size() == 1;
        } catch (Exception e) { return false; }
    }

    private static boolean t12() {
        try { manager(1, 1).cancel(999999); return false; }
        catch (TicketNotFoundException e) { return e.getPnr().intValue() == 999999; }
    }

    private static boolean t13() {
        try { manager(1, 1).book(p("Amit", 30), "FIRST"); return false; }
        catch (InvalidInputException e) { return true; }
        catch (Exception e) { return false; }
    }

    private static boolean t14() {
        try {
            ReservationManager m = manager(1, 1);
            Ticket t = m.book(p("Elder", 65), "SL");
            return t.getFare().doubleValue() == 36.0;
        } catch (Exception e) { return false; }
    }

    private static boolean t15() {
        try {
            ReservationManager m = manager(1, 1);
            Ticket t = m.book(p("Kid", 8), "SL");
            return t.getFare().doubleValue() == 30.0;
        } catch (Exception e) { return false; }
    }

    private static boolean t16() {
        try {
            ReservationManager m = manager(1, 1);
            Ticket t = m.book(p("Amit", 30), "AC");
            CancellationResult r = m.cancel(t.getPnr().intValue());
            return r.getRefund().doubleValue() == 97.5;
        } catch (Exception e) { return false; }
    }

    private static boolean t17() {
        try {
            ReservationManager m = manager(1, 3);
            for (int i = 0; i < 5; i++) { m.book(p("Pax", 30), "AC"); }
            m.book(p("Sleeper", 40), "SL");
            TicketStore s = new TicketStore(DIR + File.separator + "t17.txt");
            s.save(m.getTickets());
            ReservationManager m2 = manager(1, 3);
            int skipped = m2.restore(s.load());
            Ticket w = m2.find(100005);
            return skipped == 0 && m2.getTickets().length == 6 && m2.getTrain().coach(0).available() == 0
                    && w.status().equals("WL/1") && m2.book(p("Next", 22), "SL").getPnr().intValue() == 100007;
        } catch (Exception e) { return false; }
    }

    private static boolean t18() {
        try {
            String path = DIR + File.separator + "t18.txt";
            PrintWriter pw = new PrintWriter(new FileWriter(path));
            pw.println("100001|Amit Das|30|M|AC|CNF|0|0|0|130.0|2026-11-01 10:00:00");
            pw.println("garbage line without delimiters");
            pw.println("100002|Bina Roy|abc|F|AC|CNF|0|1|0|130.0|2026-11-01 10:01:00");
            pw.close();
            TicketStore s = new TicketStore(path);
            Ticket[] loaded = s.load();
            return loaded.length == 1 && s.getSkipped() == 2;
        } catch (IOException e) { return false; }
    }

    private static boolean t19() {
        try {
            Coach c = new railway.model.ACCoach(1);
            c.occupy(0, 0, Integer.valueOf(1));
            c.occupy(0, 0, Integer.valueOf(2));
            return false;
        } catch (SeatNotAvailableException e) { return true; }
    }

    private static boolean t20() {
        try {
            ReservationManager m = manager(1, 10);
            for (int i = 0; i < 4; i++) { m.book(p("Pax", 30), "AC"); }
            for (int i = 0; i < 9; i++) { m.book(p("Wait", 30), "AC"); }
            return m.getTrain().waitingList(0).size() == 9 && m.getTickets().length == 13;
        } catch (Exception e) { return false; }
    }

    private static boolean t21() {
        try {
            ReservationManager m = manager(1, 2);
            m.book(p("Sunita Ghosh", 34), "AC");
            String path = DIR + File.separator + "t21_manifest.txt";
            new ManifestWriter().write(path, m.getTrain(), m.getTickets());
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(path));
            boolean found = false;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("Sunita Ghosh") && line.contains("AC-C1-LB1")) { found = true; }
            }
            br.close();
            return found;
        } catch (Exception e) { return false; }
    }

    private static boolean t22() {
        try {
            String path = DIR + File.separator + "t22_history.txt";
            new File(path).delete();
            Train tr = new Train(20001, "Test Express", "A", "B", 50, 1, 2);
            ReservationManager m = new ReservationManager(tr, new HistoryLogger(path));
            Ticket t = m.book(p("Amit", 30), "SL");
            m.cancel(t.getPnr().intValue());
            List<String> lines = new HistoryLogger(path).readAll();
            return lines.size() == 2 && lines.get(0).contains("BOOKED") && lines.get(1).contains("CANCELLED");
        } catch (Exception e) { return false; }
    }
}
