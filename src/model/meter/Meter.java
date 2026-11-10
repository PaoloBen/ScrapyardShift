package model.meter;

public abstract class Meter {
    private final String label;
    private int value;
    private final int min;
    private final int max;

    protected Meter(String label, int startValue, int min, int max) {
        this.label = label;
        this.value = startValue;
        this.min = min;
        this.max = max;
    }

    public void change(int delta) {
        this.value = Math.max(min, Math.min(max, this.value + delta));
    }

    public void reset(int newValue) {
        this.value = Math.max(min, Math.min(max, newValue));
    }

    public abstract boolean isCritical();

    public String getLabel() { return label; }
    public int getValue() { return value; }
    public int getMax() { return max; }
}