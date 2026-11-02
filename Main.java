package railway.app;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
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
import railway.model.WaitingList;
import railway.service.CancellationResult;
import railway.service.ReservationManager;

/**
 * Command-line entry point.
 * Usage: java railway.app.Main [dataDir] [compartmentsPerClass] [waitingListLimit]
 */
public class Main {
    private static Scanner in;
    private static boolean eof = false;

    public static void main(String[] args) {
        String dir = args.length > 0 ? args[0] : "data";
        int compartments = intArg(args, 1, 4, 1, 20, "compartment count");
        int waitLimit = intArg(args, 2, 5, 0, 50, "waiting-list limit");

        File d = new File(dir);
        if (!d.exists() && !d.mkdirs()) {
            System.out.println("Cannot create data directory '" + dir + "'.");
            return;
        }
        String sep = File.separator;
        Train train = new Train(20001, "Kalyani-Howrah Intercity Express", "Kalyani Junction",
                "Howrah Junction", 50, compartments, waitLimit);
        HistoryLogger logger = new HistoryLogger(dir + sep + "trip_history.txt");
        TicketStore store = new TicketStore(dir + sep + "tickets.txt");
        ReservationManager mgr = new ReservationManager(train, logger);

        try {
            Ticket[] saved = store.load();
            int bad = store.getSkipped() + mgr.restore(saved);
            System.out.println("Loaded " + mgr.getTickets().length + " active ticket(s) from " + dir
                    + (bad > 0 ? " (" + bad + " record(s) skipped)" : "") + ".");
        } catch (IOException e) {
            System.out.println("Could not read saved tickets: " + e.getMessage());
        }

        in = new Scanner(System.in);
        boolean running = true;
        while (running && !eof) {
            printMenu(train);
            String choice = ask("Enter choice: ").trim();
            if (eof) {
                break;
            }
            try {
                switch (choice) {
                    case "1": bookTicket(mgr, store); break;
                    case "2": cancelTicket(mgr, store); break;
                    case "3": checkPnr(mgr); break;
                    case "4": showAvailability(train); break;
                    case "5": showWaitingLists(train); break;
                    case "6": exportManifest(mgr, dir + sep + "manifest_" + train.getNumber() + ".txt"); break;
                    case "7": showHistory(logger); break;
                    case "0": running = false; break;
                    default: System.out.println("Invalid choice '" + choice + "'. Enter a number from 0 to 7.");
                }
            } catch (InvalidInputException e) {
                System.out.println("Input error: " + e.getMessage());
            } catch (SeatNotAvailableException e) {
                System.out.println("Booking failed [" + e.getClassCode() + "]: " + e.getMessage());
            } catch (TicketNotFoundException e) {
                System.out.println("Not found: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("File error: " + e.getMessage());
            }
        }
        System.out.println("Thank you for using the Railway Reservation System.");
    }

    private static void printMenu(Train t) {
        System.out.println();
        System.out.println("=== " + t.getNumber() + " " + t.getName() + " ===");
        System.out.println(" 1. Book ticket");
        System.out.println(" 2. Cancel ticket");
        System.out.println(" 3. Check PNR status");
        System.out.println(" 4. Seat availability chart");
        System.out.println(" 5. View waiting lists");
        System.out.println(" 6. Export passenger manifest");
        System.out.println(" 7. View trip history log");
        System.out.println(" 0. Exit");
    }

    private static void bookTicket(ReservationManager mgr, TicketStore store)
            throws InvalidInputException, SeatNotAvailableException, IOException {
        String name = ask("Passenger name : ");
        int age = readInt("Age            : ");
        String gender = ask("Gender (M/F/O) : ");
        String cls = ask("Class (AC/SL/GEN): ");
        Passenger p = new Passenger(name, age, gender);
        Ticket t = mgr.book(p, cls);
        Coach coach = mgr.getTrain().coach(mgr.getTrain().indexOf(cls));
        System.out.println("---------------------------------------------");
        System.out.println(" PNR     : " + t.getPnr());
        System.out.println(" Name    : " + p.getName());
        System.out.println(" Class   : " + coach.getTitle());
        System.out.println(" Status  : " + t.status()
                + (t.isConfirmed() ? "   Seat: " + coach.seatLabel(t.getRow(), t.getCol()) : ""));
        System.out.println(" Fare    : Rs " + String.format("%.2f", t.getFare()));
        System.out.println("---------------------------------------------");
        store.save(mgr.getTickets());
    }

    private static void cancelTicket(ReservationManager mgr, TicketStore store)
            throws InvalidInputException, TicketNotFoundException, IOException {
        int pnr = readInt("PNR to cancel : ");
        CancellationResult r = mgr.cancel(pnr);
        System.out.println("Ticket " + pnr + " cancelled. Refund: Rs " + String.format("%.2f", r.getRefund()));
        if (r.getPromoted() != null) {
            Ticket p = r.getPromoted();
            Coach coach = mgr.getTrain().coach(mgr.getTrain().indexOf(p.getClassCode()));
            System.out.println("Waiting-list promotion: PNR " + p.getPnr() + " (" + p.getPassenger().getName()
                    + ") is now CNF, seat " + coach.seatLabel(p.getRow(), p.getCol()) + ".");
        }
        store.save(mgr.getTickets());
    }

    private static void checkPnr(ReservationManager mgr) throws InvalidInputException, TicketNotFoundException {
        int pnr = readInt("PNR : ");
        Ticket t = mgr.find(pnr);
        Coach coach = mgr.getTrain().coach(mgr.getTrain().indexOf(t.getClassCode()));
        System.out.println(" PNR " + t.getPnr() + " | " + t.getPassenger() + " | " + coach.getTitle()
                + " | " + t.status()
                + (t.isConfirmed() ? " | Seat " + coach.seatLabel(t.getRow(), t.getCol()) : "")
                + " | Rs " + String.format("%.2f", t.getFare()));
    }

    private static void showAvailability(Train train) {
        for (int i = 0; i < Train.CLASS_CODES.length; i++) {
            Coach c = train.coach(i);
            WaitingList wl = train.waitingList(i);
            System.out.println(c.getCode() + " (" + c.getTitle() + ")  capacity " + c.capacity()
                    + ", available " + c.available() + ", waiting " + wl.size() + "/" + wl.limit());
            for (int r = 0; r < c.getRows(); r++) {
                StringBuilder row = new StringBuilder("  C" + (r + 1) + " ");
                for (int k = 0; k < c.getCols(); k++) {
                    row.append(c.pnrAt(r, k) == null ? "[ ]" : "[X]");
                }
                System.out.println(row);
            }
        }
    }

    private static void showWaitingLists(Train train) {
        boolean any = false;
        for (int i = 0; i < Train.CLASS_CODES.length; i++) {
            WaitingList wl = train.waitingList(i);
            for (int k = 0; k < wl.size(); k++) {
                Ticket t = wl.get(k);
                System.out.println(" " + Train.CLASS_CODES[i] + "  " + t.status() + "  PNR " + t.getPnr() + "  "
                        + t.getPassenger());
                any = true;
            }
        }
        if (!any) {
            System.out.println("All waiting lists are empty.");
        }
    }

    private static void exportManifest(ReservationManager mgr, String path) throws IOException {
        new ManifestWriter().write(path, mgr.getTrain(), mgr.getTickets());
        System.out.println("Manifest written to " + path);
    }

    private static void showHistory(HistoryLogger logger) throws IOException {
        List<String> lines = logger.readAll();
        if (lines.isEmpty()) {
            System.out.println("History log is empty.");
        }
        for (String s : lines) {
            System.out.println(s);
        }
    }

    private static String ask(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) {
            eof = true;
            return "";
        }
        return in.nextLine();
    }

    private static int readInt(String prompt) throws InvalidInputException {
        String s = ask(prompt).trim();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("'" + s + "' is not a valid whole number.");
        }
    }

    private static int intArg(String[] args, int i, int def, int min, int max, String what) {
        if (args.length <= i) {
            return def;
        }
        try {
            int v = Integer.parseInt(args[i]);
            if (v < min || v > max) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException e) {
            System.out.println("Invalid " + what + " '" + args[i] + "'; using default " + def + ".");
            return def;
        }
    }
}
