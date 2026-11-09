package model;

public class ComplianceMeter extends Meter {
    public ComplianceMeter() {
        super("Facility Compliance", 100, 0, 100);
    }

    @Override
    public boolean isCritical() {
        return getValue() <= 25;
    }

    public boolean isDepleted() {
        return getValue() <= 0;
    }
}