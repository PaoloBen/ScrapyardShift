package model;

public enum BinType {
    BIODEGRADABLE("Biodegradable (Compost)"),
    RECYCLABLE("Recyclable (Baler)"),
    RESIDUAL("Residual (Landfill)"),
    HAZARDOUS("Hazardous (Vault)");

    private final String label;

    BinType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}