package railway.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import railway.exception.InvalidInputException;
import railway.model.Ticket;

/** Reads and writes active tickets as a flat, pipe-delimited text file. */
public class TicketStore {
    private final String path;
    private int skipped = 0;

    public TicketStore(String path) {
        this.path = path;
    }

    public int getSkipped() {
        return skipped;
    }

    public void save(Ticket[] tickets) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path, false))) {
            for (int i = 0; i < tickets.length; i++) {
                pw.println(tickets[i].toRecord());
            }
        }
    }

    /** Loads tickets; malformed lines are counted in getSkipped() and ignored. */
    public Ticket[] load() throws IOException {
        skipped = 0;
        List<Ticket> list = new ArrayList<Ticket>();
        if (!new File(path).exists()) {
            return new Ticket[0];
        }
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    list.add(Ticket.fromRecord(line));
                } catch (InvalidInputException e) {
                    skipped++;
                    System.err.println("Skipped line " + lineNo + " of " + path + ": " + e.getMessage());
                }
            }
        }
        return list.toArray(new Ticket[0]);
    }
}
