package railway.model;

import railway.exception.InvalidInputException;

/** Immutable passenger profile with validated, normalised fields. */
public class Passenger {
    private final String name;
    private final Integer age;
    private final String gender;

    public Passenger(String name, int age, String gender) throws InvalidInputException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Passenger name must not be empty.");
        }
        String clean = name.trim().replaceAll("\\s+", " ");
        for (int i = 0; i < clean.length(); i++) {
            char ch = clean.charAt(i);
            if (!(Character.isLetter(ch) || ch == ' ' || ch == '.')) {
                throw new InvalidInputException("Passenger name may contain only letters, spaces and '.'.");
            }
        }
        Integer a = Integer.valueOf(age);
        if (a < 1 || a > 120) {
            throw new InvalidInputException("Age must be between 1 and 120.");
        }
        String g = (gender == null) ? "" : gender.trim().toUpperCase();
        if (!(g.equals("M") || g.equals("F") || g.equals("O"))) {
            throw new InvalidInputException("Gender must be M, F or O.");
        }
        this.name = toTitleCase(clean);
        this.age = a;
        this.gender = g;
    }

    private static String toTitleCase(String s) {
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                sb.append(w.substring(1).toLowerCase());
            }
            if (i < words.length - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    public String getName() { return name; }
    public Integer getAge() { return age; }
    public String getGender() { return gender; }

    @Override
    public String toString() {
        return name + " (" + age + "/" + gender + ")";
    }
}
