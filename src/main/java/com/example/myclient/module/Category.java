package com.example.myclient.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    VISUAL("Visual"),
    MISC("Misc"),
    DONUT("Donut");

    public final String label;
    Category(String label) { this.label = label; }
}
