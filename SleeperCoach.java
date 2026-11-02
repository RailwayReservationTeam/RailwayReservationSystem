package railway.model;

/** Sleeper class: 6 berths per compartment. */
public class SleeperCoach extends Coach {
    private static final String[] BERTHS = { "LB", "MB", "UB", "LB", "MB", "UB" };

    public SleeperCoach(int compartments) {
        super("SL", "Sleeper", compartments, BERTHS.length);
    }

    @Override
    public double ratePerKm() { return 1.20; }

    @Override
    public String berthName(int col) { return BERTHS[col]; }
}
