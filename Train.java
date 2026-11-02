package railway.model;

import railway.exception.InvalidInputException;

/** A train with one coach per class and one waiting list per class. */
public class Train {
    public static final String[] CLASS_CODES = { "AC", "SL", "GEN" };

    private final Integer number;
    private final String name;
    private final String source;
    private final String destination;
    private final Integer distanceKm;
    private final Coach[] coaches;
    private final WaitingList[] waiting;

    public Train(int number, String name, String source, String destination,
                 int distanceKm, int compartments, int waitLimit) {
        this.number = Integer.valueOf(number);
        this.name = name;
        this.source = source;
        this.destination = destination;
        this.distanceKm = Integer.valueOf(distanceKm);
        this.coaches = new Coach[] {
            new ACCoach(compartments), new SleeperCoach(compartments), new GeneralCoach(compartments)
        };
        this.waiting = new WaitingList[CLASS_CODES.length];
        for (int i = 0; i < CLASS_CODES.length; i++) {
            waiting[i] = new WaitingList(CLASS_CODES[i], waitLimit);
        }
    }

    public Integer getNumber() { return number; }
    public String getName() { return name; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public Integer getDistanceKm() { return distanceKm; }

    public int indexOf(String classCode) throws InvalidInputException {
        String c = (classCode == null) ? "" : classCode.trim().toUpperCase();
        for (int i = 0; i < CLASS_CODES.length; i++) {
            if (CLASS_CODES[i].equals(c)) {
                return i;
            }
        }
        throw new InvalidInputException("Unknown class '" + classCode + "'. Choose AC, SL or GEN.");
    }

    public Coach coach(int index) { return coaches[index]; }
    public WaitingList waitingList(int index) { return waiting[index]; }
}
