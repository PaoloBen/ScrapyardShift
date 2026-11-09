package model;

public enum StationType {
    WASHER("Industrial Washer"),
    DISMANTLER("E-Waste Dismantler"),
    NEUTRALIZER("Hazard Neutralizer");

    private final String label;

    StationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}