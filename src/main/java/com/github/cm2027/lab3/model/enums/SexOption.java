package com.github.cm2027.lab3.model.enums;

public enum SexOption {
    MAN("Man"),
    KVINNA("Kvinna"),
    ÖVRIGT("Övrigt");

    private final String label;

    SexOption(String label) {this.label = label;}

    public String getLabel () {
        return label;
    }

    public static SexOption fromString (String value) {
        for (SexOption option : values()) {
            if (option.name().equalsIgnoreCase(value.trim()) || option.getLabel().equalsIgnoreCase(value.trim())) {
                return option;
            }
        }
        throw new IllegalArgumentException("Invalid Sex option. Use: Male, Female or Other");
    }

}