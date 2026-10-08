package br.com.pabelis.quickclock.domain.company;

public record WeekdaySettings(
    boolean mondayActive,
    boolean tuesdayActive,
    boolean wednesdayActive,
    boolean thursdayActive,
    boolean fridayActive,
    boolean saturdayActive,
    boolean sundayActive
) {

    public static WeekdaySettings mondayToFriday() {
        return new WeekdaySettings(true, true, true, true, true, false, false);
    }
}
