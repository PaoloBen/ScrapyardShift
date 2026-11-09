package model;

public class BinFullException extends Exception {
    private final BinType binType;

    public BinFullException(BinType binType) {
        super("The " + binType.getLabel() + " bay is full! Press SPACE to dispatch forklift.");
        this.binType = binType;
    }

    public BinType getBinType() {
        return binType;
    }
}