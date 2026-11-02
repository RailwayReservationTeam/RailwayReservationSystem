package railway.model;

/** Air-conditioned class: 4 berths per compartment. */
public class ACCoach extends Coach {
    private static final String[] BERTHS = { "LB", "UB", "SL", "SU" };

    public ACCoach(int compartments) {
        super("AC", "AC 3-Tier", compartments, BERTHS.length);
    }

    @Override
    public double ratePerKm() { return 2.60; }

    @Override
    public String berthName(int col) { return BERTHS[col]; }
}
