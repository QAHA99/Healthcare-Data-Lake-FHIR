package com.github.cm2027.lab3.model.enums;

public enum Severity {
    HÖG("Hög"),
    MEDEL("Medel"),
    LÅG("Låg");

    private final String label;

    Severity (String label) {this.label = label;}

    public String getLabel() {
        return label;
    }

    public static Severity fromString (String value) {
        for (Severity option : values()) {
            if (option.name().equalsIgnoreCase(value.trim()) || option.getLabel().equalsIgnoreCase(value.trim())) {
                return option;
            }
        }
        throw new IllegalArgumentException("Invalid severity input. Use: Hög, Medel or Låg");
    }

}
