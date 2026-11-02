package railway.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Appends timestamped events to the trip history log. */
public class HistoryLogger {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String path;

    public HistoryLogger(String path) {
        this.path = path;
    }

    public static String now() {
        return LocalDateTime.now().format(FMT);
    }

    public void log(String event) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path, true))) {
            pw.println("[" + now() + "] " + event);
        } catch (IOException e) {
            System.err.println("Warning: could not write history log: " + e.getMessage());
        }
    }

    public List<String> readAll() throws IOException {
        List<String> lines = new ArrayList<String>();
        if (!new File(path).exists()) {
            return lines;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
