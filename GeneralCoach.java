package railway.model;

/** General class: 8 seats per compartment. */
public class GeneralCoach extends Coach {

    public GeneralCoach(int compartments) {
        super("GEN", "General", compartments, 8);
    }

    @Override
    public double ratePerKm() { return 0.55; }

    @Override
    public String berthName(int col) { return "ST"; }
}
