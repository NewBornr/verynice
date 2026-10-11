package com.example.myclient.setting;

public class NumberSetting extends Setting<Double> {
    public final double min, max, step;

    public NumberSetting(String name, String description, double value, double min, double max, double step) {
        super(name, description, value);
        this.min = min;
        this.max = max;
        this.step = step;
        set(value);
    }

    @Override
    public void set(Double v) {
        double c = Math.max(min, Math.min(max, v));
        c = Math.round((c - min) / step) * step + min;
        value = Math.max(min, Math.min(max, c));
    }

    public void setFromFraction(double f) {
        f = Math.max(0, Math.min(1, f));
        set(min + f * (max - min));
    }

    public double fraction() { return (value - min) / (max - min); }

    public int intValue() { return value.intValue(); }

    public String display() {
        return step >= 1 ? String.valueOf(value.intValue()) : String.format("%.1f", value);
    }
}
